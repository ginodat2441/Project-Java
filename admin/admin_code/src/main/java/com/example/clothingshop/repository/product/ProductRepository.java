package com.example.clothingshop.repository.product;

import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.common.SqlHelper;
import com.example.clothingshop.dto.product.ProductDetailView;
import com.example.clothingshop.dto.product.ProductView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

  private final SqlHelper sql;

  public ProductRepository(SqlHelper sql) {
    this.sql = sql;
  }

  private static final String SELECT_PRODUCT =
      """
      SELECT p.ID, p.PRODUCT_CODE, p.PRODUCT_NAME,
             b.BRAND_NAME, m.MATERIAL_NAME,
             p.TARGET_GENDER, p.THUMBNAIL_URL, p.STATUS,
             (SELECT MIN(pd.PRICE) FROM PRODUCT_DETAIL pd
               WHERE pd.PRODUCT_ID = p.ID AND pd.DELETE_FLAG = 0)          AS MIN_PRICE,
             (SELECT NVL(SUM(pd.QUANTITY), 0) FROM PRODUCT_DETAIL pd
               WHERE pd.PRODUCT_ID = p.ID AND pd.DELETE_FLAG = 0)          AS TOTAL_STOCK
        FROM PRODUCT p
        JOIN BRAND b    ON b.ID = p.BRAND_ID
        JOIN MATERIAL m ON m.ID = p.MATERIAL_ID
       WHERE p.DELETE_FLAG = 0 AND p.STATUS = 1
      """;

  /** Tìm kiếm động: chỉ nối điều kiện khi tham số có giá trị. */
  public PageResult<ProductView> search(
      String keyword, Long brandId, Integer gender, int page, int size) {
    StringBuilder q =
        new StringBuilder(
            SELECT_PRODUCT + com.example.clothingshop.util.product.CategoryVisibility.SQL);
    Map<String, Object> p = new HashMap<>();
    if (keyword != null && !keyword.isBlank()) {
      q.append(" AND LOWER(p.PRODUCT_NAME) LIKE '%' || LOWER(:keyword) || '%' ");
      p.put("keyword", keyword.trim());
    }
    if (brandId != null) {
      q.append(" AND p.BRAND_ID = :brandId ");
      p.put("brandId", brandId);
    }
    if (gender != null) {
      q.append(" AND p.TARGET_GENDER = :gender ");
      p.put("gender", gender);
    }
    q.append(" ORDER BY p.ID DESC");
    return sql.page(q.toString(), p, page, size, ProductView.class);
  }

  public Optional<ProductView> findById(long id) {
    return sql.one(
        SELECT_PRODUCT
            + com.example.clothingshop.util.product.CategoryVisibility.SQL
            + " AND p.ID = :id",
        Map.of("id", id),
        ProductView.class);
  }

  public List<ProductDetailView> findDetails(long productId) {
    String q =
        """
SELECT pd.ID, pd.PRODUCT_ID, c.COLOR_NAME, c.HEX_CODE,
       pd.SIZE_NAME, pd.SKU, pd.PRICE, pd.QUANTITY, pd.IMAGE_URL
  FROM PRODUCT_DETAIL pd
  JOIN COLOR c ON c.ID = pd.COLOR_ID
 WHERE pd.PRODUCT_ID = :pid AND pd.DELETE_FLAG = 0 AND pd.STATUS=1
   AND EXISTS (SELECT 1 FROM PRODUCT p WHERE p.ID=pd.PRODUCT_ID AND p.DELETE_FLAG=0 AND p.STATUS=1"""
            + com.example.clothingshop.util.product.CategoryVisibility.SQL
            + """
                 )
               ORDER BY c.COLOR_NAME, pd.SIZE_NAME
              """;
    return sql.list(q, Map.of("pid", productId), ProductDetailView.class);
  }
}

package com.example.clothingshop.repositories;

import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.common.SqlHelper;
import com.example.clothingshop.view.ProductDetailView;
import com.example.clothingshop.view.ProductView;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ProductRepository {

    private final SqlHelper sql;

    public ProductRepository(SqlHelper sql) { this.sql = sql; }

    // Toàn bộ truy vấn ở đây là JOIN nhiều bảng nên trả về VIEW (đặt tên *View để dễ phân biệt).
    private static final String SELECT_PRODUCT = """
            SELECT p.ID, p.PRODUCT_CODE, p.PRODUCT_NAME,
                   b.BRAND_NAME, m.MATERIAL_NAME,
                   p.TARGET_GENDER, p.THUMBNAIL_URL, p.STATUS,
                   (SELECT MIN(pd.PRICE) FROM PRODUCT_DETAIL pd
                     WHERE pd.PRODUCT_ID = p.ID AND pd.DELETE_FLAG = 0 AND pd.STATUS = 1) AS MIN_PRICE,
                   (SELECT NVL(SUM(pd.QUANTITY), 0) FROM PRODUCT_DETAIL pd
                     WHERE pd.PRODUCT_ID = p.ID AND pd.DELETE_FLAG = 0 AND pd.STATUS = 1) AS TOTAL_STOCK
              FROM PRODUCT p
              JOIN BRAND b    ON b.ID = p.BRAND_ID
              JOIN MATERIAL m ON m.ID = p.MATERIAL_ID
             WHERE p.DELETE_FLAG = 0 AND p.STATUS = 1
               AND (p.CATEGORY_ID IS NULL OR p.CATEGORY_ID NOT IN
                 (SELECT ID FROM CATEGORY START WITH STATUS=0 CONNECT BY NOCYCLE PRIOR ID=PARENT_ID))
            """;

    /** Tìm kiếm động: chỉ nối điều kiện khi tham số có giá trị. */
    public PageResult<ProductView> searchViews(String keyword, Long brandId, Integer gender,
                                          int page, int size) {
        return searchViews(keyword, brandId, gender, null, "newest", page, size);
    }

    public PageResult<ProductView> searchViews(String keyword, Long brandId, Integer gender,
                                          Long categoryId, String sort, int page, int size) {
        StringBuilder q = new StringBuilder(SELECT_PRODUCT);
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
        if (categoryId != null) {
            q.append(" AND p.CATEGORY_ID IN (SELECT ID FROM CATEGORY START WITH ID=:categoryId CONNECT BY NOCYCLE PRIOR ID=PARENT_ID) ");
            p.put("categoryId", categoryId);
        }
        q.append(switch (sort) {
            case "price_asc" -> " ORDER BY MIN_PRICE ASC NULLS LAST, p.ID DESC";
            case "price_desc" -> " ORDER BY MIN_PRICE DESC NULLS LAST, p.ID DESC";
            default -> " ORDER BY p.CREATED_AT DESC, p.ID DESC";
        });
        return sql.page(q.toString(), p, page, size, ProductView.class);
    }

    public Optional<ProductView> findViewById(long id) {
        return sql.one(SELECT_PRODUCT + " AND p.ID = :id", Map.of("id", id), ProductView.class);
    }

    public List<ProductDetailView> findDetailViews(long productId) {
        String q = """
                SELECT pd.ID, pd.PRODUCT_ID, c.COLOR_NAME, c.HEX_CODE,
                       pd.SIZE_NAME, pd.SKU, pd.PRICE, pd.QUANTITY, pd.IMAGE_URL
                  FROM PRODUCT_DETAIL pd
                  JOIN COLOR c ON c.ID = pd.COLOR_ID
                 WHERE pd.PRODUCT_ID = :pid AND pd.DELETE_FLAG = 0 AND pd.STATUS = 1
                 ORDER BY c.COLOR_NAME, pd.SIZE_NAME
                """;
        return sql.list(q, Map.of("pid", productId), ProductDetailView.class);
    }

    public List<com.example.clothingshop.view.CategoryView> categories() {
        return sql.list("SELECT ID,CATEGORY_NAME,PARENT_ID FROM CATEGORY WHERE STATUS=1 AND ID NOT IN "
            + "(SELECT ID FROM CATEGORY START WITH STATUS=0 CONNECT BY NOCYCLE PRIOR ID=PARENT_ID) ORDER BY CATEGORY_NAME",
            Map.of(), com.example.clothingshop.view.CategoryView.class);
    }
}

package com.example.clothingshop.repositories;

import com.example.clothingshop.common.SqlHelper;
import com.example.clothingshop.entity.Cart;
import com.example.clothingshop.entity.CartDetail;
import com.example.clothingshop.view.CartItemView;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CartRepository {

    private final SqlHelper sql;

    public CartRepository(SqlHelper sql) { this.sql = sql; }

    // ====================== ENTITY ======================

    public boolean customerExists(long customerId) {
        return sql.scalar("SELECT COUNT(*) FROM CUSTOMER WHERE ID = :id AND DELETE_FLAG = 0",
                Map.of("id", customerId), Long.class).orElse(0L) > 0;
    }

    /** Giỏ đang dùng (STATUS = 1) của khách; nếu có nhiều thì lấy giỏ cũ nhất. */
    public Optional<Cart> findActiveCart(long customerId) {
        String q = """
                SELECT ID, CUSTOMER_ID, STATUS, CREATED_AT, UPDATED_AT
                  FROM CART
                 WHERE CUSTOMER_ID = :cid AND STATUS = 1 AND DELETE_FLAG = 0
                 ORDER BY ID
                 FETCH FIRST 1 ROWS ONLY
                """;
        return sql.one(q, Map.of("cid", customerId), Cart.class);
    }

    public long insertCart(long customerId) {
        return sql.insert("INSERT INTO CART (CUSTOMER_ID) VALUES (:cid)", Map.of("cid", customerId));
    }

    public List<CartDetail> findDetails(long cartId) {
        String q = """
                SELECT ID, CART_ID, PRODUCT_DETAIL_ID, QUANTITY, CREATED_AT, UPDATED_AT
                  FROM CART_DETAIL
                 WHERE CART_ID = :cartId AND DELETE_FLAG = 0
                 ORDER BY ID
                """;
        return sql.list(q, Map.of("cartId", cartId), CartDetail.class);
    }

    public Optional<CartDetail> findDetailByProduct(long cartId, long productDetailId) {
        String q = """
                SELECT ID, CART_ID, PRODUCT_DETAIL_ID, QUANTITY, CREATED_AT, UPDATED_AT
                  FROM CART_DETAIL
                 WHERE CART_ID = :cartId AND PRODUCT_DETAIL_ID = :pdId AND DELETE_FLAG = 0
                """;
        return sql.one(q, Map.of("cartId", cartId, "pdId", productDetailId), CartDetail.class);
    }

    /** Tìm 1 dòng giỏ và đảm bảo nó thuộc giỏ đang dùng CỦA ĐÚNG khách này. */
    public Optional<CartDetail> findDetailOfCustomer(long itemId, long customerId) {
        String q = """
                SELECT cd.ID, cd.CART_ID, cd.PRODUCT_DETAIL_ID, cd.QUANTITY, cd.CREATED_AT, cd.UPDATED_AT
                  FROM CART_DETAIL cd
                  JOIN CART c ON c.ID = cd.CART_ID
                 WHERE cd.ID = :id AND c.CUSTOMER_ID = :cid
                   AND c.STATUS = 1 AND c.DELETE_FLAG = 0 AND cd.DELETE_FLAG = 0
                """;
        return sql.one(q, Map.of("id", itemId, "cid", customerId), CartDetail.class);
    }

    /** Tồn kho hiện tại của biến thể còn bán; rỗng nếu biến thể không tồn tại / ngừng bán. */
    public Optional<Integer> findStock(long productDetailId) {
        return sql.scalar("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID = :id AND DELETE_FLAG = 0 AND STATUS = 1",
                Map.of("id", productDetailId), Integer.class);
    }

    public long insertDetail(long cartId, long productDetailId, int qty) {
        String q = "INSERT INTO CART_DETAIL (CART_ID, PRODUCT_DETAIL_ID, QUANTITY) VALUES (:cartId, :pdId, :qty)";
        return sql.insert(q, Map.of("cartId", cartId, "pdId", productDetailId, "qty", qty));
    }

    public int updateDetailQuantity(long itemId, int qty) {
        return sql.update("UPDATE CART_DETAIL SET QUANTITY = :qty WHERE ID = :id AND DELETE_FLAG = 0",
                Map.of("id", itemId, "qty", qty));
    }

    /** Xóa mềm 1 dòng */
    public int softDeleteDetail(long itemId) {
        return sql.update("UPDATE CART_DETAIL SET DELETE_FLAG = 1 WHERE ID = :id AND DELETE_FLAG = 0",
                Map.of("id", itemId));
    }

    /** Xóa mềm toàn bộ dòng trong giỏ */
    public int softDeleteAllDetails(long cartId) {
        return sql.update("UPDATE CART_DETAIL SET DELETE_FLAG = 1 WHERE CART_ID = :cartId AND DELETE_FLAG = 0",
                Map.of("cartId", cartId));
    }

    /** Đánh dấu giỏ đã checkout (STATUS = 0), lần sau khách thêm hàng sẽ tạo giỏ mới. */
    public int markCheckedOut(long cartId) {
        return sql.update("UPDATE CART SET STATUS = 0 WHERE ID = :id AND STATUS = 1", Map.of("id", cartId));
    }

    // ====================== VIEW (JOIN) ======================

    public List<CartItemView> findItemViews(long cartId) {
        String q = """
                SELECT cd.ID, cd.PRODUCT_DETAIL_ID, p.ID AS PRODUCT_ID, p.PRODUCT_NAME,
                       c.COLOR_NAME, pd.SIZE_NAME, pd.SKU,
                       NVL(pd.IMAGE_URL, p.THUMBNAIL_URL)                       AS IMAGE_URL,
                       pd.PRICE                                                 AS UNIT_PRICE,
                       cd.QUANTITY,
                       pd.PRICE * cd.QUANTITY                                   AS TOTAL_PRICE,
                       pd.QUANTITY                                              AS STOCK,
                       CASE WHEN pd.DELETE_FLAG = 0 AND pd.STATUS = 1 THEN 1 ELSE 0 END AS AVAILABLE
                  FROM CART_DETAIL cd
                  JOIN PRODUCT_DETAIL pd ON pd.ID = cd.PRODUCT_DETAIL_ID
                  JOIN PRODUCT p         ON p.ID  = pd.PRODUCT_ID
                  JOIN COLOR c           ON c.ID  = pd.COLOR_ID
                 WHERE cd.CART_ID = :cartId AND cd.DELETE_FLAG = 0
                 ORDER BY cd.ID
                """;
        return sql.list(q, Map.of("cartId", cartId), CartItemView.class);
    }
}

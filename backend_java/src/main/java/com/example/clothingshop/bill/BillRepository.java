package com.example.clothingshop.bill;

import com.example.clothingshop.common.SqlHelper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class BillRepository {

    private final SqlHelper sql;

    public BillRepository(SqlHelper sql) { this.sql = sql; }

    /** Snapshot địa chỉ giao hàng (gộp thành 1 chuỗi bằng toán tử || của Oracle) */
    public record AddressSnapshot(String receiverName, String receiverPhone, String fullAddress) {}

    public Optional<AddressSnapshot> findAddress(long addressId, long customerId) {
        String q = """
                SELECT RECEIVER_NAME, RECEIVER_PHONE,
                       DETAIL_ADDRESS || ', ' || WARD || ', ' || PROVINCE AS FULL_ADDRESS
                  FROM ADDRESS
                 WHERE ID = :id AND CUSTOMER_ID = :cid AND DELETE_FLAG = 0
                """;
        return sql.one(q, Map.of("id", addressId, "cid", customerId), AddressSnapshot.class);
    }

    public Optional<BigDecimal> findPrice(long productDetailId) {
        return sql.scalar("SELECT PRICE FROM PRODUCT_DETAIL WHERE ID = :id AND DELETE_FLAG = 0 AND STATUS = 1",
                Map.of("id", productDetailId), BigDecimal.class);
    }

    /**
     * Trừ tồn kho NGUYÊN TỬ: điều kiện QUANTITY >= :qty nằm ngay trong UPDATE
     * nên 2 người mua cùng lúc cũng không làm tồn kho âm. Trả 0 nếu không đủ hàng.
     */
    public int decreaseStock(long productDetailId, int qty) {
        String q = """
                UPDATE PRODUCT_DETAIL
                   SET QUANTITY = QUANTITY - :qty
                 WHERE ID = :id AND QUANTITY >= :qty AND DELETE_FLAG = 0 AND STATUS = 1
                """;
        return sql.update(q, Map.of("id", productDetailId, "qty", qty));
    }

    public long insertBill(Map<String, Object> p) {
        String q = """
                INSERT INTO BILL (BILL_CODE, CUSTOMER_ID, ADDRESS_ID, RECEIVER_NAME, RECEIVER_PHONE,
                                  SHIPPING_ADDRESS, SUB_TOTAL, SHIPPING_FEE, DISCOUNT_AMOUNT,
                                  TOTAL_AMOUNT, PAYMENT_METHOD, NOTE)
                VALUES (:code, :customerId, :addressId, :receiverName, :receiverPhone,
                        :shippingAddress, :subTotal, :shippingFee, :discount,
                        :total, :paymentMethod, :note)
                """;
        return sql.insert(q, p);
    }

    public void insertBillDetail(long billId, long productDetailId, int qty, BigDecimal unitPrice) {
        String q = """
                INSERT INTO BILL_DETAIL (BILL_ID, PRODUCT_DETAIL_ID, QUANTITY, UNIT_PRICE, TOTAL_PRICE)
                VALUES (:billId, :pdId, :qty, :price, :total)
                """;
        Map<String, Object> p = new HashMap<>();
        p.put("billId", billId);
        p.put("pdId", productDetailId);
        p.put("qty", qty);
        p.put("price", unitPrice);
        p.put("total", unitPrice.multiply(BigDecimal.valueOf(qty)));
        sql.update(q, p);
    }

    public void insertHistory(long billId, Integer oldStatus, int newStatus, String note, Long managerId) {
        String q = """
                INSERT INTO BILL_HISTORY (BILL_ID, OLD_STATUS, NEW_STATUS, NOTE, MANAGER_ID)
                VALUES (:billId, :oldStatus, :newStatus, :note, :managerId)
                """;
        Map<String, Object> p = new HashMap<>();
        p.put("billId", billId);
        p.put("oldStatus", oldStatus);
        p.put("newStatus", newStatus);
        p.put("note", note);
        p.put("managerId", managerId);
        sql.update(q, p);
    }

    private static final String SELECT_BILL = """
            SELECT ID, BILL_CODE, CUSTOMER_ID, RECEIVER_NAME, RECEIVER_PHONE, SHIPPING_ADDRESS,
                   SUB_TOTAL, SHIPPING_FEE, DISCOUNT_AMOUNT, TOTAL_AMOUNT,
                   PAYMENT_METHOD, PAYMENT_STATUS, STATUS, CREATED_AT
              FROM BILL
            """;

    public Optional<BillView> findById(long id) {
        return sql.one(SELECT_BILL + " WHERE ID = :id AND DELETE_FLAG = 0", Map.of("id", id), BillView.class);
    }

    public List<BillView> findByCustomer(long customerId) {
        return sql.list(SELECT_BILL + " WHERE CUSTOMER_ID = :cid AND DELETE_FLAG = 0 ORDER BY CREATED_AT DESC",
                Map.of("cid", customerId), BillView.class);
    }

    public List<BillLine> findLines(long billId) {
        String q = """
                SELECT bd.ID, bd.PRODUCT_DETAIL_ID, p.PRODUCT_NAME, c.COLOR_NAME,
                       pd.SIZE_NAME, pd.SKU, bd.QUANTITY, bd.UNIT_PRICE, bd.TOTAL_PRICE
                  FROM BILL_DETAIL bd
                  JOIN PRODUCT_DETAIL pd ON pd.ID = bd.PRODUCT_DETAIL_ID
                  JOIN PRODUCT p         ON p.ID  = pd.PRODUCT_ID
                  JOIN COLOR c           ON c.ID  = pd.COLOR_ID
                 WHERE bd.BILL_ID = :billId AND bd.DELETE_FLAG = 0
                 ORDER BY bd.ID
                """;
        return sql.list(q, Map.of("billId", billId), BillLine.class);
    }

    /** Đổi trạng thái; điều kiện STATUS = :oldStatus tránh ghi đè khi 2 người cùng thao tác. */
    public int updateStatus(long billId, int oldStatus, int newStatus, Long managerId) {
        String q = """
                UPDATE BILL SET STATUS = :newStatus, HANDLED_BY = NVL(:managerId, HANDLED_BY)
                 WHERE ID = :id AND STATUS = :oldStatus AND DELETE_FLAG = 0
                """;
        Map<String, Object> p = new HashMap<>();
        p.put("id", billId);
        p.put("oldStatus", oldStatus);
        p.put("newStatus", newStatus);
        p.put("managerId", managerId);
        return sql.update(q, p);
    }
}

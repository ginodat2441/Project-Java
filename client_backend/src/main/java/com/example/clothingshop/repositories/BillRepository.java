package com.example.clothingshop.repositories;

import com.example.clothingshop.common.SqlHelper;
import com.example.clothingshop.entity.Bill;
import com.example.clothingshop.entity.BillDetail;
import com.example.clothingshop.entity.SePayPaymentAttempt;
import com.example.clothingshop.view.BillLineView;
import com.example.clothingshop.view.BillHistoryView;
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

    /** Giữ chỗ nguyên tử; QUANTITY vẫn là số lượng còn bán được trên website. */
    public int reserveStock(long productDetailId, int qty) {
        String q = """
                UPDATE PRODUCT_DETAIL
                   SET QUANTITY = QUANTITY - :qty,
                       RESERVED_QUANTITY = RESERVED_QUANTITY + :qty,
                       STOCK_VERSION = STOCK_VERSION + 1
                 WHERE ID = :id AND QUANTITY >= :qty AND DELETE_FLAG = 0 AND STATUS = 1
                """;
        return sql.update(q, Map.of("id", productDetailId, "qty", qty));
    }

    public long insertBill(Map<String, Object> p) {
        String q = """
                INSERT INTO BILL (BILL_CODE, CUSTOMER_ID, ADDRESS_ID, RECEIVER_NAME, RECEIVER_PHONE,
                                  SHIPPING_ADDRESS, SUB_TOTAL, SHIPPING_FEE, DISCOUNT_AMOUNT,
                                  TOTAL_AMOUNT, PAYMENT_METHOD, NOTE, STOCK_RESERVED)
                VALUES (:code, :customerId, :addressId, :receiverName, :receiverPhone,
                        :shippingAddress, :subTotal, :shippingFee, :discount,
                        :total, :paymentMethod, :note, 1)
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

    public void releaseReservedStockForBill(long billId) {
        int reservationClaimed = sql.update("""
                UPDATE BILL
                   SET STOCK_RESERVED = 0
                 WHERE ID = :billId AND STOCK_RESERVED = 1 AND DELETE_FLAG = 0
                """, Map.of("billId", billId));
        if (reservationClaimed == 0) return;

        Long expectedProducts = sql.scalar("""
                SELECT COUNT(DISTINCT PRODUCT_DETAIL_ID)
                  FROM BILL_DETAIL
                 WHERE BILL_ID = :billId AND DELETE_FLAG = 0
                """, Map.of("billId", billId), Long.class).orElse(0L);
        int releasedProducts = sql.update("""
                UPDATE PRODUCT_DETAIL pd
                   SET RESERVED_QUANTITY = RESERVED_QUANTITY - (
                       SELECT SUM(bd.QUANTITY)
                         FROM BILL_DETAIL bd
                        WHERE bd.BILL_ID = :billId
                          AND bd.PRODUCT_DETAIL_ID = pd.ID
                          AND bd.DELETE_FLAG = 0
                   ),
                       QUANTITY = QUANTITY + (
                       SELECT SUM(bd.QUANTITY)
                         FROM BILL_DETAIL bd
                        WHERE bd.BILL_ID = :billId
                          AND bd.PRODUCT_DETAIL_ID = pd.ID
                          AND bd.DELETE_FLAG = 0
                   ),
                       STOCK_VERSION = STOCK_VERSION + 1
                 WHERE EXISTS (
                       SELECT 1
                         FROM BILL_DETAIL bd
                        WHERE bd.BILL_ID = :billId
                          AND bd.PRODUCT_DETAIL_ID = pd.ID
                          AND bd.DELETE_FLAG = 0
                 )
                   AND RESERVED_QUANTITY >= (
                       SELECT SUM(bd.QUANTITY)
                         FROM BILL_DETAIL bd
                        WHERE bd.BILL_ID = :billId
                          AND bd.PRODUCT_DETAIL_ID = pd.ID
                          AND bd.DELETE_FLAG = 0
                   )
                """, Map.of("billId", billId));
        if (releasedProducts != expectedProducts) {
            throw new IllegalStateException("Không thể giải phóng đủ tồn kho giữ chỗ cho đơn hàng id=" + billId);
        }
    }

    public int updateCashPaymentStatus(long billId, int oldPaymentStatus, int newPaymentStatus) {
        return sql.update("""
                UPDATE BILL
                   SET PAYMENT_STATUS = :newStatus
                 WHERE ID = :id
                   AND PAYMENT_METHOD = 'COD'
                   AND PAYMENT_STATUS = :oldStatus
                   AND DELETE_FLAG = 0
                """, Map.of("id", billId, "oldStatus", oldPaymentStatus, "newStatus", newPaymentStatus));
    }

    public void insertCashPaymentHistory(long billId, BigDecimal amount, int status,
                                         String note, boolean paid) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("billId", billId);
        parameters.put("amount", amount);
        parameters.put("status", status);
        parameters.put("note", note);
        parameters.put("paidAt", paid ? java.sql.Timestamp.from(java.time.Instant.now()) : null);
        sql.update("""
                INSERT INTO PAYMENT_HISTORY (BILL_ID, AMOUNT, PAYMENT_METHOD, STATUS, PAID_AT, NOTE)
                VALUES (:billId, :amount, 'COD', :status, :paidAt, :note)
                """, parameters);
    }

    public Optional<SePayPaymentAttempt> findPendingSePayPayment(long billId) {
        return sql.one("""
                SELECT PH.TRANSACTION_CODE AS INVOICE_NUMBER, PH.BILL_ID, PH.AMOUNT, PH.STATUS
                  FROM PAYMENT_HISTORY PH
                 WHERE PH.BILL_ID = :billId
                   AND PH.PAYMENT_METHOD = 'SEPAY'
                   AND PH.STATUS = 0
                   AND PH.DELETE_FLAG = 0
                 ORDER BY PH.CREATED_AT DESC, PH.ID DESC
                FETCH FIRST 1 ROWS ONLY
                """, Map.of("billId", billId), SePayPaymentAttempt.class);
    }

    public Optional<SePayPaymentAttempt> findSePayPayment(String invoiceNumber) {
        return sql.one("""
                SELECT PH.TRANSACTION_CODE AS INVOICE_NUMBER, PH.BILL_ID, PH.AMOUNT, PH.STATUS
                  FROM PAYMENT_HISTORY PH
                 WHERE PH.TRANSACTION_CODE = :invoiceNumber
                   AND PH.PAYMENT_METHOD = 'SEPAY'
                   AND PH.DELETE_FLAG = 0
                """, Map.of("invoiceNumber", invoiceNumber), SePayPaymentAttempt.class);
    }

    public void insertSePayPayment(long billId, BigDecimal amount, String invoiceNumber) {
        sql.update("""
                INSERT INTO PAYMENT_HISTORY (BILL_ID, AMOUNT, PAYMENT_METHOD, TRANSACTION_CODE, STATUS, NOTE)
                VALUES (:billId, :amount, 'SEPAY', :invoiceNumber, 0, 'Đang chờ thanh toán SePay')
                """, Map.of("billId", billId, "amount", amount, "invoiceNumber", invoiceNumber));
    }

    public int updateSePayPaymentStatus(String invoiceNumber, int oldStatus, int newStatus, String note) {
        return sql.update("""
                UPDATE PAYMENT_HISTORY
                   SET STATUS = :newStatus,
                       PAID_AT = CASE WHEN :newStatus = 1 THEN SYSTIMESTAMP ELSE PAID_AT END,
                       NOTE = :note
                 WHERE TRANSACTION_CODE = :invoiceNumber
                   AND PAYMENT_METHOD = 'SEPAY'
                   AND STATUS = :oldStatus
                   AND DELETE_FLAG = 0
                """, Map.of("invoiceNumber", invoiceNumber, "oldStatus", oldStatus,
                "newStatus", newStatus, "note", note));
    }

    public int markSePayBillPaid(long billId) {
        return sql.update("""
                UPDATE BILL
                   SET PAYMENT_STATUS = 1
                 WHERE ID = :billId
                   AND PAYMENT_METHOD = 'SEPAY'
                   AND PAYMENT_STATUS = 0
                   AND DELETE_FLAG = 0
                """, Map.of("billId", billId));
    }

    // ====================== ENTITY (1 bảng, trả về entity) ======================

    private static final String SELECT_BILL = """
            SELECT ID, BILL_CODE, CUSTOMER_ID, ADDRESS_ID, RECEIVER_NAME, RECEIVER_PHONE,
                   SHIPPING_ADDRESS, SUB_TOTAL, SHIPPING_FEE, DISCOUNT_AMOUNT, TOTAL_AMOUNT,
                   PAYMENT_METHOD, PAYMENT_STATUS, STATUS, NOTE, HANDLED_BY,
                   CREATED_AT, UPDATED_AT
              FROM BILL
            """;

    public Optional<Bill> findById(long id) {
        return sql.one(SELECT_BILL + " WHERE ID = :id AND DELETE_FLAG = 0", Map.of("id", id), Bill.class);
    }

    public Optional<Bill> findByIdAndCustomer(long id, long customerId) {
        return sql.one(SELECT_BILL + " WHERE ID = :id AND CUSTOMER_ID = :customerId AND DELETE_FLAG = 0",
                Map.of("id", id, "customerId", customerId), Bill.class);
    }

    public Optional<Bill> lockById(long id) {
        return sql.one(SELECT_BILL + " WHERE ID = :id AND DELETE_FLAG = 0 FOR UPDATE",
                Map.of("id", id), Bill.class);
    }

    public Optional<Bill> lockByIdAndCustomer(long id, long customerId) {
        return sql.one(SELECT_BILL + " WHERE ID = :id AND CUSTOMER_ID = :customerId AND DELETE_FLAG = 0 FOR UPDATE",
                Map.of("id", id, "customerId", customerId), Bill.class);
    }

    public List<Bill> findByCustomer(long customerId) {
        return sql.list(SELECT_BILL + " WHERE CUSTOMER_ID = :cid AND DELETE_FLAG = 0 ORDER BY CREATED_AT DESC",
                Map.of("cid", customerId), Bill.class);
    }

    public List<Bill> findAll() {
        return sql.list(SELECT_BILL + " WHERE DELETE_FLAG = 0 ORDER BY CREATED_AT DESC, ID DESC",
                Map.of(), Bill.class);
    }

    public List<BillDetail> findDetails(long billId) {
        String q = """
                SELECT ID, BILL_ID, PRODUCT_DETAIL_ID, QUANTITY, UNIT_PRICE, TOTAL_PRICE
                  FROM BILL_DETAIL
                 WHERE BILL_ID = :billId AND DELETE_FLAG = 0
                 ORDER BY ID
                """;
        return sql.list(q, Map.of("billId", billId), BillDetail.class);
    }

    public List<BillHistoryView> findHistory(long billId) {
        return sql.list("""
                SELECT ID, OLD_STATUS, NEW_STATUS, NOTE, CREATED_AT
                  FROM BILL_HISTORY
                 WHERE BILL_ID = :billId AND DELETE_FLAG = 0
                 ORDER BY CREATED_AT DESC, ID DESC
                """, Map.of("billId", billId), BillHistoryView.class);
    }

    // ====================== VIEW (JOIN nhiều bảng, trả về view) ======================

    public List<BillLineView> findLineViews(long billId) {
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
        return sql.list(q, Map.of("billId", billId), BillLineView.class);
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

package com.example.clothingshop.repository.order;

import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class OrderOperationsRepository {
  private final AdminRepository db;

  public OrderOperationsRepository(AdminRepository db) {
    this.db = db;
  }

  public Map<String, Object> findBill(Object... args) {
    return db.one(
        "SELECT"
            + " ID,BILL_CODE,CUSTOMER_ID,RECEIVER_NAME,RECEIVER_PHONE,SHIPPING_ADDRESS,SUB_TOTAL,SHIPPING_FEE,DISCOUNT_AMOUNT,TOTAL_AMOUNT,PAYMENT_METHOD,PAYMENT_STATUS,STATUS,VOUCHER_CODE,CARRIER,TRACKING_CODE,NOTE,CREATED_AT,COMPLETED_AT,RETURN_STATUS,RETURN_VALUE,RETURNED_AT,SHIPPING_SERVICE,SHIPPING_SERVICE_NAME,SHIPPING_QUOTED_AT"
            + " FROM BILL WHERE ID=:i AND DELETE_FLAG=0",
        args);
  }

  public List<Map<String, Object>> listBillHistory(Object... args) {
    return db.rows(
        "SELECT h.ID,h.OLD_STATUS,h.NEW_STATUS,h.NOTE,m.USERNAME,h.CREATED_AT FROM BILL_HISTORY h"
            + " LEFT JOIN MANAGER m ON m.ID=h.MANAGER_ID WHERE BILL_ID=:i ORDER BY h.ID",
        args);
  }

  public List<Map<String, Object>> listInventoryDocument(Object... args) {
    return db.rows(
        "SELECT ID,DOC_CODE,DOC_TYPE,STATUS,POSTED_AT,WAREHOUSE_NAME FROM INVENTORY_DOCUMENT WHERE BILL_ID=:i"
            + " ORDER BY ID",
        args);
  }

  public Map<String, Object> findCustomer(Object... args) {
    return db.one(
        "SELECT c.ID FROM CUSTOMER c JOIN USERS u ON u.ID=c.USER_ID WHERE c.ID=:i AND"
            + " c.DELETE_FLAG=0 AND u.DELETE_FLAG=0 AND u.STATUS=1",
        args);
  }

  public int updateBill(Object... args) {
    return db.update("UPDATE BILL SET STOCK_RESERVED=1,VOUCHER_CODE=:v WHERE ID=:i", args);
  }

  public Map<String, Object> findBill2(Object... args) {
    return db.one("SELECT ID FROM BILL WHERE ID=:i AND DELETE_FLAG=0 FOR UPDATE", args);
  }

  public int updateBill2(Object... args) {
    return db.update("UPDATE BILL SET STOCK_RESERVED=0 WHERE ID=:i AND STOCK_RESERVED=1", args);
  }

  public void cancelDelivery(Object... args) {
    db.update("UPDATE BILL_VIETTEL_DISPATCH SET SEND_STATE='CANCELLED' WHERE BILL_ID=:i AND SEND_STATE='DRAFT'", args);
  }

  public int updateBill3(Object... args) {
    return db.update(
        "UPDATE BILL SET COMPLETED_AT=COALESCE(COMPLETED_AT,LOCALTIMESTAMP) WHERE ID=:i", args);
  }

  public int updateBill4(Object... args) {
    return db.update(
        "UPDATE BILL SET RETURN_STATUS=CASE WHEN RETURN_STATUS='NONE' THEN 'REQUESTED' ELSE"
            + " RETURN_STATUS END WHERE ID=:i",
        args);
  }

  public int updateBill5(Object... args) {
    return db.update("UPDATE BILL SET PAYMENT_STATUS=1 WHERE ID=:i AND PAYMENT_STATUS=0", args);
  }

  public void recordCashPayment(Object... args) {
    db.update("""
        INSERT INTO PAYMENT_HISTORY(BILL_ID,AMOUNT,PAYMENT_METHOD,STATUS,PAID_AT,NOTE)
        SELECT ID,TOTAL_AMOUNT,'COD',1,LOCALTIMESTAMP,:note FROM BILL WHERE ID=:i
        """, args);
  }

  public Map<String, Object> findBill3(Object... args) {
    return db.one(
        "SELECT STATUS,PAYMENT_STATUS,PAYMENT_METHOD,(SELECT COUNT(*) FROM BILL_VIETTEL_TRACKING v WHERE v.BILL_ID=BILL.ID) VIETTEL_LINKED FROM BILL WHERE ID=:i AND DELETE_FLAG=0 FOR UPDATE", args);
  }

  public int updateBill6(Object... args) {
    return db.update(
        "UPDATE BILL SET"
            + " CARRIER=:c,TRACKING_CODE=:t,HANDLED_BY=:a WHERE"
            + " ID=:i",
        args);
  }

  public List<Map<String, Object>> listBillRefund(Object... args) {
    return db.rows(
        "SELECT"
            + " r.ID,r.RETURN_DOCUMENT_ID,h.DOC_CODE,r.AMOUNT,r.STATUS,r.REFERENCE_CODE,r.REASON,r.CREATED_AT,r.PAID_AT,m.USERNAME"
            + " FROM BILL_REFUND r JOIN INVENTORY_DOCUMENT h ON h.ID=r.RETURN_DOCUMENT_ID JOIN"
            + " MANAGER m ON m.ID=r.CREATED_BY WHERE r.BILL_ID=:b ORDER BY r.ID",
        args);
  }

  public Map<String, Object> findBill4(Object... args) {
    return db.one("SELECT PAYMENT_STATUS FROM BILL WHERE ID=:b FOR UPDATE", args);
  }

  public Map<String, Object> findInventoryDocument(Object... args) {
    return db.one(
        "SELECT ID FROM INVENTORY_DOCUMENT h WHERE ID=:d AND BILL_ID=:b AND (DOC_TYPE='RETURN' OR (DOC_TYPE='RELEASE' AND EXISTS(SELECT 1 FROM BILL b WHERE b.ID=h.BILL_ID AND b.STATUS=4))) AND"
            + " STATUS='POSTED' AND NOT EXISTS(SELECT 1 FROM INVENTORY_DOCUMENT x WHERE"
            + " x.SOURCE_ID=h.ID AND x.STATUS='POSTED' AND x.DOC_TYPE='REVERSAL') FOR UPDATE",
        args);
  }

  public Map<String, Object> findBillRefund(Object... args) {
    return db.one("SELECT STATUS FROM BILL_REFUND WHERE ID=:i AND BILL_ID=:b FOR UPDATE", args);
  }

  public Map<String, Object> findInventoryDocumentLine(Object... args) {
    return db.one(
        "SELECT CASE WHEN (SELECT DOC_TYPE FROM INVENTORY_DOCUMENT WHERE ID=:d)='RELEASE' THEN (SELECT b.TOTAL_AMOUNT FROM BILL b JOIN INVENTORY_DOCUMENT h ON h.BILL_ID=b.ID WHERE h.ID=:d) ELSE (SELECT NVL(SUM(ROUND(RETURN_VALUE,0)),0) FROM INVENTORY_DOCUMENT_LINE WHERE"
            + " DOCUMENT_ID=:d) END CAP,(SELECT NVL(SUM(CASE WHEN STATUS IN ('PAID','PENDING') AND (:i"
            + " IS NULL OR ID<>:i) THEN AMOUNT ELSE 0 END),0) FROM BILL_REFUND WHERE"
            + " RETURN_DOCUMENT_ID=:d) USED FROM DUAL",
        args);
  }

  public long insertBillRefund(Object... args) {
    return db.insert(
        "INSERT INTO"
            + " BILL_REFUND(BILL_ID,RETURN_DOCUMENT_ID,AMOUNT,STATUS,REFERENCE_CODE,REASON,CREATED_BY,PAID_AT)"
            + " VALUES(:b,:d,:v,:s,:ref,:r,:a,CASE WHEN :s='PAID' THEN LOCALTIMESTAMP END)",
        args);
  }

  public int updateBillRefund(Object... args) {
    return db.update(
        "UPDATE BILL_REFUND SET"
            + " RETURN_DOCUMENT_ID=:d,AMOUNT=:v,STATUS=:s,REFERENCE_CODE=:ref,REASON=:r,CREATED_BY=:a,PAID_AT=CASE"
            + " WHEN :s='PAID' THEN LOCALTIMESTAMP END WHERE ID=:i",
        args);
  }

  public List<Map<String, Object>> eligibleVoucher(
      boolean consume, String code, java.math.BigDecimal subtotal, long customerId) {
    return db.rows(
        "SELECT v.ID,v.DISCOUNT_TYPE,v.DISCOUNT_VALUE,v.MAX_DISCOUNT FROM VOUCHER v WHERE"
            + " v.CODE=:c AND v.STATUS=1 AND v.STARTS_AT<=LOCALTIMESTAMP AND"
            + " (v.ENDS_AT IS NULL OR v.ENDS_AT>LOCALTIMESTAMP) AND"
            + " v.USED_COUNT<v.USAGE_LIMIT AND v.MIN_ORDER<=:s AND EXISTS (SELECT 1 FROM CUSTOMER c"
            + " WHERE c.ID=:customerId AND c.DELETE_FLAG=0 AND (v.NEW_ACCOUNT_DAYS IS NULL OR"
            + " c.CREATED_AT>=LOCALTIMESTAMP-NUMTODSINTERVAL(v.NEW_ACCOUNT_DAYS,'DAY')) AND"
            + " (v.NO_PREVIOUS_ORDERS=0 OR NOT EXISTS (SELECT 1 FROM BILL b WHERE"
            + " b.CUSTOMER_ID=c.ID AND b.DELETE_FLAG=0 AND b.STATUS NOT IN (4,6))) AND"
            + " (v.MINIMUM_ORDERS IS NULL OR (SELECT COUNT(*) FROM BILL b WHERE"
            + " b.CUSTOMER_ID=c.ID AND b.DELETE_FLAG=0 AND b.STATUS NOT IN (4,6))>=v.MINIMUM_ORDERS))"
            + (consume ? " FOR UPDATE" : ""),
        "c",
        code,
        "s",
        subtotal,
        "customerId",
        customerId);
  }

  public int claimVoucher(long customerId, long voucherId, java.math.BigDecimal subtotal) {
    return db.update(
        "UPDATE VOUCHER v SET USED_COUNT=USED_COUNT+1 WHERE v.ID=:i AND v.STATUS=1 AND"
            + " v.STARTS_AT<=LOCALTIMESTAMP AND (v.ENDS_AT IS NULL OR v.ENDS_AT>LOCALTIMESTAMP)"
            + " AND v.USED_COUNT<v.USAGE_LIMIT AND v.MIN_ORDER<=:s AND EXISTS (SELECT 1 FROM"
            + " CUSTOMER c WHERE c.ID=:customerId AND c.DELETE_FLAG=0 AND"
            + " (v.NEW_ACCOUNT_DAYS IS NULL OR"
            + " c.CREATED_AT>=LOCALTIMESTAMP-NUMTODSINTERVAL(v.NEW_ACCOUNT_DAYS,'DAY')) AND"
            + " (v.NO_PREVIOUS_ORDERS=0 OR NOT EXISTS (SELECT 1 FROM BILL b WHERE"
            + " b.CUSTOMER_ID=c.ID AND b.DELETE_FLAG=0 AND b.STATUS NOT IN (4,6))) AND"
            + " (v.MINIMUM_ORDERS IS NULL OR (SELECT COUNT(*) FROM BILL b WHERE"
            + " b.CUSTOMER_ID=c.ID AND b.DELETE_FLAG=0 AND b.STATUS NOT IN (4,6))>=v.MINIMUM_ORDERS))",
        "i",
        voucherId,
        "s",
        subtotal,
        "customerId",
        customerId);
  }

  public List<Map<String, Object>> voucherProductIds(Object... args) {
    return db.rows(
        "SELECT vp.PRODUCT_ID FROM VOUCHER_PRODUCT vp JOIN VOUCHER v ON v.ID=vp.VOUCHER_ID"
            + " WHERE v.CODE=:c",
        args);
  }

  public Map<String, Object> productForVariant(Object... args) {
    return db.one("SELECT PRODUCT_ID FROM PRODUCT_DETAIL WHERE ID=:i", args);
  }
}

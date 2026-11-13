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
        "SELECT ID,DOC_CODE,DOC_TYPE,STATUS,POSTED_AT FROM INVENTORY_DOCUMENT WHERE BILL_ID=:i"
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

  public void recordCodPayment(long id) {
    db.update("INSERT INTO PAYMENT_HISTORY(BILL_ID,AMOUNT,PAYMENT_METHOD,STATUS,PAID_AT,NOTE) "
        + "SELECT ID,TOTAL_AMOUNT,'COD',1,LOCALTIMESTAMP,'Thu tiền khi giao thành công' FROM BILL WHERE ID=:i "
        + "AND NOT EXISTS (SELECT 1 FROM PAYMENT_HISTORY WHERE BILL_ID=:i AND PAYMENT_METHOD='COD' AND STATUS=1 AND DELETE_FLAG=0)", "i", id);
  }

  public int updateVoucher(Object... args) {
    return db.update(
        "UPDATE VOUCHER SET USED_COUNT=USED_COUNT+1 WHERE ID=:i AND USED_COUNT<USAGE_LIMIT AND"
            + " STATUS=1 AND STARTS_AT<=LOCALTIMESTAMP AND ENDS_AT>LOCALTIMESTAMP AND"
            + " MIN_ORDER<=:s",
        args);
  }

  public Map<String, Object> findBill3(Object... args) {
    return db.one(
        "SELECT STATUS,PAYMENT_STATUS,PAYMENT_METHOD FROM BILL WHERE ID=:i AND DELETE_FLAG=0 FOR UPDATE", args);
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
        "SELECT ID FROM INVENTORY_DOCUMENT h WHERE ID=:d AND BILL_ID=:b AND DOC_TYPE='RETURN' AND"
            + " STATUS='POSTED' AND NOT EXISTS(SELECT 1 FROM INVENTORY_DOCUMENT x WHERE"
            + " x.SOURCE_ID=h.ID AND x.STATUS='POSTED' AND x.DOC_TYPE='REVERSAL') FOR UPDATE",
        args);
  }

  public Map<String, Object> findBillRefund(Object... args) {
    return db.one("SELECT STATUS FROM BILL_REFUND WHERE ID=:i AND BILL_ID=:b FOR UPDATE", args);
  }

  public Map<String, Object> findInventoryDocumentLine(Object... args) {
    return db.one(
        "SELECT (SELECT NVL(SUM(ROUND(RETURN_VALUE,0)),0) FROM INVENTORY_DOCUMENT_LINE WHERE"
            + " DOCUMENT_ID=:d) CAP,(SELECT NVL(SUM(CASE WHEN STATUS IN ('PAID','PENDING') AND (:i"
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
      boolean consume, String code, java.math.BigDecimal subtotal) {
    return db.rows(
        "SELECT ID,DISCOUNT_TYPE,DISCOUNT_VALUE,MAX_DISCOUNT FROM VOUCHER WHERE CODE=:c AND"
            + " STATUS=1 AND STARTS_AT<=LOCALTIMESTAMP AND ENDS_AT>LOCALTIMESTAMP AND"
            + " USED_COUNT<USAGE_LIMIT AND MIN_ORDER<=:s"
            + (consume ? " FOR UPDATE" : ""),
        "c",
        code,
        "s",
        subtotal);
  }
}

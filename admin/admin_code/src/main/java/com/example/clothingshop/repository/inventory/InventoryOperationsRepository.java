package com.example.clothingshop.repository.inventory;

import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.util.report.SaleAccounting;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryOperationsRepository {
  private final AdminRepository db;

  public InventoryOperationsRepository(AdminRepository db) {
    this.db = db;
  }

  public List<Map<String, Object>> listBill(Object... args) {
    return db.rows(
        "SELECT * FROM (SELECT b.ID BILL_ID,b.BILL_CODE,b.RECEIVER_NAME,b.STATUS ORDER_STATUS,d.ID"
            + " DOCUMENT_ID,d.DOC_CODE,d.DOC_TYPE,ROW_NUMBER() OVER(PARTITION BY b.ID ORDER BY CASE"
            + " WHEN d.DOC_TYPE='DISPATCH' AND d.STATUS<>'CANCELLED' THEN 0 ELSE 1 END,d.ID DESC)"
            + " RN FROM BILL b JOIN INVENTORY_DOCUMENT d ON d.BILL_ID=b.ID AND d.DOC_TYPE IN"
            + " ('SALE','DISPATCH','RESERVE') WHERE b.DELETE_FLAG=0 AND (LOWER(b.BILL_CODE) LIKE :q"
            + " OR LOWER(b.RECEIVER_NAME) LIKE :q OR b.RECEIVER_PHONE LIKE :q)) WHERE RN=1 ORDER BY"
            + " BILL_ID DESC FETCH FIRST 50 ROWS ONLY",
        args);
  }

  public List<Map<String, Object>> listProductDetail(Object... args) {
    return db.rows(
        "SELECT pd.ID,pd.SKU,p.PRODUCT_NAME,c.COLOR_NAME,pd.SIZE_NAME,pd.QUANTITY FROM"
            + " PRODUCT_DETAIL pd JOIN PRODUCT p ON p.ID=pd.PRODUCT_ID JOIN COLOR c ON"
            + " c.ID=pd.COLOR_ID WHERE pd.DELETE_FLAG=0 AND (LOWER(pd.SKU) LIKE :q OR"
            + " LOWER(p.PRODUCT_NAME) LIKE :q) ORDER BY pd.ID DESC FETCH FIRST 30 ROWS ONLY",
        args);
  }

  public Map<String, Object> pageInventoryDocumentLine(int page, int size, Object... args) {
    return db.page(
        "SELECT"
            + " d.ID,d.DOC_CODE,d.DOC_TYPE,d.STATUS,d.REASON,d.BILL_ID,b.BILL_CODE,b.RECEIVER_NAME,b.STATUS"
            + " ORDER_STATUS,d.SOURCE_ID,m.USERNAME CREATED_BY,d.CREATED_AT,d.POSTED_AT,(SELECT"
            + " SUM(l.QUANTITY*l.UNIT_COST) FROM INVENTORY_DOCUMENT_LINE l WHERE l.DOCUMENT_ID=d.ID"
            + " AND l.UNIT_COST IS NOT NULL) PURCHASE_TOTAL FROM INVENTORY_DOCUMENT d LEFT JOIN"
            + " BILL b ON b.ID=d.BILL_ID LEFT JOIN MANAGER m ON m.ID=d.CREATED_BY WHERE (:t IS NULL"
            + " OR d.DOC_TYPE=:t) AND (:s IS NULL OR d.STATUS=:s) AND (LOWER(d.DOC_CODE) LIKE :q OR"
            + " LOWER(d.REASON) LIKE :q OR LOWER(b.BILL_CODE) LIKE :q OR LOWER(b.RECEIVER_NAME)"
            + " LIKE :q) ORDER BY d.ID DESC",
        page,
        size,
        args);
  }

  public Map<String, Object> findInventoryDocument(Object... args) {
    return db.one(
        "SELECT"
            + " d.ID,d.DOC_CODE,d.DOC_TYPE,d.STATUS,d.REASON,d.BILL_ID,b.BILL_CODE,b.RECEIVER_NAME,b.STATUS"
            + " ORDER_STATUS,d.SOURCE_ID,d.CREATED_BY,m.USERNAME CREATOR,a.USERNAME"
            + " APPROVER,d.CANCEL_REASON,d.CREATED_AT,d.POSTED_AT,d.CANCELLED_AT FROM"
            + " INVENTORY_DOCUMENT d LEFT JOIN BILL b ON b.ID=d.BILL_ID LEFT JOIN MANAGER m ON"
            + " m.ID=d.CREATED_BY LEFT JOIN MANAGER a ON a.ID=d.APPROVED_BY WHERE d.ID=:i",
        args);
  }

  public List<Map<String, Object>> listInventoryDocumentLine(Object... args) {
    return db.rows(
        "SELECT"
            + " l.ID,l.PRODUCT_DETAIL_ID,pd.SKU,p.PRODUCT_NAME,c.COLOR_NAME,pd.SIZE_NAME,l.QUANTITY,l.EXPECTED_QUANTITY,l.BEFORE_QUANTITY,l.AFTER_QUANTITY,l.STOCK_DELTA,l.RESTOCK,l.CONDITION_NOTE,l.RETURN_VALUE,l.PHYSICAL_BEFORE,l.PHYSICAL_AFTER,l.RESERVED_BEFORE,l.RESERVED_AFTER,l.VALUE_BEFORE,l.VALUE_AFTER,l.COST_UNIT,l.QUANTITY*l.COST_UNIT"
            + " COST_TOTAL,l.UNIT_COST,l.QUANTITY*l.UNIT_COST LINE_COST FROM"
            + " INVENTORY_DOCUMENT_LINE l JOIN PRODUCT_DETAIL pd ON pd.ID=l.PRODUCT_DETAIL_ID JOIN"
            + " PRODUCT p ON p.ID=pd.PRODUCT_ID JOIN COLOR c ON c.ID=pd.COLOR_ID WHERE"
            + " l.DOCUMENT_ID=:i ORDER BY l.ID",
        args);
  }

  public List<Map<String, Object>> listManager(Object... args) {
    return db.rows(
        "SELECT m.ID FROM MANAGER m JOIN MANAGER_ROLE r ON r.ID=m.MANAGER_ROLE_ID WHERE m.ID=:i AND"
            + " m.STATUS=1 AND m.DELETE_FLAG=0 AND r.ROLE_CODE='ADMIN' AND r.DELETE_FLAG=0",
        args);
  }

  public Map<String, Object> findDual(Object... args) {
    return db.one("SELECT SHOP_INVENTORY_SEQ.NEXTVAL N FROM DUAL", args);
  }

  public long insertInventoryDocument(Object... args) {
    return db.insert(
        "INSERT INTO"
            + " INVENTORY_DOCUMENT(DOC_CODE,DOC_TYPE,REASON,BILL_ID,SOURCE_ID,CREATED_BY,STATUS,APPROVED_BY,POSTED_AT)"
            + " VALUES(:c,:t,:r,:b,:s,:a,:st,CASE WHEN :st='POSTED' THEN :a END,CASE WHEN"
            + " :st='POSTED' THEN LOCALTIMESTAMP END)",
        args);
  }

  public List<Map<String, Object>> listBillDetail(Object... args) {
    return db.rows(
        "SELECT PRODUCT_DETAIL_ID,SUM(QUANTITY) QTY FROM BILL_DETAIL WHERE BILL_ID=:b AND"
            + " DELETE_FLAG=0 GROUP BY PRODUCT_DETAIL_ID ORDER BY PRODUCT_DETAIL_ID",
        args);
  }

  public Map<String, Object> findBill(Object... args) {
    return db.one("SELECT ID FROM BILL WHERE ID=:b FOR UPDATE", args);
  }

  public Map<String, Object> findProductDetail(Object... args) {
    return db.one(
        "SELECT"
            + " ID,PHYSICAL_QUANTITY,RESERVED_QUANTITY,QUANTITY,AVERAGE_COST,KNOWN_VALUE,UNKNOWN_QUANTITY,STOCK_VERSION"
            + " FROM PRODUCT_DETAIL WHERE ID=:i AND DELETE_FLAG=0 FOR UPDATE",
        args);
  }

  public Map<String, Object> findBill2(Object... args) {
    return db.one(
        "SELECT ID FROM BILL WHERE ID=:b AND DISPATCHED=1 AND STATUS IN (3,5,6) AND DELETE_FLAG=0",
        args);
  }

  public Map<String, Object> findInventoryDocument2(Object... args) {
    return db.one(
        "SELECT STATUS,CREATED_BY,DOC_TYPE FROM INVENTORY_DOCUMENT WHERE ID=:i FOR UPDATE", args);
  }

  public int updateInventoryDocument(Object... args) {
    return db.update(
        "UPDATE INVENTORY_DOCUMENT SET DOC_TYPE=:t,REASON=:r,BILL_ID=:b WHERE ID=:i", args);
  }

  public int deleteInventoryDocumentLine(Object... args) {
    return db.update("DELETE FROM INVENTORY_DOCUMENT_LINE WHERE DOCUMENT_ID=:i", args);
  }

  public Map<String, Object> findProductDetail2(Object... args) {
    return db.one(
        "SELECT PHYSICAL_QUANTITY,RESERVED_QUANTITY,STOCK_VERSION FROM PRODUCT_DETAIL WHERE ID=:i"
            + " AND DELETE_FLAG=0",
        args);
  }

  public List<Map<String, Object>> listBillDetail2(Object... args) {
    return db.rows(
        "SELECT ID FROM BILL_DETAIL WHERE BILL_ID=:b AND PRODUCT_DETAIL_ID=:p AND DELETE_FLAG=0",
        args);
  }

  public int updateInventoryDocumentLine(Object... args) {
    return db.update(
        "INSERT INTO"
            + " INVENTORY_DOCUMENT_LINE(DOCUMENT_ID,PRODUCT_DETAIL_ID,QUANTITY,EXPECTED_QUANTITY,EXPECTED_RESERVED,EXPECTED_VERSION,RESTOCK,CONDITION_NOTE,UNIT_COST)"
            + " VALUES(:d,:p,:q,:e,:er,:version,:r,:n,:c)",
        args);
  }

  public long insertInventoryDocumentLine(Object... args) {
    return db.insert(
        "INSERT INTO INVENTORY_DOCUMENT_LINE(DOCUMENT_ID,PRODUCT_DETAIL_ID,QUANTITY)"
            + " VALUES(:d,:p,:q)",
        args);
  }

  public int updateProductDetail(Object... args) {
    return db.update(
        "UPDATE PRODUCT_DETAIL SET"
            + " STOCK_VERSION=STOCK_VERSION+1,PHYSICAL_QUANTITY=PHYSICAL_QUANTITY+:p,RESERVED_QUANTITY=RESERVED_QUANTITY+:r,QUANTITY=PHYSICAL_QUANTITY+:p-RESERVED_QUANTITY-:r"
            + " WHERE ID=:i",
        args);
  }

  public int updateInventoryDocumentLine2(Object... args) {
    return db.update(
        "UPDATE INVENTORY_DOCUMENT_LINE SET"
            + " COST_UNIT=:u,BEFORE_QUANTITY=:ab,AFTER_QUANTITY=:aa,STOCK_DELTA=:d,PHYSICAL_BEFORE=:pb,PHYSICAL_AFTER=:pa,RESERVED_BEFORE=:rb,RESERVED_AFTER=:ra,VALUE_BEFORE=:vb,VALUE_AFTER=:va"
            + " WHERE ID=:i",
        args);
  }

  public int updateInventoryHistory(Object... args) {
    return db.update(
        "INSERT INTO"
            + " INVENTORY_HISTORY(PRODUCT_DETAIL_ID,QUANTITY_CHANGE,REASON,MANAGER_ID,BILL_ID,DOCUMENT_ID,MOVEMENT_TYPE,BEFORE_QUANTITY,AFTER_QUANTITY,PHYSICAL_CHANGE,RESERVED_CHANGE,PHYSICAL_BEFORE,PHYSICAL_AFTER,RESERVED_BEFORE,RESERVED_AFTER,VALUE_BEFORE,VALUE_AFTER,HISTORY_SCOPE)"
            + " VALUES(:p,:q,:reason,:actor,:b,:d,:t,:ab,:aa,:dp,:dr,:pb,:pa,:rb,:ra,:vb,:va,'PHYSICAL_V8')",
        args);
  }

  public int updateProductDetail2(Object... args) {
    return db.update(
        "UPDATE PRODUCT_DETAIL SET RESERVED_QUANTITY=RESERVED_QUANTITY-:q,QUANTITY=QUANTITY+:q"
            + " WHERE ID=:i",
        args);
  }

  public int updateInventoryDocument2(Object... args) {
    return db.update(
        "UPDATE INVENTORY_DOCUMENT SET STATUS='CANCELLED',CANCEL_REASON='Đơn hủy trước"
            + " xuất',CANCELLED_BY=:a,CANCELLED_AT=LOCALTIMESTAMP WHERE BILL_ID=:b AND"
            + " DOC_TYPE='DISPATCH' AND STATUS='DRAFT'",
        args);
  }

  public Map<String, Object> findBill3(Object... args) {
    return db.one("SELECT STATUS,DISPATCHED,STOCK_RESERVED FROM BILL WHERE ID=:b", args);
  }

  public List<Map<String, Object>> listInventoryDocument(Object... args) {
    return db.rows(
        "SELECT ID FROM INVENTORY_DOCUMENT WHERE BILL_ID=:b AND DOC_TYPE='DISPATCH' AND"
            + " STATUS='DRAFT'",
        args);
  }

  public Map<String, Object> findInventoryDocument3(Object... args) {
    return db.one("SELECT BILL_ID FROM INVENTORY_DOCUMENT WHERE ID=:i", args);
  }

  public Map<String, Object> findInventoryDocument4(Object... args) {
    return db.one("SELECT * FROM INVENTORY_DOCUMENT WHERE ID=:i FOR UPDATE", args);
  }

  public List<Map<String, Object>> listInventoryDocumentLine2(Object... args) {
    return db.rows(
        "SELECT * FROM INVENTORY_DOCUMENT_LINE WHERE DOCUMENT_ID=:i ORDER BY PRODUCT_DETAIL_ID",
        args);
  }

  public Map<String, Object> findBill4(Object... args) {
    return db.one("SELECT DISPATCHED,STATUS FROM BILL WHERE ID=:b", args);
  }

  public Map<String, Object> findRecord(Object... args) {
    return db.one(
        "SELECT SUM(QUANTITY) SOLD,SUM(TOTAL_PRICE-ALLOCATED_DISCOUNT) LINE_NET FROM ("
            + SaleAccounting.ALL_LINES
            + ") WHERE BILL_ID=:b AND PRODUCT_DETAIL_ID=:p GROUP BY BILL_ID,PRODUCT_DETAIL_ID",
        args);
  }

  public Map<String, Object> findInventoryDocumentLine(Object... args) {
    return db.one(
        "SELECT NVL(SUM(l.QUANTITY),0) N,NVL(SUM(ROUND(l.RETURN_VALUE,0)),0)"
            + " V,NVL(SUM(l.COGS_VALUE),0) COGS FROM INVENTORY_DOCUMENT_LINE l JOIN"
            + " INVENTORY_DOCUMENT h ON h.ID=l.DOCUMENT_ID WHERE h.DOC_TYPE='RETURN' AND"
            + " h.STATUS='POSTED' AND h.BILL_ID=:b AND l.PRODUCT_DETAIL_ID=:p AND NOT EXISTS(SELECT"
            + " 1 FROM INVENTORY_DOCUMENT x WHERE x.SOURCE_ID=h.ID AND x.DOC_TYPE='REVERSAL' AND"
            + " x.STATUS='POSTED')",
        args);
  }

  public Map<String, Object> findBillDetail(Object... args) {
    return db.one(
        "SELECT SUM(COST_TOTAL) V FROM BILL_DETAIL WHERE BILL_ID=:b AND PRODUCT_DETAIL_ID=:v AND"
            + " DELETE_FLAG=0",
        args);
  }

  public Map<String, Object> findInventoryDocumentLine2(Object... args) {
    return db.one(
        "SELECT l.*,h.DOC_TYPE SOURCE_TYPE FROM INVENTORY_DOCUMENT_LINE l JOIN INVENTORY_DOCUMENT h"
            + " ON h.ID=l.DOCUMENT_ID WHERE l.DOCUMENT_ID=:d AND l.PRODUCT_DETAIL_ID=:p AND"
            + " h.STATUS='POSTED'",
        args);
  }

  public List<Map<String, Object>> listInventoryHistory(Object... args) {
    return db.rows(
        "SELECT ID FROM INVENTORY_HISTORY WHERE PRODUCT_DETAIL_ID=:p AND ID>(SELECT NVL(MAX(ID),0)"
            + " FROM INVENTORY_HISTORY WHERE DOCUMENT_ID=:d) AND HISTORY_SCOPE='PHYSICAL_V8'",
        args);
  }

  public int updateInventoryDocumentLine3(Object... args) {
    return db.update(
        "UPDATE INVENTORY_DOCUMENT_LINE SET RETURN_VALUE=:v,COGS_VALUE=:c WHERE ID=:i", args);
  }

  public int updateInventoryDocument3(Object... args) {
    return db.update(
        "UPDATE INVENTORY_DOCUMENT SET STATUS='POSTED',APPROVED_BY=:a,POSTED_AT=LOCALTIMESTAMP"
            + " WHERE ID=:i",
        args);
  }

  public int updateBill(Object... args) {
    return db.update(
        "UPDATE BILL SET STATUS=2,DISPATCHED=1,STOCK_RESERVED=0,HANDLED_BY=:a WHERE ID=:b", args);
  }

  public int updateBillHistory(Object... args) {
    return db.update(
        "INSERT INTO BILL_HISTORY(BILL_ID,OLD_STATUS,NEW_STATUS,NOTE,MANAGER_ID)"
            + " VALUES(:b,1,2,:n,:a)",
        args);
  }

  public Map<String, Object> findInventoryDocument5(Object... args) {
    return db.one("SELECT DOC_TYPE FROM INVENTORY_DOCUMENT WHERE ID=:s", args);
  }

  public Map<String, Object> findBill5(Object... args) {
    return db.one("SELECT COMPLETED_AT FROM BILL WHERE ID=:b", args);
  }

  public Map<String, Object> findInventoryDocumentLine3(Object... args) {
    return db.one(
        "SELECT NVL(SUM(ROUND(RETURN_VALUE,0)),0) V FROM INVENTORY_DOCUMENT_LINE WHERE"
            + " DOCUMENT_ID=:d",
        args);
  }

  public int updateInventoryDocument4(Object... args) {
    return db.update("UPDATE INVENTORY_DOCUMENT SET REVENUE_DELTA=:v WHERE ID=:i", args);
  }

  public List<Map<String, Object>> listInventoryDocumentLine3(Object... args) {
    return db.rows(
        "SELECT"
            + " PRODUCT_DETAIL_ID,PHYSICAL_BEFORE,PHYSICAL_AFTER,RESERVED_BEFORE,RESERVED_AFTER,COST_UNIT,VALUE_BEFORE,VALUE_AFTER"
            + " FROM INVENTORY_DOCUMENT_LINE WHERE DOCUMENT_ID=:d",
        args);
  }

  public Map<String, Object> findInventoryDocumentLine4(Object... args) {
    return db.one(
        "SELECT NVL(SUM(l.QUANTITY),0) Q,NVL(SUM(ROUND(l.RETURN_VALUE,0)),0) V FROM"
            + " INVENTORY_DOCUMENT_LINE l JOIN INVENTORY_DOCUMENT h ON h.ID=l.DOCUMENT_ID WHERE"
            + " h.BILL_ID=:b AND h.DOC_TYPE='RETURN' AND h.STATUS='POSTED' AND NOT EXISTS(SELECT 1"
            + " FROM INVENTORY_DOCUMENT x WHERE x.SOURCE_ID=h.ID AND x.STATUS='POSTED' AND"
            + " x.DOC_TYPE='REVERSAL')",
        args);
  }

  public Map<String, Object> findBillDetail2(Object... args) {
    return db.one(
        "SELECT SUM(QUANTITY) Q FROM BILL_DETAIL WHERE BILL_ID=:b AND DELETE_FLAG=0", args);
  }

  public int updateBill2(Object... args) {
    return db.update(
        "UPDATE BILL SET RETURN_STATUS=:s,RETURN_VALUE=:v,RETURNED_AT=LOCALTIMESTAMP,STATUS=CASE"
            + " WHEN COMPLETED_AT IS NULL THEN STATUS WHEN :q=:sold THEN 5 ELSE 3 END,HANDLED_BY=:a"
            + " WHERE ID=:b",
        args);
  }

  public Map<String, Object> findInventoryDocument6(Object... args) {
    return db.one("SELECT STATUS,CREATED_BY FROM INVENTORY_DOCUMENT WHERE ID=:i FOR UPDATE", args);
  }

  public int updateInventoryDocument5(Object... args) {
    return db.update(
        "UPDATE INVENTORY_DOCUMENT SET"
            + " STATUS='CANCELLED',CANCELLED_BY=:a,CANCEL_REASON=:r,CANCELLED_AT=LOCALTIMESTAMP"
            + " WHERE ID=:i",
        args);
  }

  public Map<String, Object> findInventoryDocument7(Object... args) {
    return db.one(
        "SELECT ID FROM INVENTORY_DOCUMENT WHERE ID=:i AND STATUS='POSTED' AND DOC_TYPE IN"
            + " ('RECEIPT','ISSUE','COUNT','RETURN') FOR UPDATE",
        args);
  }

  public List<Map<String, Object>> listInventoryDocument2(Object... args) {
    return db.rows(
        "SELECT ID FROM INVENTORY_DOCUMENT WHERE SOURCE_ID=:i AND STATUS<>'CANCELLED'", args);
  }

  public List<Map<String, Object>> listBillRefund(Object... args) {
    return db.rows(
        "SELECT ID FROM BILL_REFUND WHERE RETURN_DOCUMENT_ID=:d AND STATUS IN ('PENDING','PAID')",
        args);
  }

  public int updateInventoryDocumentLine4(Object... args) {
    return db.update(
        "INSERT INTO"
            + " INVENTORY_DOCUMENT_LINE(DOCUMENT_ID,PRODUCT_DETAIL_ID,QUANTITY,EXPECTED_QUANTITY,EXPECTED_RESERVED,UNIT_COST)"
            + " SELECT"
            + " :d,l.PRODUCT_DETAIL_ID,l.QUANTITY,p.PHYSICAL_QUANTITY,p.RESERVED_QUANTITY,l.UNIT_COST"
            + " FROM INVENTORY_DOCUMENT_LINE l JOIN PRODUCT_DETAIL p ON p.ID=l.PRODUCT_DETAIL_ID"
            + " WHERE l.DOCUMENT_ID=:s",
        args);
  }

  public Map<String, Object> findProductDetail3(Object... args) {
    return db.one(
        "SELECT"
            + " v.ID,v.SKU,v.COLOR_ID,c.COLOR_NAME,v.SIZE_NAME,v.PHYSICAL_QUANTITY,v.RESERVED_QUANTITY,v.QUANTITY,v.AVERAGE_COST,v.KNOWN_VALUE,v.UNKNOWN_QUANTITY"
            + " FROM PRODUCT_DETAIL v JOIN COLOR c ON c.ID=v.COLOR_ID WHERE v.ID=:i",
        args);
  }

  public List<Map<String, Object>> listInventoryDocumentLine4(Object... args) {
    return db.rows(
        "SELECT h.DOC_CODE,h.ID DOCUMENT_ID,l.ID,l.QUANTITY,l.UNIT_COST,l.COST_UNIT,h.POSTED_AT"
            + " FROM INVENTORY_DOCUMENT_LINE l JOIN INVENTORY_DOCUMENT h ON h.ID=l.DOCUMENT_ID"
            + " WHERE l.PRODUCT_DETAIL_ID=:i AND h.DOC_TYPE='RECEIPT' AND h.STATUS='POSTED' ORDER"
            + " BY h.ID",
        args);
  }

  public Map<String, Object> findProductDetail4(Object... args) {
    return db.one(
        "SELECT PHYSICAL_QUANTITY,AVERAGE_COST,KNOWN_VALUE,UNKNOWN_QUANTITY FROM PRODUCT_DETAIL"
            + " WHERE ID=:i FOR UPDATE",
        args);
  }

  public int updateStockCostAdjustment(Object... args) {
    return db.update(
        "INSERT INTO"
            + " STOCK_COST_ADJUSTMENT(PRODUCT_DETAIL_ID,PHYSICAL_QUANTITY,BEFORE_COST,AFTER_COST,BEFORE_KNOWN_VALUE,AFTER_VALUE,REASON,EVIDENCE,CREATED_BY)"
            + " VALUES(:i,:q,:before,:after,:bv,:av,:r,:e,:a)",
        args);
  }

  public int updateProductDetail3(Object... args) {
    return db.update(
        "UPDATE PRODUCT_DETAIL SET"
            + " STOCK_VERSION=STOCK_VERSION+1,AVERAGE_COST=:c,KNOWN_VALUE=:v,UNKNOWN_QUANTITY=0,COST_RECONCILED_AT=LOCALTIMESTAMP"
            + " WHERE ID=:i",
        args);
  }

  public Map<String, Object> findProductDetail5(Object... args) {
    return db.one("SELECT PRODUCT_ID FROM PRODUCT_DETAIL WHERE ID=:i", args);
  }

  public Map<String, Object> findProduct(Object... args) {
    return db.one("SELECT ID FROM PRODUCT WHERE ID=:i FOR UPDATE", args);
  }

  public Map<String, Object> findProductDetail6(Object... args) {
    return db.one("SELECT ID FROM PRODUCT_DETAIL WHERE ID=:i FOR UPDATE", args);
  }

  public Map<String, Object> findProductDetail7(Object... args) {
    return db.one("SELECT AVERAGE_COST FROM PRODUCT_DETAIL WHERE ID=:i", args);
  }

  public Map<String, Object> findBillDetail3(Object... args) {
    return db.one(
        "SELECT SUM(QUANTITY) Q,SUM(QUANTITY*COST_UNIT) V,COUNT(*) N,COUNT(COST_UNIT) KNOWN FROM"
            + " BILL_DETAIL WHERE BILL_ID=:b AND PRODUCT_DETAIL_ID=:v AND DELETE_FLAG=0",
        args);
  }

  public int updateProductDetail4(Object... args) {
    return db.update(
        "UPDATE PRODUCT_DETAIL SET AVERAGE_COST=:a,KNOWN_VALUE=:v,UNKNOWN_QUANTITY=:u WHERE ID=:i",
        args);
  }

  public int updateBillDetail(Object... args) {
    return db.update(
        "UPDATE BILL_DETAIL d SET COST_UNIT=(SELECT v.AVERAGE_COST FROM PRODUCT_DETAIL v WHERE"
            + " v.ID=d.PRODUCT_DETAIL_ID),COST_TOTAL=(SELECT ROUND(d.QUANTITY*v.AVERAGE_COST,0)"
            + " FROM PRODUCT_DETAIL v WHERE v.ID=d.PRODUCT_DETAIL_ID) WHERE d.BILL_ID=:b",
        args);
  }
}

package com.example.clothingshop.repository.admin;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class AdminOperationsRepository {
  private final AdminRepository db;

  public AdminOperationsRepository(AdminRepository db) {
    this.db = db;
  }

  public List<Map<String, Object>> listManager(Object... args) {
    return db.rows(
        "SELECT m.ID,m.USERNAME,m.FULL_NAME,m.EMAIL,m.STATUS,m.MANAGER_ROLE_ID,r.ROLE_CODE FROM"
            + " MANAGER m JOIN MANAGER_ROLE r ON r.ID=m.MANAGER_ROLE_ID WHERE m.DELETE_FLAG=0 AND"
            + " (LOWER(m.USERNAME) LIKE :q OR LOWER(m.FULL_NAME) LIKE :q) AND (:r IS NULL OR"
            + " r.ROLE_CODE=:r) AND (:s IS NULL OR m.STATUS=:s) ORDER BY m.ID",
        args);
  }

  public List<Map<String, Object>> listCategory(Object... args) {
    return db.rows(
        "SELECT * FROM (SELECT ID,CATEGORY_NAME,PARENT_ID,STATUS,CASE WHEN"
            + " INSTR(SYS_CONNECT_BY_PATH(STATUS,'/'),'/0')>0 THEN 0 ELSE 1 END"
            + " EFFECTIVE_STATUS,LEVEL DEPTH,SYS_CONNECT_BY_PATH(CATEGORY_NAME,' / ') CATEGORY_PATH"
            + " FROM CATEGORY START WITH PARENT_ID IS NULL CONNECT BY NOCYCLE PRIOR ID=PARENT_ID"
            + " ORDER SIBLINGS BY CATEGORY_NAME) WHERE LOWER(CATEGORY_PATH) LIKE :q",
        args);
  }

  public Map<String, Object> findCategory(Object... args) {
    return db.one(
        "SELECT (SELECT COUNT(*)-1 FROM CATEGORY START WITH ID=:i CONNECT BY NOCYCLE PRIOR"
            + " ID=PARENT_ID) CHILDREN,(SELECT COUNT(*) FROM PRODUCT WHERE DELETE_FLAG=0 AND"
            + " CATEGORY_ID IN (SELECT ID FROM CATEGORY START WITH ID=:i CONNECT BY NOCYCLE PRIOR"
            + " ID=PARENT_ID)) PRODUCTS FROM DUAL",
        args);
  }

  public List<Map<String, Object>> listVoucher(Object... args) {
    return db.rows(
        "SELECT * FROM (SELECT v.*,USAGE_LIMIT-USED_COUNT REMAINING,CASE WHEN STATUS=0 THEN"
            + " 'DISABLED' WHEN LOCALTIMESTAMP<STARTS_AT THEN 'UPCOMING' WHEN"
            + " LOCALTIMESTAMP>=ENDS_AT THEN 'EXPIRED' WHEN USED_COUNT>=USAGE_LIMIT THEN"
            + " 'EXHAUSTED' ELSE 'ACTIVE' END EFFECTIVE_STATUS FROM VOUCHER v) WHERE LOWER(CODE)"
            + " LIKE :q AND (:s IS NULL OR EFFECTIVE_STATUS=:s) ORDER BY ID DESC",
        args);
  }

  public List<Map<String, Object>> listBill(Object... args) {
    return db.rows(
        "SELECT b.ID,b.BILL_CODE,b.STATUS,b.DISCOUNT_AMOUNT,b.CREATED_AT FROM BILL b JOIN VOUCHER v"
            + " ON v.CODE=b.VOUCHER_CODE WHERE v.ID=:i ORDER BY b.ID DESC",
        args);
  }

  public Map<String, Object> pageBill(int page, int size, Object... args) {
    return db.page(
        "SELECT"
            + " ID,BILL_CODE,RECEIVER_NAME,TOTAL_AMOUNT,STATUS,PAYMENT_METHOD,PAYMENT_STATUS,RETURN_STATUS,CREATED_AT,DISPATCHED"
            + " FROM BILL WHERE DELETE_FLAG=0 AND (LOWER(BILL_CODE) LIKE :q OR LOWER(RECEIVER_NAME)"
            + " LIKE :q) AND (:s IS NULL OR STATUS=:s) AND (:pm IS NULL OR PAYMENT_METHOD=:pm) AND"
            + " (:ps IS NULL OR PAYMENT_STATUS=:ps) AND (:rs IS NULL OR RETURN_STATUS=:rs) AND (:f"
            + " IS NULL OR CREATED_AT>=:f) AND (:t IS NULL OR CREATED_AT<:t) ORDER BY ID DESC",
        page,
        size,
        args);
  }

  public Map<String, Object> pageInventoryHistory(int page, int size, Object... args) {
    return db.page(
        "SELECT"
            + " h.ID,h.DOCUMENT_ID,h.PRODUCT_DETAIL_ID,v.SKU,p.PRODUCT_NAME,h.QUANTITY_CHANGE,h.REASON,m.USERNAME,h.BILL_ID,b.BILL_CODE,h.CREATED_AT,h.MOVEMENT_TYPE,h.BEFORE_QUANTITY,h.AFTER_QUANTITY,h.PHYSICAL_BEFORE,h.PHYSICAL_AFTER,h.RESERVED_BEFORE,h.RESERVED_AFTER,h.VALUE_BEFORE,h.VALUE_AFTER,h.HISTORY_SCOPE,d.DOC_CODE"
            + " FROM INVENTORY_HISTORY h JOIN PRODUCT_DETAIL v ON v.ID=h.PRODUCT_DETAIL_ID JOIN"
            + " PRODUCT p ON p.ID=v.PRODUCT_ID LEFT JOIN MANAGER m ON m.ID=h.MANAGER_ID LEFT JOIN"
            + " BILL b ON b.ID=h.BILL_ID LEFT JOIN INVENTORY_DOCUMENT d ON d.ID=h.DOCUMENT_ID WHERE"
            + " (LOWER(v.SKU) LIKE :q OR LOWER(p.PRODUCT_NAME) LIKE :q) AND (:v IS NULL OR v.ID=:v)"
            + " AND (:kind IS NULL OR h.MOVEMENT_TYPE=:kind) AND (:f IS NULL OR h.CREATED_AT>=:f)"
            + " AND (:t IS NULL OR h.CREATED_AT<:t) AND LOWER(NVL(d.DOC_CODE,' ')) LIKE :doc AND"
            + " LOWER(NVL(b.BILL_CODE,' ')) LIKE :bill ORDER BY h.ID DESC",
        page,
        size,
        args);
  }

  public Map<String, Object> pageAdminAudit(int page, int size, Object... args) {
    return db.page(
        "SELECT"
            + " a.ID,m.USERNAME,a.ACTION,a.OBJECT_TYPE,a.OBJECT_ID,a.CREATED_AT,a.REASON,DBMS_LOB.SUBSTR(a.BEFORE_DATA,4000,1)"
            + " BEFORE_DATA,DBMS_LOB.SUBSTR(a.AFTER_DATA,4000,1) AFTER_DATA FROM ADMIN_AUDIT a LEFT"
            + " JOIN MANAGER m ON m.ID=a.MANAGER_ID WHERE LOWER(NVL(m.USERNAME,' ')) LIKE :q AND"
            + " (:act IS NULL OR a.ACTION=:act) AND (:obj IS NULL OR a.OBJECT_TYPE=:obj) AND (:f IS"
            + " NULL OR a.CREATED_AT>=:f) AND (:t IS NULL OR a.CREATED_AT<:t) ORDER BY a.ID DESC",
        page,
        size,
        args);
  }

  public Map<String, Object> findManager(Object... args) {
    return db.one("SELECT ID FROM MANAGER WHERE USERNAME=:u AND STATUS=1 AND DELETE_FLAG=0", args);
  }

  public int lockColor(Object... args) {
    return db.update("LOCK TABLE COLOR IN EXCLUSIVE MODE", args);
  }

  public List<Map<String, Object>> listColor(Object... args) {
    return db.rows(
        "SELECT ID,COLOR_NAME NAME,HEX_CODE FROM COLOR WHERE LOWER(TRIM(COLOR_NAME))=LOWER(:n) AND"
            + " DELETE_FLAG=0 ORDER BY ID",
        args);
  }

  public long insertColor(Object... args) {
    return db.insert("INSERT INTO COLOR(COLOR_NAME,HEX_CODE) VALUES(:n,:h)", args);
  }

  public Map<String, Object> findColor(Object... args) {
    return db.one("SELECT ID,COLOR_NAME NAME,HEX_CODE FROM COLOR WHERE ID=:i", args);
  }

  public int lockCategory(Object... args) {
    return db.update("LOCK TABLE CATEGORY IN EXCLUSIVE MODE", args);
  }

  public Map<String, Object> findCategory2(Object... args) {
    return db.one("SELECT ID FROM CATEGORY WHERE ID=:i", args);
  }

  public Map<String, Object> findCategory3(Object... args) {
    return db.one("SELECT PARENT_ID FROM CATEGORY WHERE ID=:i", args);
  }

  public long insertCategory(Object... args) {
    return db.insert("INSERT INTO CATEGORY(CATEGORY_NAME,PARENT_ID,STATUS) VALUES(:n,:p,:s)", args);
  }

  public int updateCategory(Object... args) {
    return db.update(
        "UPDATE CATEGORY SET CATEGORY_NAME=:n,PARENT_ID=:p,STATUS=:s WHERE ID=:i", args);
  }

  public long insertManager(Object... args) {
    return db.insert(
        "INSERT INTO MANAGER(MANAGER_ROLE_ID,USERNAME,PASSWORD_HASH,FULL_NAME,EMAIL)"
            + " VALUES(:r,:u,:p,:n,:e)",
        args);
  }

  public Map<String, Object> findManagerRole(Object... args) {
    return db.one("SELECT ROLE_CODE FROM MANAGER_ROLE WHERE ID=:i AND DELETE_FLAG=0", args);
  }

  public int lockManager(Object... args) {
    return db.update("LOCK TABLE MANAGER IN EXCLUSIVE MODE", args);
  }

  public Map<String, Object> findManager2(Object... args) {
    return db.one(
        "SELECT ID,MANAGER_ROLE_ID,STATUS FROM MANAGER WHERE ID=:i AND DELETE_FLAG=0", args);
  }

  public Map<String, Object> findManager3(Object... args) {
    return db.one(
        "SELECT COUNT(*) N FROM MANAGER m JOIN MANAGER_ROLE r ON r.ID=m.MANAGER_ROLE_ID WHERE"
            + " m.STATUS=1 AND m.DELETE_FLAG=0 AND r.ROLE_CODE='ADMIN' AND r.DELETE_FLAG=0 AND"
            + " m.ID<>:i",
        args);
  }

  public int updateManager(Object... args) {
    return db.update(
        "UPDATE MANAGER SET AUTH_VERSION=AUTH_VERSION+1,MANAGER_ROLE_ID=:r,STATUS=:s WHERE ID=:i",
        args);
  }

  public int updateUsers(Object... args) {
    return db.update(
        "UPDATE USERS SET AUTH_VERSION=AUTH_VERSION+1,STATUS=:s WHERE ID=(SELECT USER_ID FROM"
            + " CUSTOMER WHERE ID=:i AND DELETE_FLAG=0)",
        args);
  }

  public Map<String, Object> findProduct(Object... args) {
    return db.one(
        "SELECT"
            + " ID,PRODUCT_CODE,PRODUCT_NAME,BRAND_ID,MATERIAL_ID,CATEGORY_ID,TARGET_GENDER,DBMS_LOB.SUBSTR(DESCRIPTION,4000,1)"
            + " DESCRIPTION,THUMBNAIL_URL,STATUS FROM PRODUCT WHERE ID=:i AND DELETE_FLAG=0",
        args);
  }

  public List<Map<String, Object>> listProductDetail(Object... args) {
    return db.rows(
        "SELECT ID,COLOR_ID,SIZE_NAME,SKU,PRICE,QUANTITY,STATUS FROM PRODUCT_DETAIL WHERE"
            + " PRODUCT_ID=:i AND DELETE_FLAG=0 ORDER BY ID",
        args);
  }

  public List<Map<String, Object>> listProductImage(Object... args) {
    return db.rows(
        "SELECT ID,IMAGE_URL,COLOR_ID,SORT_ORDER,IS_COVER FROM PRODUCT_IMAGE WHERE PRODUCT_ID=:i"
            + " ORDER BY SORT_ORDER,ID",
        args);
  }

  public Map<String, Object> findProduct2(Object... args) {
    return db.one(
        "SELECT PRODUCT_CODE FROM PRODUCT WHERE ID=:i AND DELETE_FLAG=0 FOR UPDATE", args);
  }

  public Map<String, Object> findColor2(Object... args) {
    return db.one("SELECT COLOR_NAME FROM COLOR WHERE ID=:i AND DELETE_FLAG=0", args);
  }

  public List<Map<String, Object>> listProductImage2(Object... args) {
    return db.rows("SELECT ID FROM PRODUCT_IMAGE WHERE IMAGE_URL=:u AND PRODUCT_ID=:i", args);
  }

  public long insertProduct(Object... args) {
    return db.insert(
        "INSERT INTO"
            + " PRODUCT(PRODUCT_CODE,PRODUCT_NAME,BRAND_ID,MATERIAL_ID,CATEGORY_ID,TARGET_GENDER,DESCRIPTION,THUMBNAIL_URL,STATUS)"
            + " VALUES(:c,:n,:b,:m,:cat,:g,:d,:t,:s)",
        args);
  }

  public int updateProduct(Object... args) {
    return db.update(
        "UPDATE PRODUCT SET"
            + " PRODUCT_CODE=:c,PRODUCT_NAME=:n,BRAND_ID=:b,MATERIAL_ID=:m,CATEGORY_ID=:cat,TARGET_GENDER=:g,DESCRIPTION=:d,THUMBNAIL_URL=:t,STATUS=:s"
            + " WHERE ID=:i",
        args);
  }

  public List<Map<String, Object>> listProductDetail2(Object... args) {
    return db.rows("SELECT ID FROM PRODUCT_DETAIL WHERE PRODUCT_ID=:i AND DELETE_FLAG=0", args);
  }

  public long insertProductDetail(Object... args) {
    return db.insert(
        "INSERT INTO PRODUCT_DETAIL(PRODUCT_ID,COLOR_ID,SIZE_NAME,SKU,PRICE,QUANTITY,STATUS)"
            + " VALUES(:p,:c,:s,:sku,:price,:q,:st)",
        args);
  }

  public Map<String, Object> findProductDetail(Object... args) {
    return db.one(
        "SELECT QUANTITY,COLOR_ID,SIZE_NAME FROM PRODUCT_DETAIL WHERE ID=:i AND PRODUCT_ID=:p FOR"
            + " UPDATE",
        args);
  }

  public int updateProductDetail(Object... args) {
    return db.update(
        "UPDATE PRODUCT_DETAIL SET PRICE=:price,STATUS=:st WHERE ID=:i AND PRODUCT_ID=:p", args);
  }

  public Map<String, Object> findDual(Object... args) {
    return db.one("SELECT SHOP_PRODUCT_CODE_SEQ.NEXTVAL N FROM DUAL", args);
  }

  public List<Map<String, Object>> listProduct(Object... args) {
    return db.rows("SELECT ID FROM PRODUCT WHERE PRODUCT_CODE=:c", args);
  }

  public List<Map<String, Object>> listProductDetail3(Object... args) {
    return db.rows("SELECT ID FROM PRODUCT_DETAIL WHERE UPPER(SKU)=:s", args);
  }

  public int updateInventoryHistory(Object... args) {
    return db.update(
        "INSERT INTO INVENTORY_HISTORY(PRODUCT_DETAIL_ID,QUANTITY_CHANGE,REASON,MANAGER_ID,BILL_ID)"
            + " VALUES(:p,:q,:r,:a,:b)",
        args);
  }

  public Map<String, Object> findVoucher(Object... args) {
    return db.one("SELECT USED_COUNT FROM VOUCHER WHERE ID=:i FOR UPDATE", args);
  }

  public long insertVoucher(Object... args) {
    return db.insert(
        "INSERT INTO"
            + " VOUCHER(CODE,DISCOUNT_TYPE,DISCOUNT_VALUE,MIN_ORDER,MAX_DISCOUNT,STARTS_AT,ENDS_AT,USAGE_LIMIT,STATUS)"
            + " VALUES(:c,:t,:v,:min,:max,:start,:end,:lim,:s)",
        args);
  }

  public int updateVoucher(Object... args) {
    return db.update(
        "UPDATE VOUCHER SET"
            + " CODE=:c,DISCOUNT_TYPE=:t,DISCOUNT_VALUE=:v,MIN_ORDER=:min,MAX_DISCOUNT=:max,STARTS_AT=:start,ENDS_AT=:end,USAGE_LIMIT=:lim,STATUS=:s"
            + " WHERE ID=:i",
        args);
  }

  private static void validateLookup(String kind, String column) {
    if (!Map.of("BRAND", "BRAND_NAME", "MATERIAL", "MATERIAL_NAME")
        .getOrDefault(kind, "")
        .equals(column)) throw new IllegalArgumentException("Invalid lookup");
  }

  public Map<String, Object> lookupById(String kind, Long id) {
    validateLookup(kind, kind + "_NAME");
    return db.one("SELECT ID FROM " + kind + " WHERE ID=:i AND DELETE_FLAG=0", "i", id);
  }

  public void lockLookup(String kind) {
    validateLookup(kind, kind + "_NAME");
    db.update("LOCK TABLE " + kind + " IN EXCLUSIVE MODE");
  }

  public List<Map<String, Object>> lookupByName(String kind, String column, String name) {
    validateLookup(kind, column);
    return db.rows(
        "SELECT ID FROM "
            + kind
            + " WHERE LOWER(TRIM("
            + column
            + "))=LOWER(:n) AND DELETE_FLAG=0 ORDER BY ID",
        "n",
        name);
  }

  public long insertLookup(String kind, String column, String name) {
    validateLookup(kind, column);
    return db.insert("INSERT INTO " + kind + "(" + column + ") VALUES(:n)", "n", name);
  }
}

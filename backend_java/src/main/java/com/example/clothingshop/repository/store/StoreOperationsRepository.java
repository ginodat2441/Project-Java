package com.example.clothingshop.repository.store;

import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.util.product.CategoryVisibility;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class StoreOperationsRepository {
  private final AdminRepository db;

  public StoreOperationsRepository(AdminRepository db) {
    this.db = db;
  }

  public List<Map<String, Object>> listUsers(Object... args) {
    return db.rows(
        "SELECT c.ID,u.PASSWORD_HASH,u.AUTH_VERSION FROM USERS u JOIN CUSTOMER c ON c.USER_ID=u.ID"
            + " WHERE LOWER(u.EMAIL)=:e AND u.STATUS=1 AND u.DELETE_FLAG=0 AND c.DELETE_FLAG=0",
        args);
  }

  public Map<String, Object> findRoles(Object... args) {
    return db.one("SELECT ID FROM ROLES WHERE ROLE_CODE='CUSTOMER' AND DELETE_FLAG=0", args);
  }

  public long insertUsers(Object... args) {
    return db.insert(
        "INSERT INTO USERS(ROLE_ID,USERNAME,EMAIL,PASSWORD_HASH) VALUES(:r,:u,:e,:p)", args);
  }

  public long insertCustomer(Object... args) {
    return db.insert("INSERT INTO CUSTOMER(USER_ID,FULL_NAME,PHONE) VALUES(:u,:n,:p)", args);
  }

  public List<Map<String, Object>> listCustomer(Object... args) {
    return db.rows(
        "SELECT c.ID,u.AUTH_VERSION FROM CUSTOMER c JOIN USERS u ON u.ID=c.USER_ID WHERE c.ID=:i"
            + " AND c.DELETE_FLAG=0 AND u.DELETE_FLAG=0 AND u.STATUS=1",
        args);
  }

  public Map<String, Object> findCustomer(Object... args) {
    return db.one(
        "SELECT c.FULL_NAME,c.PHONE,u.EMAIL FROM CUSTOMER c JOIN USERS u ON u.ID=c.USER_ID WHERE"
            + " c.ID=:i",
        args);
  }

  public long insertAddress(Object... args) {
    return db.insert(
        "INSERT INTO ADDRESS(CUSTOMER_ID,RECEIVER_NAME,RECEIVER_PHONE,PROVINCE,WARD,DETAIL_ADDRESS)"
            + " VALUES(:c,:n,:p,:pr,:w,:d)",
        args);
  }

  public int updateBill(Object... args) {
    return db.update(
        "UPDATE BILL SET CARRIER='Viettel"
            + " Post',SHIPPING_SERVICE=:s,SHIPPING_SERVICE_NAME=:n,SHIPPING_QUOTED_AT=LOCALTIMESTAMP"
            + " WHERE ID=:i",
        args);
  }

  public Map<String, Object> pageProductDetail(int page, int size, Object... args) {
    return db.page(
        "SELECT p.ID,p.PRODUCT_NAME,p.THUMBNAIL_URL,c.CATEGORY_NAME,(SELECT MIN(PRICE) FROM"
            + " PRODUCT_DETAIL WHERE PRODUCT_ID=p.ID AND STATUS=1 AND DELETE_FLAG=0) PRICE FROM"
            + " PRODUCT p LEFT JOIN CATEGORY c ON c.ID=p.CATEGORY_ID WHERE p.STATUS=1 AND"
            + " p.DELETE_FLAG=0 "
            + CategoryVisibility.SQL
            + " AND LOWER(p.PRODUCT_NAME) LIKE :q AND (:c IS NULL OR p.CATEGORY_ID=:c) ORDER BY"
            + " p.ID DESC",
        page,
        size,
        args);
  }

  public List<Map<String, Object>> listCategory(Object... args) {
    return db.rows(
        "SELECT ID,CATEGORY_NAME,PARENT_ID FROM CATEGORY WHERE STATUS=1 AND ID NOT IN (SELECT ID"
            + " FROM CATEGORY START WITH STATUS=0 CONNECT BY NOCYCLE PRIOR ID=PARENT_ID) ORDER BY"
            + " CATEGORY_NAME",
        args);
  }

  public Map<String, Object> findProduct(Object... args) {
    return db.one(
        "SELECT p.ID,p.PRODUCT_NAME,p.THUMBNAIL_URL,DBMS_LOB.SUBSTR(p.DESCRIPTION,4000,1)"
            + " DESCRIPTION FROM PRODUCT p WHERE p.ID=:i AND p.STATUS=1 AND p.DELETE_FLAG=0"
            + CategoryVisibility.SQL,
        args);
  }

  public List<Map<String, Object>> listProductDetail(Object... args) {
    return db.rows(
        "SELECT pd.ID,pd.COLOR_ID,c.COLOR_NAME,pd.SIZE_NAME,pd.PRICE,pd.QUANTITY FROM"
            + " PRODUCT_DETAIL pd JOIN COLOR c ON c.ID=pd.COLOR_ID WHERE pd.PRODUCT_ID=:i AND"
            + " pd.STATUS=1 AND pd.DELETE_FLAG=0 ORDER BY c.COLOR_NAME,pd.SIZE_NAME",
        args);
  }

  public List<Map<String, Object>> listProductImage(Object... args) {
    return db.rows(
        "SELECT IMAGE_URL,COLOR_ID,SORT_ORDER FROM PRODUCT_IMAGE WHERE PRODUCT_ID=:i ORDER BY"
            + " SORT_ORDER,ID",
        args);
  }

  public Map<String, Object> findProductDetail(Object... args) {
    return db.one(
        "SELECT pd.PRICE FROM PRODUCT_DETAIL pd JOIN PRODUCT p ON p.ID=pd.PRODUCT_ID WHERE pd.ID=:i"
            + " AND pd.STATUS=1 AND pd.DELETE_FLAG=0 AND p.STATUS=1 AND p.DELETE_FLAG=0 AND"
            + " pd.QUANTITY>=:q"
            + CategoryVisibility.SQL,
        args);
  }
}

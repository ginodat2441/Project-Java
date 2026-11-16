package com.example.clothingshop.repository.product;

import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class ProductOperationsRepository {
  private final AdminRepository db;

  public ProductOperationsRepository(AdminRepository db) {
    this.db = db;
  }

  public List<Map<String, Object>> listBrand(Object... args) {
    return db.rows(
        "SELECT ID,BRAND_NAME NAME FROM BRAND WHERE DELETE_FLAG=0 ORDER BY BRAND_NAME", args);
  }

  public List<Map<String, Object>> listMaterial(Object... args) {
    return db.rows(
        "SELECT ID,MATERIAL_NAME NAME FROM MATERIAL WHERE DELETE_FLAG=0 ORDER BY MATERIAL_NAME",
        args);
  }

  public List<Map<String, Object>> listColor(Object... args) {
    return db.rows(
        "SELECT ID,COLOR_NAME NAME FROM COLOR WHERE DELETE_FLAG=0 ORDER BY COLOR_NAME", args);
  }

  public List<Map<String, Object>> listCategory(Object... args) {
    return db.rows(
        "SELECT ID,CATEGORY_NAME NAME,PARENT_ID,STATUS FROM CATEGORY ORDER BY CATEGORY_NAME", args);
  }

  public Map<String, Object> findProduct(Object... args) {
    return db.one("SELECT ID FROM PRODUCT WHERE ID=:i AND DELETE_FLAG=0 FOR UPDATE", args);
  }

  public Map<String, Object> findColor(Object... args) {
    return db.one("SELECT ID FROM COLOR WHERE ID=:i AND DELETE_FLAG=0", args);
  }

  public long insertProductImage(Object... args) {
    return db.insert(
        "INSERT INTO PRODUCT_IMAGE(PRODUCT_ID,COLOR_ID,IMAGE_URL,SORT_ORDER)"
            + " VALUES(:p,:c,:u,(SELECT NVL(MAX(SORT_ORDER),0)+1 FROM PRODUCT_IMAGE WHERE"
            + " PRODUCT_ID=:p))",
        args);
  }

  public Map<String, Object> findProduct2(Object... args) {
    return db.one("SELECT ID FROM PRODUCT WHERE ID=:p FOR UPDATE", args);
  }

  public Map<String, Object> findProductImage(Object... args) {
    return db.one("SELECT IMAGE_URL FROM PRODUCT_IMAGE WHERE ID=:i AND PRODUCT_ID=:p", args);
  }

  public int updateProductImage(Object... args) {
    return db.update("UPDATE PRODUCT_IMAGE SET IS_COVER=0 WHERE PRODUCT_ID=:p", args);
  }

  public int updateProduct(Object... args) {
    return db.update("UPDATE PRODUCT SET THUMBNAIL_URL=:u WHERE ID=:p", args);
  }

  public int updateProductImage2(Object... args) {
    return db.update(
        "UPDATE PRODUCT_IMAGE SET COLOR_ID=:c,SORT_ORDER=:o,IS_COVER=CASE WHEN :cover=1 THEN 1 ELSE"
            + " IS_COVER END WHERE ID=:i",
        args);
  }

  public Map<String, Object> findProduct3(Object... args) {
    return db.one("SELECT STATUS,THUMBNAIL_URL FROM PRODUCT WHERE ID=:p FOR UPDATE", args);
  }

  public int updateProduct2(Object... args) {
    return db.update("UPDATE PRODUCT SET THUMBNAIL_URL=NULL WHERE ID=:p", args);
  }

  public int deleteProductImage(Object... args) {
    return db.update("DELETE FROM PRODUCT_IMAGE WHERE ID=:i", args);
  }

  public Map<String, Object> findProductDetail(Object... args) {
    return db.one(
        "SELECT THUMBNAIL_URL,(SELECT COUNT(*) FROM PRODUCT_DETAIL WHERE PRODUCT_ID=:i AND STATUS=1"
            + " AND DELETE_FLAG=0) SELLING FROM PRODUCT WHERE ID=:i",
        args);
  }

  public int updateProduct3(Object... args) {
    return db.update("UPDATE PRODUCT SET STATUS=:s WHERE ID=:i", args);
  }

  public List<Map<String, Object>> listProduct(Object... args) {
    return db.rows(
        "SELECT ID,PRODUCT_CODE||' · '||PRODUCT_NAME NAME FROM PRODUCT WHERE DELETE_FLAG=0 ORDER BY"
            + " PRODUCT_CODE",
        args);
  }
}

package com.example.clothingshop.repository.store;

import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.util.product.CategoryVisibility;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerCartRepository {
  private final AdminRepository db;
  public CustomerCartRepository(AdminRepository db) { this.db = db; }

  // Serialize all cart writes for an account without reserving or changing inventory.
  public void lockCustomer(long customer) {
    db.one("SELECT ID FROM CUSTOMER WHERE ID=:c AND DELETE_FLAG=0 FOR UPDATE", "c", customer);
  }

  public Long activeCart(long customer) {
    var rows = db.rows("SELECT ID FROM CART WHERE CUSTOMER_ID=:c AND STATUS=1 AND DELETE_FLAG=0 ORDER BY ID DESC", "c", customer);
    return rows.isEmpty() ? null : AdminRepository.number(rows.get(0), "ID");
  }

  public long create(long customer) {
    return db.insert("INSERT INTO CART(CUSTOMER_ID) VALUES(:c)", "c", customer);
  }

  public List<Map<String, Object>> lines(long cart) {
    return db.rows("SELECT pd.ID PRODUCT_DETAIL_ID,p.ID PRODUCT_ID,p.PRODUCT_NAME,c.COLOR_NAME,pd.SIZE_NAME,pd.SKU,"
        + "COALESCE(pd.IMAGE_URL,p.THUMBNAIL_URL) IMAGE_URL,pd.PRICE,pd.QUANTITY STOCK,cd.QUANTITY,"
        + "CASE WHEN p.STATUS=1 AND p.DELETE_FLAG=0 AND pd.STATUS=1 AND pd.DELETE_FLAG=0"
        + CategoryVisibility.SQL + " THEN 1 ELSE 0 END AVAILABLE FROM"
        + " (SELECT PRODUCT_DETAIL_ID,SUM(QUANTITY) QUANTITY FROM CART_DETAIL WHERE CART_ID=:c AND DELETE_FLAG=0 GROUP BY PRODUCT_DETAIL_ID) cd"
        + " JOIN PRODUCT_DETAIL pd ON pd.ID=cd.PRODUCT_DETAIL_ID JOIN PRODUCT p ON p.ID=pd.PRODUCT_ID"
        + " JOIN COLOR c ON c.ID=pd.COLOR_ID ORDER BY pd.ID",
        "c", cart);
  }

  public Map<String, Object> sellableVariant(long variant) {
    return db.one("SELECT pd.ID,pd.QUANTITY FROM PRODUCT_DETAIL pd JOIN PRODUCT p ON p.ID=pd.PRODUCT_ID"
        + " WHERE pd.ID=:i AND pd.STATUS=1 AND pd.DELETE_FLAG=0 AND p.STATUS=1 AND p.DELETE_FLAG=0"
        + CategoryVisibility.SQL, "i", variant);
  }

  public List<Map<String, Object>> existing(long cart, long variant) {
    return db.rows("SELECT ID,QUANTITY FROM CART_DETAIL WHERE CART_ID=:c AND PRODUCT_DETAIL_ID=:v"
        + " AND DELETE_FLAG=0 ORDER BY ID", "c", cart, "v", variant);
  }

  public void put(long cart, long variant, int quantity) {
    var rows = existing(cart, variant);
    if (rows.isEmpty()) {
      db.insert("INSERT INTO CART_DETAIL(CART_ID,PRODUCT_DETAIL_ID,QUANTITY) VALUES(:c,:v,:q)",
          "c", cart, "v", variant, "q", quantity);
    } else {
      long first = AdminRepository.number(rows.get(0), "ID");
      db.update("UPDATE CART_DETAIL SET QUANTITY=:q WHERE ID=:i", "q", quantity, "i", first);
      db.update("UPDATE CART_DETAIL SET DELETE_FLAG=1 WHERE CART_ID=:c AND PRODUCT_DETAIL_ID=:v AND ID<>:i AND DELETE_FLAG=0",
          "c", cart, "v", variant, "i", first);
    }
  }

  public void remove(long cart, long variant) {
    db.update("UPDATE CART_DETAIL SET DELETE_FLAG=1 WHERE CART_ID=:c AND PRODUCT_DETAIL_ID=:v AND DELETE_FLAG=0", "c", cart, "v", variant);
  }
  public void clear(long cart) {
    db.update("UPDATE CART_DETAIL SET DELETE_FLAG=1 WHERE CART_ID=:c AND DELETE_FLAG=0", "c", cart);
  }
  public void finish(long cart) {
    db.update("UPDATE CART SET STATUS=0 WHERE ID=:c AND STATUS=1", "c", cart);
  }
}

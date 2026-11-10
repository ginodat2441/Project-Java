package com.example.clothingshop.repository.store;

import com.example.clothingshop.dto.store.CustomerRequests.*;
import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerAccountRepository {
  private final AdminRepository db;
  public CustomerAccountRepository(AdminRepository db) { this.db = db; }

  public Map<String, Object> user(long customer) {
    return db.one("SELECT u.ID USER_ID,u.PASSWORD_HASH,u.AUTH_VERSION FROM USERS u JOIN CUSTOMER c ON c.USER_ID=u.ID"
        + " WHERE c.ID=:c AND c.DELETE_FLAG=0 AND u.DELETE_FLAG=0 AND u.STATUS=1 FOR UPDATE OF u.PASSWORD_HASH", "c", customer);
  }
  public boolean emailUsed(String email, long user) {
    return !db.rows("SELECT ID FROM USERS WHERE LOWER(EMAIL)=:e AND ID<>:u", "e", email, "u", user).isEmpty();
  }
  public void profile(long customer, long user, Profile p, String email) {
    db.update("UPDATE CUSTOMER SET FULL_NAME=:n,PHONE=:p,GENDER=:g,BIRTH_DATE=:b WHERE ID=:c", "n", p.name().strip(), "p", p.phone().strip(), "g", p.gender(),
        "b", p.birthDate() == null ? null : java.sql.Date.valueOf(p.birthDate()), "c", customer);
    db.update("UPDATE USERS SET EMAIL=:e WHERE ID=:i", "e", email, "i", user);
  }
  public void password(long user, String hash) {
    db.update("UPDATE USERS SET PASSWORD_HASH=:h,AUTH_VERSION=AUTH_VERSION+1 WHERE ID=:i", "h", hash, "i", user);
  }
  public List<Map<String, Object>> addresses(long customer) {
    // Checkout keeps an immutable address snapshot for each bill. Those rows are not
    // address-book entries; editing the book must never edit an existing order.
    return db.rows("SELECT a.ID,a.RECEIVER_NAME,a.RECEIVER_PHONE,a.PROVINCE,a.WARD,a.DETAIL_ADDRESS,a.IS_DEFAULT FROM ADDRESS a"
        + " WHERE a.CUSTOMER_ID=:c AND a.DELETE_FLAG=0 AND NOT EXISTS (SELECT 1 FROM BILL b WHERE b.ADDRESS_ID=a.ID)"
        + " ORDER BY a.IS_DEFAULT DESC,a.ID DESC", "c", customer);
  }
  public Map<String, Object> ownedAddress(long customer, long id) {
    return db.one("SELECT a.ID,a.IS_DEFAULT FROM ADDRESS a WHERE a.ID=:i AND a.CUSTOMER_ID=:c AND a.DELETE_FLAG=0"
        + " AND NOT EXISTS (SELECT 1 FROM BILL b WHERE b.ADDRESS_ID=a.ID)", "i", id, "c", customer);
  }
  public long address(long customer, Long id, Address a, boolean defaultAddress) {
    Object[] args = {"c", customer, "n", a.name().strip(), "p", a.phone().strip(), "pr", a.province().strip(),
        "w", a.ward().strip(), "d", a.address().strip(), "def", defaultAddress ? 1 : 0, "i", id};
    if (defaultAddress) db.update("UPDATE ADDRESS SET IS_DEFAULT=0 WHERE CUSTOMER_ID=:c AND DELETE_FLAG=0", "c", customer);
    if (id == null) return db.insert("INSERT INTO ADDRESS(CUSTOMER_ID,RECEIVER_NAME,RECEIVER_PHONE,PROVINCE,WARD,DETAIL_ADDRESS,IS_DEFAULT)"
        + " VALUES(:c,:n,:p,:pr,:w,:d,:def)", args);
    db.update("UPDATE ADDRESS SET RECEIVER_NAME=:n,RECEIVER_PHONE=:p,PROVINCE=:pr,WARD=:w,DETAIL_ADDRESS=:d,IS_DEFAULT=:def"
        + " WHERE ID=:i AND CUSTOMER_ID=:c AND DELETE_FLAG=0", args);
    return id;
  }
  public void removeAddress(long customer, long id) {
    db.update("UPDATE ADDRESS SET DELETE_FLAG=1,IS_DEFAULT=0 WHERE ID=:i AND CUSTOMER_ID=:c AND DELETE_FLAG=0", "c", customer, "i", id);
  }
  public void defaultAddress(long customer, long id) {
    db.update("UPDATE ADDRESS SET IS_DEFAULT=CASE WHEN ID=:i THEN 1 ELSE 0 END WHERE CUSTOMER_ID=:c AND DELETE_FLAG=0", "c", customer, "i", id);
  }
  public void ownedOrder(long customer, long id) {
    db.one("SELECT ID FROM BILL WHERE ID=:i AND CUSTOMER_ID=:c AND DELETE_FLAG=0", "i", id, "c", customer);
  }
}

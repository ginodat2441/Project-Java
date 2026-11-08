package com.example.clothingshop.repository.account;

import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class AccountOperationsRepository {
  private final AdminRepository db;

  public AccountOperationsRepository(AdminRepository db) {
    this.db = db;
  }

  public List<Map<String, Object>> listManagerRole(Object... args) {
    return db.rows(
        "SELECT ID,ROLE_CODE,ROLE_NAME FROM MANAGER_ROLE WHERE DELETE_FLAG=0 AND ROLE_CODE IN"
            + " ('ADMIN','STAFF')",
        args);
  }

  public Map<String, Object> pageCustomer(int page, int size, Object... args) {
    return db.page(
        "SELECT c.ID,c.FULL_NAME,c.PHONE,u.EMAIL,u.STATUS FROM CUSTOMER c JOIN USERS u ON"
            + " u.ID=c.USER_ID WHERE c.DELETE_FLAG=0 AND (LOWER(c.FULL_NAME) LIKE :q OR"
            + " LOWER(u.EMAIL) LIKE :q OR c.PHONE LIKE :q) ORDER BY c.ID DESC",
        page,
        size,
        args);
  }

  public Map<String, Object> findCustomer(Object... args) {
    return db.one(
        "SELECT c.ID,c.FULL_NAME,c.PHONE,u.EMAIL,u.STATUS FROM CUSTOMER c JOIN USERS u ON"
            + " u.ID=c.USER_ID WHERE c.ID=:i",
        args);
  }

  public List<Map<String, Object>> listBill(Object... args) {
    return db.rows(
        "SELECT ID,BILL_CODE,TOTAL_AMOUNT,STATUS,CREATED_AT FROM BILL WHERE CUSTOMER_ID=:i AND"
            + " DELETE_FLAG=0 ORDER BY ID DESC",
        args);
  }
}

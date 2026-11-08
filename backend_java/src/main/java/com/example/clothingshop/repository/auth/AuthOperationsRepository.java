package com.example.clothingshop.repository.auth;

import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class AuthOperationsRepository {
  private final AdminRepository db;

  public AuthOperationsRepository(AdminRepository db) {
    this.db = db;
  }

  public List<Map<String, Object>> listManager(Object... args) {
    return db.rows(
        "SELECT m.ID,m.EMAIL,m.AUTH_VERSION FROM MANAGER m JOIN MANAGER_ROLE r ON"
            + " r.ID=m.MANAGER_ROLE_ID WHERE (LOWER(m.USERNAME)=LOWER(:u) OR"
            + " LOWER(m.EMAIL)=LOWER(:u)) AND m.STATUS=1 AND m.DELETE_FLAG=0 AND r.DELETE_FLAG=0"
            + " AND r.ROLE_CODE IN ('ADMIN','STAFF') ORDER BY m.ID",
        args);
  }

  public Map<String, Object> findManager(Object... args) {
    return db.one("SELECT AUTH_VERSION,STATUS FROM MANAGER WHERE ID=:i FOR UPDATE", args);
  }

  public int updateManagerPasswordReset(Object... args) {
    return db.update(
        "UPDATE MANAGER_PASSWORD_RESET SET USED_AT=LOCALTIMESTAMP WHERE MANAGER_ID=:i AND USED_AT"
            + " IS NULL",
        args);
  }

  public long insertManagerPasswordReset(Object... args) {
    return db.insert(
        "INSERT INTO MANAGER_PASSWORD_RESET(MANAGER_ID,TOKEN_HASH,AUTH_VERSION,EXPIRES_AT)"
            + " VALUES(:i,:t,:v,LOCALTIMESTAMP+INTERVAL '15' MINUTE)",
        args);
  }

  public List<Map<String, Object>> listManagerPasswordReset(Object... args) {
    return db.rows("SELECT ID,MANAGER_ID FROM MANAGER_PASSWORD_RESET WHERE TOKEN_HASH=:t", args);
  }

  public Map<String, Object> findManager2(Object... args) {
    return db.one(
        "SELECT AUTH_VERSION,STATUS,DELETE_FLAG FROM MANAGER WHERE ID=:i FOR UPDATE", args);
  }

  public List<Map<String, Object>> listManagerPasswordReset2(Object... args) {
    return db.rows(
        "SELECT ID,AUTH_VERSION FROM MANAGER_PASSWORD_RESET WHERE TOKEN_HASH=:t AND USED_AT IS NULL"
            + " AND EXPIRES_AT>LOCALTIMESTAMP FOR UPDATE",
        args);
  }

  public int updateManager(Object... args) {
    return db.update(
        "UPDATE MANAGER SET PASSWORD_HASH=:p,AUTH_VERSION=AUTH_VERSION+1 WHERE ID=:i", args);
  }
}

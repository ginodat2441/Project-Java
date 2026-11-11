package com.example.clothingshop.repositories;

import com.example.clothingshop.common.SqlHelper;
import com.example.clothingshop.entity.UserAccount;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {

    private final SqlHelper sql;

    public UserRepository(SqlHelper sql) {
        this.sql = sql;
    }

    public Optional<UserAccount> findByEmail(String email) {
        return sql.one("""
                SELECT u.ID AS USER_ID, c.ID AS CUSTOMER_ID, u.USERNAME, u.EMAIL,
                       u.PASSWORD_HASH, u.STATUS, c.FULL_NAME, c.PHONE
                  FROM USERS u
                  JOIN CUSTOMER c ON c.USER_ID = u.ID AND c.DELETE_FLAG = 0
                 WHERE LOWER(u.EMAIL) = LOWER(:email)
                   AND u.DELETE_FLAG = 0
                """, Map.of("email", email), UserAccount.class);
    }

    public Optional<UserAccount> findByCustomerId(long customerId) {
        return sql.one("""
                SELECT u.ID AS USER_ID, c.ID AS CUSTOMER_ID, u.USERNAME, u.EMAIL,
                       u.PASSWORD_HASH, u.STATUS, c.FULL_NAME, c.PHONE
                  FROM USERS u
                  JOIN CUSTOMER c ON c.USER_ID = u.ID AND c.DELETE_FLAG = 0
                 WHERE c.ID = :customerId AND u.DELETE_FLAG = 0
                """, Map.of("customerId", customerId), UserAccount.class);
    }

    public boolean emailExists(String email) {
        return sql.scalar("""
                SELECT COUNT(*) FROM USERS
                 WHERE LOWER(EMAIL) = LOWER(:email)
                """, Map.of("email", email), Long.class).orElse(0L) > 0;
    }

    public boolean usernameExists(String username) {
        return sql.scalar("""
                SELECT COUNT(*) FROM USERS
                 WHERE LOWER(USERNAME) = LOWER(:username)
                """, Map.of("username", username), Long.class).orElse(0L) > 0;
    }

    public long customerRoleId() {
        return sql.scalar("""
                SELECT ID FROM ROLES
                 WHERE ROLE_CODE = 'CUSTOMER' AND DELETE_FLAG = 0
                """, Map.of(), Long.class)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role CUSTOMER trong bảng ROLES"));
    }

    public long insertUser(long roleId, String username, String email, String passwordHash) {
        return sql.insert("""
                INSERT INTO USERS (ROLE_ID, USERNAME, EMAIL, PASSWORD_HASH)
                VALUES (:roleId, :username, :email, :passwordHash)
                """, Map.of("roleId", roleId, "username", username, "email", email,
                "passwordHash", passwordHash));
    }

    public long insertCustomer(long userId, String fullName, String phone) {
        Map<String, Object> values = new java.util.HashMap<>();
        values.put("userId", userId);
        values.put("fullName", fullName);
        values.put("phone", phone);
        return sql.insert("""
                INSERT INTO CUSTOMER (USER_ID, FULL_NAME, PHONE)
                VALUES (:userId, :fullName, :phone)
                """, values);
    }
}

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
        Optional<Long> existingRoleId = sql.scalar("""
                SELECT ID FROM ROLES
                 WHERE UPPER(TRIM(ROLE_CODE)) = 'CUSTOMER'
                   AND DELETE_FLAG = 0
                """, Map.of(), Long.class);
        if (existingRoleId.isPresent()) {
            return existingRoleId.get();
        }

        sql.update("""
                MERGE INTO ROLES target
                USING (SELECT 'CUSTOMER' AS ROLE_CODE FROM DUAL) source
                  ON (UPPER(TRIM(target.ROLE_CODE)) = source.ROLE_CODE)
                WHEN MATCHED THEN
                     UPDATE SET target.ROLE_NAME = N'Khách hàng',
                                target.DESCRIPTION = N'Người dùng mua hàng trên website',
                                target.DELETE_FLAG = 0
                 WHEN NOT MATCHED THEN
                      INSERT (ROLE_CODE, ROLE_NAME, DESCRIPTION, DELETE_FLAG)
                      VALUES ('CUSTOMER', N'Khách hàng',
                              N'Người dùng mua hàng trên website', 0)
                """, Map.of());

        Optional<Long> customerRoleId = sql.scalar("""
                SELECT ID FROM ROLES
                 WHERE UPPER(TRIM(ROLE_CODE)) = 'CUSTOMER'
                   AND DELETE_FLAG = 0
                """, Map.of(), Long.class);
        if (customerRoleId.isPresent()) {
            return customerRoleId.get();
        }

        DatabaseContext context = sql.one("""
                SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') AS DB_USER,
                       SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS CURRENT_SCHEMA,
                       SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS SERVICE_NAME,
                       SYS_CONTEXT('USERENV', 'CON_NAME') AS CONTAINER_NAME
                  FROM DUAL
                """, Map.of(), DatabaseContext.class)
                .orElseThrow(() -> new IllegalStateException("Không đọc được thông tin kết nối Oracle"));
        throw new IllegalStateException(
                "Không tạo hoặc kích hoạt được role CUSTOMER. Oracle connection: dbUser="
                        + context.dbUser() + ", schema=" + context.currentSchema()
                        + ", service=" + context.serviceName()
                        + ", container=" + context.containerName());
    }

    public record DatabaseContext(String dbUser, String currentSchema,
                                  String serviceName, String containerName) {}

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

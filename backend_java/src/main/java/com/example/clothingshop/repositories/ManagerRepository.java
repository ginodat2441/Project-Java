package com.example.clothingshop.repositories;

import com.example.clothingshop.common.SqlHelper;
import com.example.clothingshop.entity.ManagerAccount;
import com.example.clothingshop.view.ManagerView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ManagerRepository {

    private static final Logger log = LoggerFactory.getLogger(ManagerRepository.class);

    private final SqlHelper sql;

    public ManagerRepository(SqlHelper sql) {
        this.sql = sql;
    }

    public Optional<ManagerAccount> findActiveByEmail(String email) {
        return sql.one("""
                SELECT m.ID, m.USERNAME, m.EMAIL, m.PASSWORD_HASH,
                       m.STATUS, m.FULL_NAME, mr.ROLE_CODE
                  FROM MANAGER m
                  JOIN MANAGER_ROLE mr ON mr.ID = m.MANAGER_ROLE_ID
                                       AND mr.DELETE_FLAG = 0
                 WHERE LOWER(m.EMAIL) = LOWER(:email)
                   AND m.STATUS = 1
                   AND m.DELETE_FLAG = 0
                   AND mr.ROLE_CODE IN ('ADMIN', 'STAFF')
                """, Map.of("email", email), ManagerAccount.class);
    }

    public long lockAdminRole() {
        return sql.scalar("""
                SELECT ID FROM MANAGER_ROLE
                 WHERE ROLE_CODE = 'ADMIN' AND DELETE_FLAG = 0
                 FOR UPDATE
                """, Map.of(), Long.class)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role ADMIN trong MANAGER_ROLE"));
    }

    public long roleId(String roleCode) {
        return sql.scalar("""
                SELECT ID FROM MANAGER_ROLE
                 WHERE ROLE_CODE = :roleCode AND DELETE_FLAG = 0
                """, Map.of("roleCode", roleCode), Long.class)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role " + roleCode + " trong MANAGER_ROLE"));
    }

    public long countOwners() {
        OwnerCountSnapshot snapshot = sql.one("""
                SELECT
                    (SELECT COUNT(*)
                       FROM MANAGER m
                       JOIN MANAGER_ROLE mr ON mr.ID = m.MANAGER_ROLE_ID
                      WHERE mr.ROLE_CODE = 'ADMIN'
                        AND mr.DELETE_FLAG = 0
                        AND m.DELETE_FLAG = 0) AS OWNER_COUNT,
                    SYS_CONTEXT('USERENV', 'SESSION_USER') AS DB_USER,
                    SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS CURRENT_SCHEMA,
                    SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS SERVICE_NAME,
                    SYS_CONTEXT('USERENV', 'CON_NAME') AS CONTAINER_NAME
                  FROM DUAL
                """, Map.of(), OwnerCountSnapshot.class)
                .orElseThrow(() -> new IllegalStateException("Không đọc được trạng thái tài khoản chủ cửa hàng"));
        log.info("Bootstrap owner lookup: dbUser={}, schema={}, service={}, container={}, ownerCount={}",
                snapshot.dbUser(), snapshot.currentSchema(), snapshot.serviceName(),
                snapshot.containerName(), snapshot.ownerCount());
        return snapshot.ownerCount();
    }

    public record OwnerCountSnapshot(Long ownerCount, String dbUser, String currentSchema,
                                     String serviceName, String containerName) {}

    public ManagerAccountLookup lookupAccount(String username, String email) {
        ManagerAccountLookup lookup = sql.one("""
                SELECT
                    (SELECT COUNT(*) FROM MANAGER
                      WHERE LOWER(USERNAME) = LOWER(:username)) AS USERNAME_COUNT,
                    (SELECT COUNT(*) FROM MANAGER
                      WHERE LOWER(EMAIL) = LOWER(:email)) AS EMAIL_COUNT,
                    SYS_CONTEXT('USERENV', 'SESSION_USER') AS DB_USER,
                    SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS CURRENT_SCHEMA,
                    SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS SERVICE_NAME,
                    SYS_CONTEXT('USERENV', 'CON_NAME') AS CONTAINER_NAME
                  FROM DUAL
                """, Map.of("username", username, "email", email), ManagerAccountLookup.class)
                .orElseThrow(() -> new IllegalStateException("Không kiểm tra được tài khoản quản lý"));
        log.info("Manager account lookup: dbUser={}, schema={}, service={}, container={}, usernameMatches={}, emailMatches={}",
                lookup.dbUser(), lookup.currentSchema(), lookup.serviceName(), lookup.containerName(),
                lookup.usernameCount(), lookup.emailCount());
        return lookup;
    }

    public record ManagerAccountLookup(Long usernameCount, Long emailCount, String dbUser,
                                       String currentSchema, String serviceName, String containerName) {}

    public long insert(long roleId, String username, String email, String passwordHash,
                       String fullName, String phone) {
        Map<String, Object> values = new HashMap<>();
        values.put("roleId", roleId);
        values.put("username", username);
        values.put("email", email);
        values.put("passwordHash", passwordHash);
        values.put("fullName", fullName);
        values.put("phone", phone);
        return sql.insert("""
                INSERT INTO MANAGER (MANAGER_ROLE_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE)
                VALUES (:roleId, :username, :passwordHash, :fullName, :email, :phone)
                """, values);
    }

    public List<ManagerView> findAll() {
        return sql.list("""
                SELECT m.ID, m.USERNAME, m.EMAIL, m.FULL_NAME, m.PHONE,
                       mr.ROLE_CODE, m.STATUS, m.CREATED_AT
                  FROM MANAGER m
                  JOIN MANAGER_ROLE mr ON mr.ID = m.MANAGER_ROLE_ID
                 WHERE m.DELETE_FLAG = 0 AND mr.DELETE_FLAG = 0
                 ORDER BY m.CREATED_AT DESC
                """, Map.of(), ManagerView.class);
    }

    public Optional<ManagerView> findViewById(long id) {
        return sql.one("""
                SELECT m.ID, m.USERNAME, m.EMAIL, m.FULL_NAME, m.PHONE,
                       mr.ROLE_CODE, m.STATUS, m.CREATED_AT
                  FROM MANAGER m
                  JOIN MANAGER_ROLE mr ON mr.ID = m.MANAGER_ROLE_ID
                 WHERE m.ID = :id AND m.DELETE_FLAG = 0 AND mr.DELETE_FLAG = 0
                """, Map.of("id", id), ManagerView.class);
    }
}

package com.example.clothingshop.repositories;

import com.example.clothingshop.common.SqlHelper;
import com.example.clothingshop.entity.Address;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AddressRepository {

    private static final String SELECT_ADDRESS = """
            SELECT ID, CUSTOMER_ID, RECEIVER_NAME, RECEIVER_PHONE, PROVINCE,
                   WARD, DETAIL_ADDRESS, IS_DEFAULT
              FROM ADDRESS
             WHERE DELETE_FLAG = 0
            """;

    private final SqlHelper sql;

    public AddressRepository(SqlHelper sql) {
        this.sql = sql;
    }

    public List<Address> findByCustomer(long customerId) {
        return sql.list(SELECT_ADDRESS + " AND CUSTOMER_ID = :customerId ORDER BY IS_DEFAULT DESC, ID DESC",
                Map.of("customerId", customerId), Address.class);
    }

    public Optional<Address> findById(long addressId, long customerId) {
        return sql.one(SELECT_ADDRESS + " AND ID = :id AND CUSTOMER_ID = :customerId",
                Map.of("id", addressId, "customerId", customerId), Address.class);
    }

    public boolean hasAddress(long customerId) {
        return sql.scalar("""
                SELECT COUNT(*) FROM ADDRESS
                 WHERE CUSTOMER_ID = :customerId AND DELETE_FLAG = 0
                """, Map.of("customerId", customerId), Long.class).orElse(0L) > 0;
    }

    public long insert(long customerId, Map<String, Object> values) {
        values.put("customerId", customerId);
        return sql.insert("""
                INSERT INTO ADDRESS
                    (CUSTOMER_ID, RECEIVER_NAME, RECEIVER_PHONE, PROVINCE, WARD, DETAIL_ADDRESS, IS_DEFAULT)
                VALUES
                    (:customerId, :receiverName, :receiverPhone, :province, :ward, :detailAddress, :isDefault)
                """, values);
    }

    public int update(long addressId, long customerId, Map<String, Object> values) {
        values.put("id", addressId);
        values.put("customerId", customerId);
        return sql.update("""
                UPDATE ADDRESS
                   SET RECEIVER_NAME = :receiverName, RECEIVER_PHONE = :receiverPhone,
                       PROVINCE = :province, WARD = :ward, DETAIL_ADDRESS = :detailAddress,
                       IS_DEFAULT = :isDefault
                 WHERE ID = :id AND CUSTOMER_ID = :customerId AND DELETE_FLAG = 0
                """, values);
    }

    public void clearDefault(long customerId) {
        sql.update("UPDATE ADDRESS SET IS_DEFAULT = 0 WHERE CUSTOMER_ID = :customerId AND DELETE_FLAG = 0",
                Map.of("customerId", customerId));
    }

    public void softDelete(long addressId, long customerId) {
        sql.update("""
                UPDATE ADDRESS SET DELETE_FLAG = 1
                 WHERE ID = :id AND CUSTOMER_ID = :customerId AND DELETE_FLAG = 0
                """, Map.of("id", addressId, "customerId", customerId));
    }
}

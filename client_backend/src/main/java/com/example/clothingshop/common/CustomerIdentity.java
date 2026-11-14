package com.example.clothingshop.common;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public final class CustomerIdentity {

    private CustomerIdentity() {}

    public static long customerId(Jwt jwt) {
        Object claim = jwt.getClaims().get("customerId");
        if (!(claim instanceof Number customerId)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Token không có thông tin khách hàng");
        }
        return customerId.longValue();
    }

    public static long requireCustomer(Jwt jwt, long requestedCustomerId) {
        long authenticatedCustomerId = customerId(jwt);
        if (authenticatedCustomerId != requestedCustomerId) {
            throw new ResponseStatusException(FORBIDDEN, "Không có quyền truy cập dữ liệu khách hàng khác");
        }
        return authenticatedCustomerId;
    }
}

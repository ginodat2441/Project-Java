package com.example.clothingshop;

import static org.junit.jupiter.api.Assertions.*;
import com.example.clothingshop.common.*;
import com.example.clothingshop.dto.request.*;
import com.example.clothingshop.service.AuthenticationService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named="RUN_REGISTRATION_ORACLE_TEST",matches="true")
class RegistrationOracleTest {
    @Autowired AuthenticationService auth;
    @Autowired SqlHelper sql;
    @Autowired PasswordEncoder encoder;
    @Test void registrationAndLoginUseOracleWithHashedPasswordAndCustomerRole() {
        String suffix=UUID.randomUUID().toString().replace("-","").substring(0,12);
        String email="authcheck."+suffix+"@example.com",password="PrivateTest!2026"+suffix;
        var result=auth.register(new RegisterRequest("auth_"+suffix,email,password,"Khách Kiểm Thử","+84 912 345 678"));
        var user=result.user();var params=Map.<String,Object>of("id",user.userId());
        String hash=sql.scalar("SELECT PASSWORD_HASH FROM USERS WHERE ID=:id",params,String.class).orElseThrow();
        assertNotEquals(password,hash);assertTrue(encoder.matches(password,hash));
        assertEquals("0912345678",sql.scalar("SELECT PHONE FROM CUSTOMER WHERE USER_ID=:id",params,String.class).orElseThrow());
        assertEquals("CUSTOMER",sql.scalar("SELECT r.ROLE_CODE FROM USERS u JOIN ROLES r ON r.ID=u.ROLE_ID WHERE u.ID=:id",params,String.class).orElseThrow());
        assertEquals(user.customerId(),auth.login(new LoginRequest(email.toUpperCase(Locale.ROOT),password)).user().customerId());
        assertThrows(AccountInputPolicy.Invalid.class,()->auth.register(new RegisterRequest("another_"+suffix,email,password,"Khách Kiểm Thử","0912345678")));
    }
}

package com.example.clothingshop.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.example.clothingshop.common.*;
import com.example.clothingshop.controller.AuthenticationController;
import com.example.clothingshop.dto.request.*;
import com.example.clothingshop.entity.UserAccount;
import com.example.clothingshop.repositories.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthenticationValidationTest {
    final UserRepository users=mock(UserRepository.class);
    final JwtEncoder jwt=mock(JwtEncoder.class);
    final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(4);
    final AuthenticationService service=new AuthenticationService(users,passwords,jwt,mock(ManagerRepository.class));
    RegisterRequest valid(){return new RegisterRequest("shop_user","test@example.com","A safe password 2026!","Nguyễn Văn An","+84 912 345 678");}
    @Test void normalizesWithoutChangingPasswordAndRejectsGarbageBeforeDatabase() {
        var good=valid();assertEquals("0912345678",good.phone());assertDoesNotThrow(()->AccountInputPolicy.register(good));
        var error=assertThrows(AccountInputPolicy.Invalid.class,()->service.register(new RegisterRequest("!!!","a@b","12345678","123456","++++----")));
        assertTrue(error.fields().keySet().containsAll(Set.of("username","email","password","fullName","phone")));
        verifyNoInteractions(users);
    }
    @Test void rejectsCommonPasswordsAndBcryptOverflow() {
        for(String password:List.of("password1234","123456789012","aaaaaaaaaaaa","a1"+"á".repeat(40)," LeadingPass123"))
            assertThrows(AccountInputPolicy.Invalid.class,()->AccountInputPolicy.register(new RegisterRequest("valid_user","test@example.com",password,"Nguyễn An","0912345678")));
    }
    @Test void duplicateEmailAndUsernameCannotCreateUser() {
        when(users.emailExists("test@example.com")).thenReturn(true);
        assertThrows(AccountInputPolicy.Invalid.class,()->service.register(valid()));
        when(users.emailExists("test@example.com")).thenReturn(false);when(users.usernameExists("shop_user")).thenReturn(true);
        assertThrows(AccountInputPolicy.Invalid.class,()->service.register(valid()));
        verify(users,never()).insertUser(anyLong(),anyString(),anyString(),anyString());
    }
    @Test void wrongPasswordAndLockedCustomerCannotLogin() {
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(new UserAccount(1L,2L,"test","test@example.com",passwords.encode("OldPass1"),1,"Nguyễn An","0912345678")));
        assertThrows(ResponseStatusException.class,()->service.login(new LoginRequest("test@example.com","wrong")));
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(new UserAccount(1L,2L,"test","test@example.com",passwords.encode("OldPass1"),0,"Nguyễn An","0912345678")));
        assertThrows(ResponseStatusException.class,()->service.login(new LoginRequest("test@example.com","OldPass1")));
        verifyNoInteractions(jwt);
    }
    @Test void existingShortPasswordStillWorksForLogin() {
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(new UserAccount(1L,2L,"test","test@example.com",passwords.encode("OldPass1"),1,"Nguyễn An","0912345678")));
        when(jwt.encode(any())).thenReturn(Jwt.withTokenValue("fixture").header("alg","HS256").claim("sub","test@example.com").build());
        assertEquals(2L,service.login(new LoginRequest(" TEST@EXAMPLE.COM ","OldPass1")).user().customerId());
    }
    @Test void directHttpRequestCannotBypassFrontendValidation() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new AuthenticationController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(Map.of("username","valid_user","email","test@example.com","password","short123","fullName","Nguyễn An","phone","0912345678"))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.password").exists());
        mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(Map.of("username","valid_user","email","test@example.com","password","ValidPassword2026!","fullName","123456","phone","++++----"))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.phone").exists()).andExpect(jsonPath("$.fieldErrors.fullName").exists());
        verifyNoInteractions(users);
    }
}

package com.example.clothingshop.service.store;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.dto.store.StoreRequests.*;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.store.StoreOperationsRepository;
import com.example.clothingshop.service.order.BillService;
import com.example.clothingshop.service.shipping.ShippingQuoteService;
import jakarta.servlet.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class CustomerAuthTest {
  final StoreOperationsRepository queries = mock(StoreOperationsRepository.class);
  final PasswordEncoder encoder = mock(PasswordEncoder.class);
  final HttpSession session = mock(HttpSession.class);
  final HttpServletRequest request = mock(HttpServletRequest.class);
  final StoreService service = new StoreService(mock(AdminRepository.class), encoder,
      mock(BillService.class), mock(ShippingQuoteService.class), queries);

  @Test void registrationStoresHashAndProfileWithoutSigningIn() {
    when(queries.findRoles()).thenReturn(Map.of("ID", 2L));
    when(encoder.encode("Password@12345")).thenReturn("bcrypt-hash");
    when(queries.insertUsers(any(Object[].class))).thenReturn(14L);
    assertEquals(Map.of("success", true), service.register(
        new Register("TEST@example.test", "Password@12345", "Đào Khải", "0901234567"), session, request));
    verify(queries).emailExists("test@example.test");
    verify(queries).insertUsers(eq("r"), eq(2L), eq("u"), startsWith("customer-"),
        eq("e"), eq("test@example.test"), eq("p"), eq("bcrypt-hash"));
    verify(queries).insertCustomer("u", 14L, "n", "Đào Khải", "p", "0901234567");
    verify(session, never()).setAttribute(anyString(), any());
  }

  @Test void duplicateEmailDoesNotInsertAnotherCustomer() {
    when(queries.emailExists("used@example.test")).thenReturn(true);
    assertThrows(BusinessException.class, () -> service.register(
        new Register("used@example.test", "Password@12345", "Khải", "0901234567"), session, request));
    verify(queries, never()).insertUsers(any(Object[].class));
  }

  @Test void unicodePasswordOverBcryptByteLimitIsRejected() {
    assertThrows(BusinessException.class, () -> service.register(
        new Register("test@example.test", "đ".repeat(40), "Khải", "0901234567"), session, request));
    verifyNoInteractions(queries);
  }

  @Test void loginChecksHashAndSetsCustomerSession() {
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(queries.listUsers("e", "test@example.test")).thenReturn(
        List.of(Map.of("ID", 14L, "PASSWORD_HASH", "bcrypt-hash", "AUTH_VERSION", 0L)));
    when(encoder.matches("Password@12345", "bcrypt-hash")).thenReturn(true);
    service.login(new Login("TEST@example.test", "Password@12345"), session, request);
    verify(request).changeSessionId();
    verify(session).setAttribute("customer", 14L);
    verify(session).setAttribute("customer-access-version", 0L);
  }

  @Test void wrongPasswordDoesNotAuthenticate() {
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(queries.listUsers("e", "test@example.test")).thenReturn(List.of());
    assertThrows(BusinessException.class, () -> service.login(
        new Login("test@example.test", "wrong"), session, request));
    verify(session, never()).setAttribute(anyString(), any());
  }
}

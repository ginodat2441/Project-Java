package com.example.clothingshop.controller.store;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.clothingshop.dto.order.BillRequest;
import com.example.clothingshop.dto.order.BillView;
import com.example.clothingshop.dto.store.StoreRequests;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.service.order.BillService;
import com.example.clothingshop.service.shipping.ShippingQuoteService;
import com.example.clothingshop.service.store.StoreService;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

class ShippingCheckoutTest {
  AdminRepository db;
  BillService bills;
  ShippingQuoteService shipping;
  StoreService store;
  MockHttpSession session;
  List<BillRequest.Item> items = List.of(new BillRequest.Item(1L, 2));

  @BeforeEach
  void setup() {
    db = mock(AdminRepository.class);
    bills = mock(BillService.class);
    shipping = mock(ShippingQuoteService.class);
    store =
        new StoreService(
            db,
            mock(PasswordEncoder.class),
            bills,
            shipping,
            new com.example.clothingshop.repository.store.StoreOperationsRepository(db));
    session = new MockHttpSession();
    session.setAttribute("customer", 9L);
    session.setAttribute("customer-access-version", 0L);
    when(db.rows(anyString(), any(Object[].class)))
        .thenReturn(List.of(Map.of("ID", 9L, "AUTH_VERSION", 0L)));
    when(db.one(anyString(), any(Object[].class)))
        .thenReturn(Map.of("PRICE", new BigDecimal("200000")));
    when(bills.discount(any(), any(), eq(false))).thenReturn(BigDecimal.ZERO);
    when(shipping.quote(
            anyString(), anyString(), anyString(), anyString(), anyList(), any(), any()))
        .thenReturn(
            List.of(
                new ShippingQuoteService.Offer("VCN", "Nhanh", new BigDecimal("36000"), "2 ngày")));
  }

  StoreRequests.Checkout request(String code, int fee) {
    return new StoreRequests.Checkout(
        "Demo",
        "0900000000",
        "Hà Nội",
        "Cầu Giấy",
        "12 Demo",
        "COD",
        null,
        null,
        items,
        code,
        BigDecimal.valueOf(fee));
  }

  @Test
  void priceChangeIsRejectedBeforeAddressOrOrderWrites() {
    assertThrows(RuntimeException.class, () -> store.checkout(request("VCN", 1000), session));
    verify(db, never()).insert(anyString(), any(Object[].class));
    verify(bills, never()).createCarrier(any(), any(), any(), any());
  }

  @Test
  void unavailableServiceCannotBeForgedByBrowser() {
    assertThrows(RuntimeException.class, () -> store.checkout(request("FAKE", 36000), session));
    verify(db, never()).insert(anyString(), any(Object[].class));
  }

  @Test
  void validQuoteIsRecheckedAndSavedAsCarrierSnapshot() {
    when(db.insert(anyString(), any(Object[].class))).thenReturn(10L);
    var order =
        new BillView(
            12L,
            "HD12",
            9L,
            "Demo",
            "0900000000",
            "12 Demo",
            new BigDecimal("400000"),
            new BigDecimal("36000"),
            BigDecimal.ZERO,
            new BigDecimal("436000"),
            "COD",
            0,
            0,
            java.time.LocalDateTime.now());
    when(bills.createCarrier(any(), any(), any(), any())).thenReturn(order);
    assertEquals(order, store.checkout(request("VCN", 36000), session));
    verify(shipping)
        .quote(
            eq("Hà Nội"),
            eq("Cầu Giấy"),
            eq("12 Demo"),
            eq("COD"),
            eq(items),
            eq(new BigDecimal("400000")),
            eq(BigDecimal.ZERO));
    verify(bills)
        .createCarrier(
            any(), eq(new BigDecimal("36000")), eq(new BigDecimal("400000")), eq(BigDecimal.ZERO));
    verify(db).update(contains("SHIPPING_SERVICE"), any(Object[].class));
  }
}

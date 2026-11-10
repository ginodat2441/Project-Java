package com.example.clothingshop.service.store;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.dto.order.BillRequest;
import com.example.clothingshop.dto.store.StoreRequests.Checkout;
import com.example.clothingshop.repository.store.CustomerCartRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpSession;

class CustomerCartServiceTest {
  final CustomerCartRepository repo = mock(CustomerCartRepository.class);
  final StoreService store = mock(StoreService.class);
  final CustomerCartService service = new CustomerCartService(repo, store);
  final MockHttpSession session = new MockHttpSession();

  @BeforeEach void setup() {
    when(store.customer(session)).thenReturn(9L);
    when(repo.activeCart(9L)).thenReturn(41L);
    when(repo.lines(41L)).thenReturn(List.of(line(2, 5, 1)));
  }

  Map<String,Object> line(int quantity, int stock, int available) {
    Map<String,Object> row = new HashMap<>();
    row.put("PRODUCT_DETAIL_ID", 12L); row.put("PRODUCT_ID", 4L);
    row.put("PRODUCT_NAME", "Áo thun"); row.put("COLOR_NAME", "Trắng");
    row.put("SIZE_NAME", "M"); row.put("SKU", "AO-TRANG-M");
    row.put("PRICE", new BigDecimal("199000")); row.put("QUANTITY", quantity);
    row.put("STOCK", stock); row.put("AVAILABLE", available);
    return row;
  }

  @Test void readsServerCartAndCurrentPricesForEachSession() {
    var first = service.get(session);
    assertEquals(2, first.totalQuantity());
    assertEquals(new BigDecimal("398000"), first.subtotal());
    assertTrue(first.checkoutAllowed());
    var later = new MockHttpSession();
    when(store.customer(later)).thenReturn(9L);
    assertEquals(first, service.get(later));
    verify(repo, times(2)).activeCart(9L);
  }

  @Test void mergesLegacyDuplicateLinesUnderCustomerLock() {
    when(repo.existing(41L, 12L)).thenReturn(List.of(Map.of("QUANTITY", 1),Map.of("QUANTITY", 2)));
    when(repo.sellableVariant(12L)).thenReturn(Map.of("QUANTITY", 5));
    service.add(session, 12, 2);
    var order = inOrder(repo);
    order.verify(repo).lockCustomer(9L);
    order.verify(repo).activeCart(9L);
    order.verify(repo).existing(41L, 12L);
    order.verify(repo).sellableVariant(12L);
    order.verify(repo).put(41L, 12L, 5);
  }

  @Test void rejectsCombinedQuantityOverStockWithoutWriting() {
    when(repo.existing(41L, 12L)).thenReturn(List.of(Map.of("QUANTITY", 4)));
    when(repo.sellableVariant(12L)).thenReturn(Map.of("QUANTITY", 5));
    assertThrows(BusinessException.class, () -> service.add(session,12,2));
    verify(repo, never()).put(anyLong(),anyLong(),anyInt());
  }

  @Test void updateCannotTargetAnotherCustomersCart() {
    when(repo.existing(41L, 999L)).thenReturn(List.of());
    assertThrows(BusinessException.class, () -> service.update(session,999,2));
    verify(repo, never()).put(anyLong(),anyLong(),anyInt());
  }

  @Test void removalWorksForUnavailableProductAndUsesOwnedCart() {
    service.remove(session,12);
    verify(repo).remove(41L,12L);
    verify(repo, never()).sellableVariant(anyLong());
  }

  @Test void stockChangeDisablesCheckoutWithoutDeletingSavedItems() {
    when(repo.lines(41L)).thenReturn(List.of(line(4, 2, 1)));
    var cart=service.get(session);
    assertFalse(cart.checkoutAllowed()); assertEquals(4,cart.totalQuantity());
    verify(repo, never()).clear(anyLong());
  }

  Checkout checkout(List<BillRequest.Item> items) {
    return new Checkout("Khách", "0900000000", "Hà Nội", "Cầu Giấy", "12 Đường A", "COD", null, null, items, "VCN", new BigDecimal("36000"));
  }

  @Test void staleOrDuplicatedCheckoutNeverCreatesOrder() {
    assertThrows(BusinessException.class, () -> service.checkout(session,checkout(List.of(new BillRequest.Item(12L,1)))));
    assertThrows(BusinessException.class, () -> service.checkout(session,checkout(List.of(new BillRequest.Item(12L,1),new BillRequest.Item(12L,1)))));
    verify(store, never()).checkout(any(),any());
    verify(repo, never()).finish(anyLong());
  }

  @Test void successfulOrderClosesCartAfterServerCheckout() {
    var request=checkout(List.of(new BillRequest.Item(12L,2)));
    var response=Map.of("id",88L);
    when(store.checkout(request,session)).thenReturn(response);
    assertEquals(response,service.checkout(session,request));
    var order=inOrder(store,repo);
    order.verify(store).customer(session);
    order.verify(repo).lockCustomer(9L);
    order.verify(repo).activeCart(9L);
    order.verify(repo).lines(41L);
    order.verify(store).checkout(request,session);
    order.verify(repo).finish(41L);
  }

  @Test void failedOrderKeepsCartOpen() {
    var request=checkout(List.of(new BillRequest.Item(12L,2)));
    when(store.checkout(request,session)).thenThrow(new BusinessException("Phí giao hàng thay đổi"));
    assertThrows(BusinessException.class,()->service.checkout(session,request));
    verify(repo,never()).finish(anyLong());
  }

  @Test void unauthenticatedSessionCannotReadOrWriteCart() {
    when(store.customer(session)).thenThrow(new BusinessException("Đăng nhập trước"));
    assertThrows(BusinessException.class,()->service.get(session));
    assertThrows(BusinessException.class,()->service.add(session,12,1));
    verifyNoInteractions(repo);
  }
}

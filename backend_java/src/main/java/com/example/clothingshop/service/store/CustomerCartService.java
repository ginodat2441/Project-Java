package com.example.clothingshop.service.store;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.dto.store.CustomerViews.*;
import com.example.clothingshop.dto.store.StoreRequests.Checkout;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.store.CustomerCartRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerCartService {
  private final CustomerCartRepository repo;
  private final StoreService store;
  public CustomerCartService(CustomerCartRepository repo, StoreService store) {
    this.repo = repo; this.store = store;
  }

  @Transactional(readOnly = true)
  public Cart get(HttpSession session) { return view(repo.activeCart(store.customer(session))); }

  private long open(long customer) {
    Long cart = repo.activeCart(customer);
    return cart == null ? repo.create(customer) : cart;
  }

  @Transactional
  public Cart add(HttpSession session, long variant, int quantity) {
    validateQuantity(quantity);
    long customer = store.customer(session);
    repo.lockCustomer(customer);
    long id = open(customer);
    var existing = repo.existing(id, variant);
    long before = existing.stream().mapToLong(row -> AdminRepository.number(row, "QUANTITY")).sum();
    long next = before + quantity;
    int stock = ((Number) repo.sellableVariant(variant).get("QUANTITY")).intValue();
    if (next > stock || next > 100000) throw new BusinessException("Số lượng trong giỏ vượt số hàng đang còn.");
    if (existing.isEmpty() && repo.lines(id).size() >= 100) throw new BusinessException("Giỏ hàng tối đa 100 loại sản phẩm.");
    repo.put(id, variant, (int) next);
    return view(id);
  }

  @Transactional
  public Cart update(HttpSession session, long variant, int quantity) {
    validateQuantity(quantity);
    long customer = store.customer(session);
    repo.lockCustomer(customer);
    Long id = repo.activeCart(customer);
    if (id == null || repo.existing(id, variant).isEmpty()) throw new BusinessException("Sản phẩm không còn trong giỏ hàng của bạn.");
    int stock = ((Number) repo.sellableVariant(variant).get("QUANTITY")).intValue();
    if (quantity > stock) throw new BusinessException("Số lượng vượt tồn kho hiện tại.");
    repo.put(id, variant, quantity);
    return view(id);
  }

  @Transactional
  public Cart remove(HttpSession session, long variant) {
    long customer = store.customer(session);
    repo.lockCustomer(customer);
    Long id = repo.activeCart(customer);
    if (id != null) repo.remove(id, variant);
    return view(id);
  }

  @Transactional
  public Cart clear(HttpSession session) {
    long customer = store.customer(session);
    repo.lockCustomer(customer);
    Long id = repo.activeCart(customer);
    if (id != null) repo.clear(id);
    return view(id);
  }

  @Transactional
  public Object checkout(HttpSession session, Checkout request) {
    long customer = store.customer(session);
    repo.lockCustomer(customer);
    Long id = repo.activeCart(customer);
    Cart cart = view(id);
    if (!"COD".equals(request.paymentMethod())) throw new BusinessException("Cửa hàng chỉ thanh toán khi nhận hàng (COD).");
    Map<Long, Integer> expected = new TreeMap<>();
    cart.items().forEach(line -> expected.put(line.productDetailId(), line.quantity()));
    Map<Long, Integer> submitted = new TreeMap<>();
    request.items().forEach(line -> {
      if (submitted.put(line.productDetailId(), line.quantity()) != null) throw new BusinessException("Giỏ hàng có dòng trùng.");
    });
    if (submitted.isEmpty()) throw new BusinessException("Chọn ít nhất một sản phẩm để đặt hàng.");
    for (var line : cart.items()) {
      if (submitted.containsKey(line.productDetailId()) &&
          (!line.available() || line.quantity() > line.stock()))
        throw new BusinessException("Sản phẩm đã chọn không đủ hàng. Vui lòng kiểm tra lại giỏ.");
    }
    if (submitted.entrySet().stream().anyMatch(e -> !Objects.equals(expected.get(e.getKey()), e.getValue())))
      throw new BusinessException("Giỏ hàng đã thay đổi. Tải lại giỏ trước khi đặt đơn.");
    Object order = store.checkout(request, session);
    submitted.keySet().forEach(variant -> repo.remove(id, variant));
    if (submitted.size() == expected.size()) repo.finish(id);
    return order;
  }

  private void validateQuantity(int quantity) {
    if (quantity < 1 || quantity > 100000) throw new BusinessException("Số lượng cần từ 1 đến 100.000.");
  }

  private Cart view(Long id) {
    if (id == null) return new Cart(null, List.of(), BigDecimal.ZERO, 0, false);
    List<CartLine> items = repo.lines(id).stream().map(row -> new CartLine(
        AdminRepository.number(row, "PRODUCT_DETAIL_ID"), AdminRepository.number(row, "PRODUCT_ID"),
        Objects.toString(row.get("PRODUCT_NAME"), ""), Objects.toString(row.get("COLOR_NAME"), ""),
        Objects.toString(row.get("SIZE_NAME"), ""), Objects.toString(row.get("SKU"), ""),
        Objects.toString(row.get("IMAGE_URL"), ""), (BigDecimal) row.get("PRICE"),
        ((Number) row.get("QUANTITY")).intValue(), ((Number) row.get("STOCK")).intValue(),
        AdminRepository.number(row, "AVAILABLE") == 1)).toList();
    BigDecimal subtotal = items.stream().map(line -> line.price().multiply(BigDecimal.valueOf(line.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
    int count = items.stream().mapToInt(CartLine::quantity).sum();
    boolean ready = !items.isEmpty() && items.size() <= 100 && items.stream().allMatch(line -> line.available() && line.quantity() > 0 && line.quantity() <= 100000 && line.quantity() <= line.stock());
    return new Cart(id, items, subtotal, count, ready);
  }
}

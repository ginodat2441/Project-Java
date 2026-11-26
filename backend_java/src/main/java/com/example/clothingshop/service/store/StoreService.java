package com.example.clothingshop.service.store;

import static com.example.clothingshop.repository.admin.AdminRepository.number;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.dto.order.BillRequest;
import com.example.clothingshop.dto.store.StoreRequests.*;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.store.StoreOperationsRepository;
import com.example.clothingshop.service.order.BillService;
import com.example.clothingshop.service.shipping.ShippingQuoteService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@org.springframework.stereotype.Service
public class StoreService {
  private final StoreOperationsRepository queries;

  public StoreService(
      AdminRepository db,
      PasswordEncoder encoder,
      BillService bills,
      ShippingQuoteService shipping,
      StoreOperationsRepository queries) {
    this.queries = queries;
    this.db = db;
    this.encoder = encoder;
    this.bills = bills;
    this.shipping = shipping;
  }

  final AdminRepository db;

  final PasswordEncoder encoder;

  final BillService bills;

  final ShippingQuoteService shipping;

  private final Map<String, long[]> attempts = new java.util.concurrent.ConcurrentHashMap<>();

  public Object login(
      Login r, HttpSession session, jakarta.servlet.http.HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    long now = System.currentTimeMillis();
    var count =
        attempts.compute(
            ip,
            (k, v) ->
                v == null || v[1] < now
                    ? new long[] {1, now + 600000}
                    : new long[] {v[0] + 1, v[1]});
    if (count[0] > 5) throw new BusinessException("Thử lại sau 10 phút");
    var found = queries.listUsers("e", r.email().toLowerCase(Locale.ROOT));
    String hash =
        found.isEmpty()
            ? "$2a$12$KIXxMcj5pXmZKL4yhu.9C.5DU8HiK7jZ1FkC3PzJK0jD9x8mIhEha"
            : found.get(0).get("PASSWORD_HASH").toString();
    if (!encoder.matches(r.password(), hash) || found.isEmpty())
      throw new BusinessException("Email hoặc mật khẩu không đúng");
    request.changeSessionId();
    session.setAttribute("customer", number(found.get(0), "ID"));
    session.setAttribute("customer-access-version", number(found.get(0), "AUTH_VERSION"));
    attempts.remove(ip);
    return Map.of("success", true);
  }

  @Transactional
  public Object register(
      Register r, HttpSession session, jakarta.servlet.http.HttpServletRequest request) {
    long role = number(queries.findRoles(), "ID");
    long user =
        queries.insertUsers(
            "r",
            role,
            "u",
            "customer-" + UUID.randomUUID(),
            "e",
            r.email().toLowerCase(Locale.ROOT),
            "p",
            encoder.encode(r.password()));
    long id = queries.insertCustomer("u", user, "n", r.name(), "p", r.phone());
    request.changeSessionId();
    session.setAttribute("customer", id);
    session.setAttribute("customer-access-version", 0L);
    return Map.of("success", true);
  }

  long customer(HttpSession session) {
    Object id = session.getAttribute("customer");
    if (id == null)
      throw new BusinessException("Đăng nhập tài khoản khách hàng trước khi đặt hàng");
    var found = queries.listCustomer("i", id);
    if (found.isEmpty()
        || !Long.valueOf(number(found.get(0), "AUTH_VERSION"))
            .equals(session.getAttribute("customer-access-version"))) {
      session.removeAttribute("customer");
      throw new BusinessException("Tài khoản hoặc quyền truy cập đã thay đổi. Hãy đăng nhập lại.");
    }
    return number(found.get(0), "ID");
  }

  public Object me(HttpSession session) {
    long id = customer(session);
    return queries.findCustomer("i", id);
  }

  public void logout(HttpSession s) {
    s.removeAttribute("customer");
  }

  @Transactional
  public Object checkout(Checkout r, HttpSession session) {
    long customer = customer(session);
    var amounts = amounts(r.voucherCode(), r.items());
    var offers =
        shipping.quote(
            r.province(),
            r.ward(),
            r.address(),
            r.paymentMethod(),
            r.items(),
            amounts[0],
            amounts[1]);
    var selected =
        offers.stream()
            .filter(o -> o.serviceCode().equals(r.shippingService()))
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessException(
                        "Chọn dịch vụ vận chuyển và tính lại phí trước khi đặt hàng"));
    if (selected.fee().compareTo(r.expectedShippingFee()) != 0)
      throw new BusinessException(
          "Cước Viettel Post đã thay đổi; bấm Tính lại phí và kiểm tra trước khi đặt hàng");
    long address =
        queries.insertAddress(
            "c",
            customer,
            "n",
            r.name(),
            "p",
            r.phone(),
            "pr",
            r.province(),
            "w",
            r.ward(),
            "d",
            r.address());
    var order =
        bills.createCarrier(
            new BillRequest(
                customer, address, r.paymentMethod(), null, r.note(), r.voucherCode(), r.items()),
            selected.fee(),
            amounts[0],
            amounts[1]);
    queries.updateBill("s", selected.serviceCode(), "n", selected.serviceName(), "i", order.id());
    return order;
  }

  public Object catalog(String q, Long categoryId, int page) {
    return queries.pageProductDetail(
        page, 20, "q", "%" + q.toLowerCase(Locale.ROOT) + "%", "c", categoryId);
  }

  public Object categories() {
    return queries.listCategory();
  }

  public Object product(long id) {
    return Map.of(
        "product",
        queries.findProduct("i", id),
        "variants",
        queries.listProductDetail("i", id),
        "images",
        queries.listProductImage("i", id));
  }

  public Object quote(Quote r) {
    var a = amounts(r.code(), r.items());
    var result = new LinkedHashMap<String, Object>();
    result.put("subtotal", a[0]);
    result.put("discount", a[1]);
    if (r.province() == null
        || r.province().isBlank()
        || r.ward() == null
        || r.ward().isBlank()
        || r.address() == null
        || r.address().isBlank()) {
      result.put("shippingRequired", true);
      result.put("services", List.of());
      result.put("shippingStatus", shipping.status());
      return result;
    }
    if (r.paymentMethod() == null)
      throw new BusinessException("Chọn phương thức thanh toán trước khi tính phí");
    result.put(
        "services",
        shipping.quote(
            r.province(), r.ward(), r.address(), r.paymentMethod(), r.items(), a[0], a[1]));
    result.put("shippingRequired", false);
    return result;
  }

  private BigDecimal[] amounts(String code, List<BillRequest.Item> items) {
    if (items.stream().map(BillRequest.Item::productDetailId).distinct().count() != items.size())
      throw new BusinessException("Gộp các dòng trùng SKU trước khi tính phí");
    BigDecimal subtotal = BigDecimal.ZERO;
    for (var i : items) {
      var p = queries.findProductDetail("i", i.productDetailId(), "q", i.quantity());
      subtotal =
          subtotal.add(((BigDecimal) p.get("PRICE")).multiply(BigDecimal.valueOf(i.quantity())));
    }
    return new BigDecimal[] {subtotal, bills.discount(code, subtotal, false)};
  }
}

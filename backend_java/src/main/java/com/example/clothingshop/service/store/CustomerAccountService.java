package com.example.clothingshop.service.store;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.dto.store.CustomerRequests.*;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.store.CustomerAccountRepository;
import com.example.clothingshop.repository.store.CustomerCartRepository;
import com.example.clothingshop.service.order.BillService;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerAccountService {
  private final StoreService store;
  private final CustomerAccountRepository repo;
  private final CustomerCartRepository locks;
  private final PasswordEncoder encoder;
  private final BillService bills;
  public CustomerAccountService(StoreService store, CustomerAccountRepository repo,
      CustomerCartRepository locks, PasswordEncoder encoder, BillService bills) {
    this.store = store; this.repo = repo; this.locks = locks; this.encoder = encoder; this.bills = bills;
  }

  @Transactional
  public Object profile(HttpSession session, Profile request) {
    long customer = store.customer(session);
    locks.lockCustomer(customer);
    var user = repo.user(customer);
    long userId = AdminRepository.number(user, "USER_ID");
    String email = request.email().strip().toLowerCase(Locale.ROOT);
    if (repo.emailUsed(email, userId)) throw new BusinessException("Email này đã được sử dụng. Hãy chọn email khác.");
    repo.profile(customer, userId, request, email);
    return store.me(session);
  }

  @Transactional
  public void password(HttpSession session, Password request) {
    validPassword(request.currentPassword(), false);
    validPassword(request.newPassword(), true);
    long customer = store.customer(session);
    locks.lockCustomer(customer);
    var user = repo.user(customer);
    if (!encoder.matches(request.currentPassword(), user.get("PASSWORD_HASH").toString()))
      throw new BusinessException("Mật khẩu hiện tại chưa đúng.");
    if (request.currentPassword().equals(request.newPassword())) throw new BusinessException("Chọn mật khẩu mới khác mật khẩu hiện tại.");
    repo.password(AdminRepository.number(user, "USER_ID"), encoder.encode(request.newPassword()));
    session.removeAttribute("customer");
    session.removeAttribute("customer-access-version");
  }

  static void validPassword(String password, boolean isNew) {
    if (password == null || password.isBlank() || (isNew && password.length() < 12)
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new BusinessException("Mật khẩu cần ít nhất 12 ký tự và tối đa 72 byte UTF-8.");
  }

  public Object addresses(HttpSession session) { return repo.addresses(store.customer(session)); }

  @Transactional
  public Object address(HttpSession session, Long id, Address request) {
    long customer = store.customer(session);
    locks.lockCustomer(customer);
    var rows = repo.addresses(customer);
    if (id == null && rows.size() >= 20) throw new BusinessException("Bạn có thể lưu tối đa 20 địa chỉ.");
    boolean wasDefault = id != null && AdminRepository.number(repo.ownedAddress(customer, id), "IS_DEFAULT") == 1;
    repo.address(customer, id, request, request.isDefault() || rows.isEmpty() || wasDefault);
    return repo.addresses(customer);
  }

  @Transactional
  public Object removeAddress(HttpSession session, long id) {
    long customer = store.customer(session);
    locks.lockCustomer(customer);
    repo.ownedAddress(customer, id);
    repo.removeAddress(customer, id);
    var remaining = repo.addresses(customer);
    if (!remaining.isEmpty() && remaining.stream().noneMatch(row -> AdminRepository.number(row, "IS_DEFAULT") == 1))
      repo.defaultAddress(customer, AdminRepository.number(remaining.get(0), "ID"));
    return repo.addresses(customer);
  }

  @Transactional
  public Object defaultAddress(HttpSession session, long id) {
    long customer = store.customer(session);
    locks.lockCustomer(customer);
    repo.ownedAddress(customer, id);
    repo.defaultAddress(customer, id);
    return repo.addresses(customer);
  }

  public Object orders(HttpSession session) { return bills.byCustomer(store.customer(session)); }
  public Object order(HttpSession session, long id) {
    repo.ownedOrder(store.customer(session), id);
    return Map.of("order", bills.get(id), "lines", bills.lines(id));
  }
}

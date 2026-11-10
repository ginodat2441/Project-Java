package com.example.clothingshop.service.inventory;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.common.*;
import com.example.clothingshop.repository.admin.AdminOperationsRepository;
import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryMovementService {
  private final AdminRepository db;
  private final AdminOperationsRepository queries;
  private final PasswordEncoder encoder;

  public InventoryMovementService(
      AdminRepository db, AdminOperationsRepository queries, PasswordEncoder encoder) {
    this.db = db;
    this.queries = queries;
    this.encoder = encoder;
  }

  public void movement(long variant, int delta, String reason, Long actor, Long bill) {
    queries.updateInventoryHistory("p", variant, "q", delta, "r", reason, "a", actor, "b", bill);
  }

  @Transactional
  public void stock(long id, Stock r, long actor) {
    throw new BusinessException(
        "Không sửa tồn trực tiếp. Lập phiếu nhập/xuất/kiểm kê và chờ ADMIN xác nhận.");
  }
}

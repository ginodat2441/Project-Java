package com.example.clothingshop.service.account;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.common.*;
import com.example.clothingshop.repository.admin.AdminOperationsRepository;
import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
  private final AdminRepository db;
  private final AdminOperationsRepository queries;
  private final PasswordEncoder encoder;

  public CustomerService(
      AdminRepository db, AdminOperationsRepository queries, PasswordEncoder encoder) {
    this.db = db;
    this.queries = queries;
    this.encoder = encoder;
  }

  @Transactional
  public void customer(long id, int status, long actor) {
    if (status != 0 && status != 1) throw new BusinessException("Trạng thái không hợp lệ");
    if (queries.updateUsers("s", status, "i", id) == 0)
      throw new NotFoundException("Không tìm thấy khách hàng");
    db.audit(actor, "STATUS", "CUSTOMER", id);
  }
}

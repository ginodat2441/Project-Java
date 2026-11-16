package com.example.clothingshop.service.promotion;

import static com.example.clothingshop.dto.admin.AdminRequests.*;
import static com.example.clothingshop.repository.admin.AdminRepository.number;

import com.example.clothingshop.common.*;
import com.example.clothingshop.repository.admin.AdminOperationsRepository;
import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherService {
  private final AdminRepository db;
  private final AdminOperationsRepository queries;
  private final PasswordEncoder encoder;

  public VoucherService(
      AdminRepository db, AdminOperationsRepository queries, PasswordEncoder encoder) {
    this.db = db;
    this.queries = queries;
    this.encoder = encoder;
  }

  @Transactional
  public long voucher(Long id, Voucher r, long actor) {
    if (!r.endsAt().isAfter(r.startsAt())
        || (r.type().equals("PERCENT")
            && (r.value().compareTo(java.math.BigDecimal.valueOf(100)) > 0
                || r.maxDiscount().signum() == 0)))
      throw new BusinessException("Thời gian hoặc mức giảm không hợp lệ");
    if (id != null) {
      var old = queries.findVoucher("i", id);
      if (number(old, "USED_COUNT") > r.usageLimit())
        throw new BusinessException("Giới hạn thấp hơn lượt đã dùng");
    }
    Object[] p = {
      "c",
      r.code().toUpperCase(Locale.ROOT),
      "t",
      r.type(),
      "v",
      r.value(),
      "min",
      r.minOrder(),
      "max",
      r.maxDiscount(),
      "start",
      java.sql.Timestamp.valueOf(r.startsAt()),
      "end",
      java.sql.Timestamp.valueOf(r.endsAt()),
      "lim",
      r.usageLimit(),
      "s",
      r.status(),
      "i",
      id
    };
    if (id == null) id = queries.insertVoucher(p);
    else queries.updateVoucher(p);
    db.audit(actor, "SAVE", "VOUCHER", id);
    return id;
  }
}

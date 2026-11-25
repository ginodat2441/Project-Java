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
    if (!r.endsAt().isAfter(r.startsAt()))
      throw new BusinessException("Hết hạn phải sau thời điểm Có hiệu lực từ.");
    if (r.type().equals("PERCENT") && r.value().compareTo(java.math.BigDecimal.valueOf(100)) > 0)
      throw new BusinessException("Mức giảm không được vượt quá 100%.");
    if (r.type().equals("PERCENT") && r.maxDiscount().signum() == 0)
      throw new BusinessException("Giảm tối đa phải lớn hơn 0 khi dùng loại giảm Phần trăm.");
    if (id != null) {
      var old = queries.findVoucher("i", id);
      if (number(old, "USED_COUNT") > r.usageLimit())
        throw new BusinessException("Giới hạn lượt không được thấp hơn số lượt đã dùng.");
    }
    List<Long> productIds;
    if (r.productIds() == null && id != null) {
      productIds =
          queries.voucherProducts("i", id).stream()
              .map(product -> number(product, "ID"))
              .toList();
    } else {
      productIds =
          r.productIds() == null ? List.of() : r.productIds().stream().distinct().toList();
    }
    for (Long productId : productIds) {
      if (queries.findVoucherProduct("i", productId).isEmpty())
        throw new BusinessException("Sản phẩm được áp dụng có mục đã bị xóa hoặc không còn tồn tại.");
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
    try {
      if (id == null) id = queries.insertVoucher(p);
      else queries.updateVoucher(p);
    } catch (org.springframework.dao.DuplicateKeyException e) {
      throw new BusinessException("Mã voucher đã được sử dụng.");
    }
    queries.deleteVoucherProducts("i", id);
    for (Long productId : productIds) {
      queries.insertVoucherProduct("v", id, "p", productId);
    }
    db.audit(actor, "SAVE", "VOUCHER", id);
    return id;
  }
}

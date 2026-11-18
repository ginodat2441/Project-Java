package com.example.clothingshop.service.promotion;

import static com.example.clothingshop.dto.admin.AdminRequests.*;
import static com.example.clothingshop.repository.admin.AdminRepository.number;

import com.example.clothingshop.common.*;
import com.example.clothingshop.repository.admin.AdminOperationsRepository;
import com.example.clothingshop.repository.admin.AdminRepository;
import java.time.LocalDateTime;
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
    var old = id == null ? null : queries.findVoucher("i", id);
    LocalDateTime startsAt = r.startsAt();
    if (r.noExpiry()) {
      startsAt =
          old == null
              ? LocalDateTime.now()
              : ((java.sql.Timestamp) old.get("STARTS_AT")).toLocalDateTime();
    }
    LocalDateTime endsAt = r.noExpiry() ? null : r.endsAt();
    if (startsAt == null
        || (!r.noExpiry() && (endsAt == null || !endsAt.isAfter(startsAt)))
        || (r.type().equals("PERCENT")
            && (r.value().compareTo(java.math.BigDecimal.valueOf(100)) > 0
                || r.maxDiscount().signum() == 0)))
      throw new BusinessException("Thời gian hoặc mức giảm không hợp lệ");
    if ((r.newAccountDays() != null
            && (r.newAccountDays() < 1 || r.newAccountDays() > 36500))
        || (r.minimumOrders() != null && r.minimumOrders() < 1))
      throw new BusinessException("Điều kiện nhóm khách hàng không hợp lệ");
    if (id != null) {
      if (number(old, "USED_COUNT") > r.usageLimit())
        throw new BusinessException("Giới hạn thấp hơn lượt đã dùng");
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
        throw new BusinessException("Có sản phẩm không tồn tại hoặc đã bị xóa");
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
      java.sql.Timestamp.valueOf(startsAt),
      "end",
      endsAt == null ? null : java.sql.Timestamp.valueOf(endsAt),
      "lim",
      r.usageLimit(),
      "s",
      r.status(),
      "newAccountDays",
      r.newAccountDays(),
      "noPreviousOrders",
      r.noPreviousOrders() ? 1 : 0,
      "minimumOrders",
      r.minimumOrders(),
      "i",
      id
    };
    if (id == null) id = queries.insertVoucher(p);
    else queries.updateVoucher(p);
    queries.deleteVoucherProducts("i", id);
    for (Long productId : productIds) {
      queries.insertVoucherProduct("v", id, "p", productId);
    }
    db.audit(actor, "SAVE", "VOUCHER", id);
    return id;
  }
}

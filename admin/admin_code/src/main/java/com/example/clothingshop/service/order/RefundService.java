package com.example.clothingshop.service.order;

import static com.example.clothingshop.repository.admin.AdminRepository.number;

import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.order.OrderOperationsRepository;
import com.example.clothingshop.service.inventory.InventoryService;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {
  private final OrderOperationsRepository queries;
  private final AdminRepository db;
  private final InventoryService inventory;

  public RefundService(
      AdminRepository db, InventoryService inventory, OrderOperationsRepository queries) {
    this.queries = queries;
    this.db = db;
    this.inventory = inventory;
  }

  public record Request(
      @NotNull @Positive Long documentId,
      @NotNull @DecimalMin("1") @Digits(integer = 15, fraction = 0) BigDecimal amount,
      @NotBlank @Pattern(regexp = "PENDING|PAID|FAILED") String status,
      @NotBlank @Size(max = 150) String reference,
      @NotBlank @Size(max = 500) String reason) {}

  @Transactional
  public long save(long bill, Long id, Request r, long actor) {
    if (!inventory.isAdmin(actor))
      throw new org.springframework.security.access.AccessDeniedException(
          "Chỉ ADMIN xác nhận hoàn tiền");
    var b = queries.findBill4("b", bill);
    if (number(b, "PAYMENT_STATUS") != 1 && number(b, "PAYMENT_STATUS") != 2)
      throw new IllegalArgumentException("Chưa ghi nhận đã thu tiền, không thể hoàn tiền");
    queries.findInventoryDocument("d", r.documentId(), "b", bill);
    if (id != null) {
      var old = queries.findBillRefund("i", id, "b", bill);
      if (!old.get("STATUS").equals("PENDING"))
        throw new IllegalArgumentException("Khoản hoàn đã kết thúc, không ghi nhận lại");
    }
    var a = queries.findInventoryDocumentLine("d", r.documentId(), "i", id);
    if (!r.status().equals("FAILED")
        && r.amount().compareTo(((BigDecimal) a.get("CAP")).subtract((BigDecimal) a.get("USED")))
            > 0)
      throw new IllegalArgumentException(
          "Số tiền hoàn vượt giá trị hàng nhận hoàn còn được hoàn tiền");
    Object[] p = {
      "b",
      bill,
      "d",
      r.documentId(),
      "v",
      r.amount(),
      "s",
      r.status(),
      "ref",
      r.reference(),
      "r",
      r.reason(),
      "a",
      actor,
      "i",
      id
    };
    if (id == null) id = queries.insertBillRefund(p);
    else queries.updateBillRefund(p);
    db.audit(actor, "REFUND_" + r.status(), "BILL_REFUND", id);
    return id;
  }
}

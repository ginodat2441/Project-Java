package com.example.clothingshop.service.inventory;

import static com.example.clothingshop.repository.admin.AdminRepository.number;

import com.example.clothingshop.dto.inventory.SkuCostRequests.*;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.inventory.InventoryOperationsRepository;
import com.example.clothingshop.service.admin.AdminService;
import jakarta.validation.constraints.*;
import java.math.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@org.springframework.stereotype.Service
public class SkuCostService {
  private final InventoryOperationsRepository queries;

  public SkuCostService(
      AdminRepository d,
      AdminService a,
      StockCostService c,
      InventoryOperationsRepository queries) {
    this.queries = queries;
    db = d;
    admin = a;
    costs = c;
  }

  private final AdminRepository db;

  private final AdminService admin;

  private final StockCostService costs;

  public Object preview(long id) {
    return Map.of(
        "stock",
        queries.findProductDetail3("i", id),
        "receipts",
        queries.listInventoryDocumentLine4("i", id),
        "impact",
        Map.of(
            "quantityChanged",
            false,
            "scope",
            "CURRENT_BALANCE_AND_FUTURE_DISPATCHES",
            "historicalSalesChanged",
            false));
  }

  @Transactional
  public void snapshot(long id, Snapshot r, Authentication a) {
    long actor = admin.actor(a.getName());
    costs.lockVariants(List.of(id));
    var before = queries.findProductDetail4("i", id);
    long q = number(before, "PHYSICAL_QUANTITY");
    if (q != r.expectedQuantity())
      throw new IllegalArgumentException("Số dư đã thay đổi; mở lại xem trước");
    BigDecimal effective = q == 0 ? BigDecimal.ZERO : r.unitCost(),
        value = effective.multiply(BigDecimal.valueOf(q)).setScale(8, RoundingMode.HALF_UP);
    queries.updateStockCostAdjustment(
        "i",
        id,
        "q",
        q,
        "before",
        before.get("AVERAGE_COST"),
        "after",
        effective,
        "bv",
        before.get("KNOWN_VALUE"),
        "av",
        value,
        "r",
        r.reason(),
        "e",
        r.evidence(),
        "a",
        actor);
    queries.updateProductDetail3("c", q == 0 ? BigDecimal.ZERO : r.unitCost(), "v", value, "i", id);
    db.auditChange(
        actor,
        "COST_SNAPSHOT",
        "PRODUCT_DETAIL",
        id,
        before,
        Map.of("quantity", q, "unitCost", effective, "value", value, "evidence", r.evidence()),
        r.reason());
  }
}

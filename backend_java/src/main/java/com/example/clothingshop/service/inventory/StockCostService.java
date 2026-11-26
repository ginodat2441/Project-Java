package com.example.clothingshop.service.inventory;

import static com.example.clothingshop.repository.admin.AdminRepository.number;

import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.inventory.InventoryOperationsRepository;
import java.math.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** Moving average of physical stock for each immutable variant ID. */
@Service
public class StockCostService {
  private final InventoryOperationsRepository queries;
  private final AdminRepository db;

  public StockCostService(AdminRepository db, InventoryOperationsRepository queries) {
    this.queries = queries;
    this.db = db;
  }

  public void lockVariants(Collection<Long> variants) {
    var products = new TreeSet<Long>();
    for (long id : variants)
      products.add(number(queries.findProductDetail5("i", id), "PRODUCT_ID"));
    for (long id : products) queries.findProduct("i", id);
    for (long id : new TreeSet<>(variants)) queries.findProductDetail6("i", id);
  }

  public BigDecimal current(long variant) {
    return (BigDecimal) queries.findProductDetail7("i", variant).get("AVERAGE_COST");
  }

  public BigDecimal sold(long bill, long variant) {
    var r = queries.findBillDetail3("b", bill, "v", variant);
    return number(r, "N") == 0 || number(r, "N") != number(r, "KNOWN")
        ? null
        : ((BigDecimal) r.get("V")).divide((BigDecimal) r.get("Q"), 8, RoundingMode.HALF_UP);
  }

  public static BigDecimal adjusted(long before, BigDecimal average, long delta, BigDecimal unit) {
    if (before + delta == 0) return BigDecimal.ZERO;
    if ((before > 0 && average == null) || (delta != 0 && unit == null)) return null;
    BigDecimal value =
        (before == 0 ? BigDecimal.ZERO : average.multiply(BigDecimal.valueOf(before)))
            .add(unit == null ? BigDecimal.ZERO : unit.multiply(BigDecimal.valueOf(delta)));
    if (value.signum() < 0)
      throw new IllegalArgumentException(
          "Giá trị tồn âm; kiểm tra phiếu đảo và các lần nhập/xuất sau phiếu gốc");
    return value.divide(BigDecimal.valueOf(before + delta), 8, RoundingMode.HALF_UP);
  }

  public void movement(long variant, long delta, BigDecimal unit) {
    var r = queries.findProductDetail4("i", variant);
    long before = number(r, "PHYSICAL_QUANTITY");
    BigDecimal known = (BigDecimal) r.get("KNOWN_VALUE"),
        unknown = (BigDecimal) r.get("UNKNOWN_QUANTITY");
    BigDecimal next = adjusted(before, (BigDecimal) r.get("AVERAGE_COST"), delta, unit);
    if (before + delta == 0) {
      known = BigDecimal.ZERO;
      unknown = BigDecimal.ZERO;
    } else if (delta > 0) {
      if (unit == null) unknown = unknown.add(BigDecimal.valueOf(delta));
      else known = known.add(unit.multiply(BigDecimal.valueOf(delta)));
    } else if (delta < 0) {
      if (next != null) {
        known = next.multiply(BigDecimal.valueOf(before + delta));
        unknown = BigDecimal.ZERO;
      } else if (before > 0) {
        BigDecimal ratio =
            BigDecimal.valueOf(before + delta)
                .divide(BigDecimal.valueOf(before), 16, RoundingMode.HALF_UP);
        known = known.multiply(ratio).setScale(8, RoundingMode.HALF_UP);
        unknown = unknown.multiply(ratio).setScale(8, RoundingMode.HALF_UP);
      }
    }
    if (unknown.signum() == 0 && before + delta > 0)
      next = known.divide(BigDecimal.valueOf(before + delta), 8, RoundingMode.HALF_UP);
    queries.updateProductDetail4("a", next, "v", known, "u", unknown, "i", variant);
  }

  public void snapshotSale(long bill) {
    queries.updateBillDetail("b", bill);
  }
}

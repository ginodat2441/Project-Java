package com.example.clothingshop.service.inventory;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StockCostServiceTest {
  private BigDecimal n(String value) {
    return new BigDecimal(value);
  }

  @Test
  void movingAverageUsesRemainingStockAndNewReceipt() {
    assertEquals(n("150.00000000"), StockCostService.adjusted(10, n("100"), 10, n("200")));
    // Selling six units at 150 removes 900 in cost; later receipts use only the remaining stock.
    assertEquals(n("150.00000000"), StockCostService.adjusted(20, n("150"), -6, n("150")));
    assertEquals(n("195.00000000"), StockCostService.adjusted(14, n("150"), 6, n("300")));
  }

  @Test
  void cancellationAndReturnRestoreOriginalSaleCost() {
    assertEquals(n("180.00000000"), StockCostService.adjusted(8, n("200"), 2, n("100")));
    assertEquals(n("100.00000000"), StockCostService.adjusted(20, n("150"), -10, n("200")));
  }

  @Test
  void unknownOpeningCostIsNeverTreatedAsZero() {
    assertNull(StockCostService.adjusted(10, null, 5, n("200")));
    assertNull(StockCostService.adjusted(10, n("100"), 2, null));
    assertEquals(n("200.00000000"), StockCostService.adjusted(0, null, 5, n("200")));
    assertEquals(BigDecimal.ZERO, StockCostService.adjusted(10, null, -10, null));
  }

  @Test
  void reversalCannotMakeRemainingValueNegative() {
    assertThrows(
        IllegalArgumentException.class, () -> StockCostService.adjusted(2, n("100"), -1, n("300")));
  }
}

package com.example.clothingshop.dto.store;

import java.math.BigDecimal;
import java.util.List;

public final class CustomerViews {
  private CustomerViews() {}

  public record CartLine(long productDetailId, long productId, String productName,
      String colorName, String sizeName, String sku, String imageUrl, BigDecimal price,
      int quantity, int stock, boolean available) {}
  public record Cart(Long id, List<CartLine> items, BigDecimal subtotal,
      int totalQuantity, boolean checkoutAllowed) {}
}

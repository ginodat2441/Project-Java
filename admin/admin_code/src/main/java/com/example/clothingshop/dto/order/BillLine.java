package com.example.clothingshop.dto.order;

import java.math.BigDecimal;

public record BillLine(
    Long id,
    Long productDetailId,
    String productName,
    String colorName,
    String sizeName,
    String sku,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice) {}

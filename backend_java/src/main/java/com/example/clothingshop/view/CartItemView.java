package com.example.clothingshop.view;

import java.math.BigDecimal;

/**
 * 1 dòng trong giỏ, kết quả JOIN CART_DETAIL + PRODUCT_DETAIL + PRODUCT + COLOR.
 * stock = tồn kho hiện tại, available = 1 nếu biến thể còn bán (chưa xóa, STATUS = 1).
 */
public record CartItemView(Long id, Long productDetailId, Long productId, String productName,
                           String colorName, String sizeName, String sku, String imageUrl,
                           BigDecimal unitPrice, Integer quantity, BigDecimal totalPrice,
                           Integer stock, Integer available) {}

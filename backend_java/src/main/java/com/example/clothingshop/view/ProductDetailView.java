package com.example.clothingshop.view;

import java.math.BigDecimal;

public record ProductDetailView(Long id, Long productId, String colorName, String hexCode,
                                String sizeName, String sku, BigDecimal price,
                                Integer quantity, String imageUrl) {}

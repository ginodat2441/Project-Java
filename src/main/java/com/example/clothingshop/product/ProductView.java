package com.example.clothingshop.product;

import java.math.BigDecimal;

/** Kết quả JOIN PRODUCT + BRAND + MATERIAL + subquery giá/tồn kho */
public record ProductView(Long id, String productCode, String productName, String brandName,
                          String materialName, Integer targetGender, String thumbnailUrl,
                          Integer status, BigDecimal minPrice, Long totalStock) {}

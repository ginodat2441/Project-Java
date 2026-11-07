package com.example.clothingshop.view;

import java.math.BigDecimal;

/** View = 1 dòng sản phẩm của hóa đơn, kết quả JOIN BILL_DETAIL + PRODUCT_DETAIL + PRODUCT + COLOR. */
public record BillLineView(Long id, Long productDetailId, String productName, String colorName,
                           String sizeName, String sku, Integer quantity,
                           BigDecimal unitPrice, BigDecimal totalPrice) {}

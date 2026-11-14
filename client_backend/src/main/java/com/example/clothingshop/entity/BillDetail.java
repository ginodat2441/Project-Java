package com.example.clothingshop.entity;

import java.math.BigDecimal;

/** Entity = 1 dòng của bảng BILL_DETAIL (không có tên sản phẩm/màu/size - cái đó thuộc về view). */
public record BillDetail(Long id, Long billId, Long productDetailId, Integer quantity,
                         BigDecimal unitPrice, BigDecimal totalPrice) {}

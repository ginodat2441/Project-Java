package com.example.clothingshop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entity = 1 dòng của bảng BILL (đúng tên cột, không JOIN). */
public record Bill(Long id, String billCode, Long customerId, Long addressId,
                   String receiverName, String receiverPhone, String shippingAddress,
                   BigDecimal subTotal, BigDecimal shippingFee, BigDecimal discountAmount,
                   BigDecimal totalAmount, String paymentMethod, Integer paymentStatus,
                   Integer status, String note, Long handledBy,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {}

package com.example.clothingshop.bill;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillView(Long id, String billCode, Long customerId, String receiverName,
                       String receiverPhone, String shippingAddress, BigDecimal subTotal,
                       BigDecimal shippingFee, BigDecimal discountAmount, BigDecimal totalAmount,
                       String paymentMethod, Integer paymentStatus, Integer status,
                       LocalDateTime createdAt) {}

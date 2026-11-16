package com.example.clothingshop.entity;

import java.math.BigDecimal;

public record SePayPaymentAttempt(String invoiceNumber, Long billId, BigDecimal amount, Integer status) {}

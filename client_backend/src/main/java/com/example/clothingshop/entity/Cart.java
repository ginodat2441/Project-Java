package com.example.clothingshop.entity;

import java.time.LocalDateTime;

/** 1 dòng của bảng CART. STATUS: 1 = đang dùng, 0 = đã checkout. */
public record Cart(Long id, Long customerId, Integer status,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {}

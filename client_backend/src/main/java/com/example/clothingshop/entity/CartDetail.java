package com.example.clothingshop.entity;

import java.time.LocalDateTime;

/** 1 dòng của bảng CART_DETAIL. */
public record CartDetail(Long id, Long cartId, Long productDetailId, Integer quantity,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {}

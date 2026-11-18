package com.example.clothingshop.entity;

import java.time.LocalDateTime;

/** 1 dòng của bảng COLOR. */
public record Color(Long id, String colorName, String hexCode,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {}

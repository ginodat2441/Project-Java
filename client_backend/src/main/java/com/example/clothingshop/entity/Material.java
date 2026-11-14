package com.example.clothingshop.entity;

import java.time.LocalDateTime;

/** 1 dòng của bảng MATERIAL. */
public record Material(Long id, String materialName, String description,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {}

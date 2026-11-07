package com.example.clothingshop.entity;

import java.time.LocalDateTime;

public record Brand(Long id, String brandName, String country, String logoUrl,
                    String description, LocalDateTime createdAt, LocalDateTime updatedAt) {}

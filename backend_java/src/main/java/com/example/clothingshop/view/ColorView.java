package com.example.clothingshop.view;

import com.example.clothingshop.entity.Color;

import java.time.LocalDateTime;

public record ColorView(Long id, String colorName, String hexCode,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static ColorView from(Color c) {
        return new ColorView(c.id(), c.colorName(), c.hexCode(), c.createdAt(), c.updatedAt());
    }
}

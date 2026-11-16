package com.example.clothingshop.view;

import com.example.clothingshop.entity.Material;

import java.time.LocalDateTime;

public record MaterialView(Long id, String materialName, String description,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static MaterialView from(Material m) {
        return new MaterialView(m.id(), m.materialName(), m.description(), m.createdAt(), m.updatedAt());
    }
}

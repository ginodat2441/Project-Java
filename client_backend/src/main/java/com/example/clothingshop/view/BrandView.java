package com.example.clothingshop.view;

import com.example.clothingshop.entity.Brand;

import java.time.LocalDateTime;

/** View = dữ liệu thương hiệu trả ra API. */
public record BrandView(Long id, String brandName, String country, String logoUrl,
                        String description, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static BrandView from(Brand b) {
        return new BrandView(b.id(), b.brandName(), b.country(), b.logoUrl(),
                b.description(), b.createdAt(), b.updatedAt());
    }
}

package com.example.clothingshop.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandRequest(
    @NotBlank @Size(max = 150) String brandName,
    String country,
    String logoUrl,
    String description) {}

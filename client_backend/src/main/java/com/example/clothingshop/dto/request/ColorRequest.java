package com.example.clothingshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ColorRequest(@NotBlank @Size(max = 100) String colorName,
                           @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "phải dạng #RRGGBB, ví dụ #FF0000")
                           String hexCode) {}

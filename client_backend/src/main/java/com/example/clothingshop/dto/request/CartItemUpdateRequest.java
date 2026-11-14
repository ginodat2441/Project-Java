package com.example.clothingshop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Đặt lại số lượng của 1 dòng trong giỏ. */
public record CartItemUpdateRequest(@NotNull @Min(1) @Max(100) Integer quantity) {}

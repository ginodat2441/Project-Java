package com.example.clothingshop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Thêm sản phẩm vào giỏ. */
public record CartItemRequest(@NotNull Long productDetailId,
                              @NotNull @Min(1) @Max(100) Integer quantity) {}

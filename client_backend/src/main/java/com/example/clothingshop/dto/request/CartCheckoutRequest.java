package com.example.clothingshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Đặt hàng từ giỏ: sản phẩm lấy từ giỏ nên chỉ cần địa chỉ + thanh toán. */
public record CartCheckoutRequest(@NotNull Long addressId,
                                  @NotBlank @Pattern(regexp = "COD|SEPAY") String paymentMethod,
                                  @Size(max = 500) String note) {}

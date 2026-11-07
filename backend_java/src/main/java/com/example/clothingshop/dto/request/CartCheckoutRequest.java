package com.example.clothingshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Đặt hàng từ giỏ: sản phẩm lấy từ giỏ nên chỉ cần địa chỉ + thanh toán. */
public record CartCheckoutRequest(@NotNull Long addressId,
                                  @NotBlank @Pattern(regexp = "COD|BANK") String paymentMethod,
                                  @PositiveOrZero BigDecimal shippingFee,
                                  String note) {}

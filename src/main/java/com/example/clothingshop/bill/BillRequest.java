package com.example.clothingshop.bill;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record BillRequest(
        @NotNull Long customerId,
        @NotNull Long addressId,
        @NotBlank @Pattern(regexp = "COD|BANK|MOMO|VNPAY") String paymentMethod,
        BigDecimal shippingFee,
        String note,
        @NotEmpty @Valid List<Item> items) {

    public record Item(@NotNull Long productDetailId, @NotNull @Min(1) Integer quantity) {}
}

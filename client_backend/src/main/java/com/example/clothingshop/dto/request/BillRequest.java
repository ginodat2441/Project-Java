package com.example.clothingshop.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record BillRequest(
        @NotNull Long addressId,
        @NotBlank @Pattern(regexp = "COD|SEPAY") String paymentMethod,
        @Size(max = 500) String note,
        @NotEmpty @Valid List<Item> items) {

    public record Item(@NotNull @Positive Long productDetailId, @NotNull @Min(1) @Max(100) Integer quantity) {}
}

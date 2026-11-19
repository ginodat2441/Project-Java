package com.example.clothingshop.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VoucherQuoteRequest(
        @NotBlank @Size(max = 50) String code,
        @NotEmpty @Size(max = 100) @Valid List<BillRequest.Item> items) {}

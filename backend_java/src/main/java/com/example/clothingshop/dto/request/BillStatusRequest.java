package com.example.clothingshop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BillStatusRequest(
        @NotNull @Min(0) @Max(5) Integer status,
        @Size(max = 500) String note) {}

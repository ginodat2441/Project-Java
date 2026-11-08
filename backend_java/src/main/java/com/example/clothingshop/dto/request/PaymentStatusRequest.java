package com.example.clothingshop.dto.request;

import jakarta.validation.constraints.NotNull;

public record PaymentStatusRequest(@NotNull Boolean paid) {}

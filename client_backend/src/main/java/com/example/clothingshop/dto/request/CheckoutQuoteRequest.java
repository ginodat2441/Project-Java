package com.example.clothingshop.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CheckoutQuoteRequest(@NotEmpty @Size(max=100) @Valid List<BillRequest.Item> items) {}

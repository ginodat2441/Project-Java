package com.example.clothingshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 150) String receiverName,
        @NotBlank @Pattern(regexp = "^[0-9+() .-]{8,20}$") String receiverPhone,
        @NotBlank @Size(max = 100) String province,
        @NotBlank @Size(max = 100) String ward,
        @NotBlank @Size(max = 300) String detailAddress,
        Boolean isDefault) {}

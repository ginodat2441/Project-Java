package com.example.clothingshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message="Vui lòng nhập email.") @Email(message="Email không đúng định dạng.") @jakarta.validation.constraints.Size(max=255) String email,
                           @NotBlank(message="Vui lòng nhập mật khẩu.") @jakarta.validation.constraints.Size(max=72) String password) {
    public LoginRequest { email=com.example.clothingshop.common.AccountInputPolicy.email(email); }
}

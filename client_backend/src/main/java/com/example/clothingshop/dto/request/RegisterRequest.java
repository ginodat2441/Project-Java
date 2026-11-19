package com.example.clothingshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message="Vui lòng nhập tên đăng nhập.") @Size(min=3,max=30,message="Tên đăng nhập phải có 3–30 ký tự.") String username,
        @NotBlank(message="Vui lòng nhập email.") @Email(message="Email không đúng định dạng.") @Size(max=255) String email,
        @NotBlank(message="Vui lòng nhập mật khẩu.") @Size(min=12,max=72,message="Mật khẩu phải có ít nhất 12 ký tự.") String password,
        @NotBlank(message="Vui lòng nhập họ tên.") @Size(max=150) String fullName,
        @NotBlank(message="Vui lòng nhập số điện thoại.") String phone) {
    public RegisterRequest {
        username=com.example.clothingshop.common.AccountInputPolicy.text(username).toLowerCase(java.util.Locale.ROOT);
        email=com.example.clothingshop.common.AccountInputPolicy.email(email);
        fullName=com.example.clothingshop.common.AccountInputPolicy.text(fullName).replaceAll("\\s+"," ");
        phone=com.example.clothingshop.common.AccountInputPolicy.phone(phone);
    }
}

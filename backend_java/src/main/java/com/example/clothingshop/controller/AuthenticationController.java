package com.example.clothingshop.controller;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.CustomerIdentity;
import com.example.clothingshop.dto.request.LoginRequest;
import com.example.clothingshop.dto.request.RegisterRequest;
import com.example.clothingshop.service.AuthenticationService;
import com.example.clothingshop.view.AuthView;
import com.example.clothingshop.view.AuthenticatedUserView;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService service;

    public AuthenticationController(AuthenticationService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ApiResponse<AuthView> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Đăng ký thành công", service.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthView> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Đăng nhập thành công", service.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthenticatedUserView> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(service.currentUser(CustomerIdentity.customerId(jwt)));
    }
}
package com.example.clothingshop.view;

public record AdminAuthView(String tokenType, String accessToken, long expiresIn,
                            Long managerId, String username, String fullName, String email, String role) {}

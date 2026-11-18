package com.example.clothingshop.view;

public record AuthView(String tokenType, String accessToken, long expiresIn, AuthenticatedUserView user) {}

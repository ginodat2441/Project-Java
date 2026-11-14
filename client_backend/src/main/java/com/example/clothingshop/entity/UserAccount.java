package com.example.clothingshop.entity;

public record UserAccount(Long userId, Long customerId, String username, String email,
                          String passwordHash, Integer status, String fullName, String phone) {}

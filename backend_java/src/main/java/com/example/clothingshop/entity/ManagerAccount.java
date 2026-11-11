package com.example.clothingshop.entity;

public record ManagerAccount(Long id, String username, String email, String passwordHash,
                             Integer status, String fullName, String roleCode) {}

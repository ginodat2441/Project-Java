package com.example.clothingshop.view;

import java.time.LocalDateTime;

public record ManagerView(Long id, String username, String email, String fullName,
                          String phone, String roleCode, Integer status, LocalDateTime createdAt) {}

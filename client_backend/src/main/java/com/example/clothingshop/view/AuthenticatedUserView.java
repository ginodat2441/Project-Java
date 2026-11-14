package com.example.clothingshop.view;

public record AuthenticatedUserView(Long userId, Long customerId, String username, String email,
                                    String fullName, String phone) {}

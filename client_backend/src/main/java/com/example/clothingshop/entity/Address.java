package com.example.clothingshop.entity;

public record Address(Long id, Long customerId, String receiverName, String receiverPhone,
                      String province, String ward, String detailAddress, Integer isDefault) {}

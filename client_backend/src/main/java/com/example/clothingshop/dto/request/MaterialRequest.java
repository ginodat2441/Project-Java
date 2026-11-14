package com.example.clothingshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaterialRequest(@NotBlank @Size(max = 150) String materialName,
                              @Size(max = 1000) String description) {}

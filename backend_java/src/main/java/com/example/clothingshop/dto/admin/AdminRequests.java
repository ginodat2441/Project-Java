package com.example.clothingshop.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AdminRequests {
  public record Color(
      @NotBlank @Size(max = 100) String name,
      @Pattern(regexp = "#([0-9a-fA-F]{6})") String hexCode) {}

  public record Category(
      @NotBlank @Size(max = 150) String name, Long parentId, @Min(0) @Max(1) int status) {}

  public record Manager(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{3,100}") String username,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotBlank @Size(max = 150) String fullName,
      @NotBlank @Email @Size(max = 255) String email,
      @NotNull Long roleId) {}

  public record Access(@NotNull Long roleId, @Min(0) @Max(1) int status) {}

  public record Product(
      @Size(max = 50) String code,
      @NotBlank @Size(max = 255) String name,
      Long brandId,
      Long materialId,
      Long categoryId,
      @Min(0) @Max(2) Integer gender,
      @Size(max = 4000) String description,
      @Size(max = 500) String thumbnailUrl,
      @Min(0) @Max(1) int status,
      @NotEmpty @Valid List<Variant> variants,
      @Size(max = 150) String brandName,
      @Size(max = 150) String materialName) {}

  public record ImageInput(Long colorId, @Min(0) int order, boolean cover) {}

  public record Variant(
      Long id,
      @NotNull Long colorId,
      @NotBlank @Size(max = 20) String size,
      @Size(max = 80) String sku,
      @NotNull @DecimalMin("0") BigDecimal price,
      @Min(0) int quantity,
      @Min(0) @Max(1) int status) {}

  public record Stock(int delta, @NotBlank @Size(max = 500) String reason) {}

  public record Voucher(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,50}") String code,
      @Pattern(regexp = "PERCENT|FIXED") @NotNull String type,
      @NotNull @DecimalMin("0.01") BigDecimal value,
      @NotNull @DecimalMin("0") BigDecimal minOrder,
      @NotNull @DecimalMin("0") BigDecimal maxDiscount,
      @NotNull LocalDateTime startsAt,
      @NotNull LocalDateTime endsAt,
      @Min(0) int usageLimit,
      @Min(0) @Max(1) int status) {}

  public record OrderStatus(@Min(0) @Max(6) int status, @Size(max = 500) String note) {}

  public record Shipping(
      @Pattern(regexp = "COD|BANK") @NotNull String paymentMethod,
      @Min(0) @Max(1) int paymentStatus,
      @Size(max = 150) String carrier,
      @Size(max = 150) String trackingCode) {}
}

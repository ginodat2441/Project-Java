package com.example.clothingshop.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public class InventoryRequests {
  public record Document(
      @NotNull @Pattern(regexp = "RECEIPT|ISSUE|COUNT|RETURN") String type,
      @NotBlank @Size(max = 500) String reason,
      Long billId,
      @NotEmpty @Size(max = 100) @Valid List<Line> lines) {}

  public record Line(
      @NotNull @Positive Long variantId,
      @Min(0) @Max(100000000) int quantity,
      boolean restock,
      @Size(max = 500) String conditionNote,
      @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal unitCost) {}

  public record Note(@NotBlank @Size(max = 500) String reason) {}
}

package com.example.clothingshop.dto.inventory;

import jakarta.validation.constraints.*;
import java.math.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

public final class SkuCostRequests {
  private SkuCostRequests() {}

  public record Snapshot(
      @NotNull @PositiveOrZero Long expectedQuantity,
      @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 8) BigDecimal unitCost,
      @NotBlank @Size(max = 500) String reason,
      @NotBlank @Size(max = 500) String evidence) {}
}

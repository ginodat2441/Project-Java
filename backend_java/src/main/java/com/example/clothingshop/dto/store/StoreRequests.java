package com.example.clothingshop.dto.store;

import com.example.clothingshop.dto.order.BillRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

public final class StoreRequests {
  private StoreRequests() {}

  public record Login(@NotBlank @Email String email, @NotBlank @Size(max = 72) String password) {}

  public record Register(
      @NotBlank @Email String email,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotBlank @Size(max = 150) String name,
      @NotBlank @Size(max = 20) String phone) {}

  public record Checkout(
      @NotBlank @Size(max = 150) String name,
      @NotBlank @Size(max = 20) String phone,
      @NotBlank @Size(max = 100) String province,
      @NotBlank @Size(max = 100) String ward,
      @NotBlank @Size(max = 300) String address,
      @Pattern(regexp = "COD") @NotNull String paymentMethod,
      @Size(max = 50) String voucherCode,
      @Size(max = 500) String note,
      @NotEmpty @Size(max = 100) @Valid List<BillRequest.Item> items,
      @Size(max = 100) String shippingService,
      @NotNull @DecimalMin("0") java.math.BigDecimal expectedShippingFee) {}

  public record Quote(
      @Size(max = 50) String code,
      @NotEmpty @Size(max = 100) @Valid List<BillRequest.Item> items,
      @Size(max = 100) String province,
      @Size(max = 100) String ward,
      @Size(max = 300) String address,
      @Pattern(regexp = "COD") String paymentMethod) {}
}

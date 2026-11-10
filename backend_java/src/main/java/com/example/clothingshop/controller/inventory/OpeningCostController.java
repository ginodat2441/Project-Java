package com.example.clothingshop.controller.inventory;

import com.example.clothingshop.service.admin.AdminService;
import com.example.clothingshop.service.inventory.OpeningCostService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/opening-cost")
public class OpeningCostController {
  private final OpeningCostService costs;
  private final AdminService admin;

  public OpeningCostController(OpeningCostService costs, AdminService admin) {
    this.costs = costs;
    this.admin = admin;
  }

  @GetMapping("/{id}")
  public Object preview(@PathVariable long id) {
    throw new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.GONE,
        "Dùng đối chiếu giá vốn theo ID biến thể tại /api/admin/sku-cost/{id}");
  }

  @PostMapping("/{id}")
  public void save(
      @PathVariable long id, @Valid @RequestBody OpeningCostService.Request r, Authentication a) {
    throw new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.GONE,
        "Không dùng giá vốn chung của mã; dùng đối chiếu giá vốn SKU");
  }
}

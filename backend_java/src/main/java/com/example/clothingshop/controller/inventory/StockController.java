package com.example.clothingshop.controller.inventory;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.service.admin.AdminSearchService;
import com.example.clothingshop.service.admin.AdminService;
import com.example.clothingshop.service.product.ProductMetricsService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class StockController {
  private final AdminService service;
  private final ProductMetricsService metrics;
  private final AdminSearchService search;

  public StockController(
      AdminService service, ProductMetricsService metrics, AdminSearchService search) {
    this.service = service;
    this.metrics = metrics;
    this.search = search;
  }

  @GetMapping("/api/admin/stock")
  public Object stock(
      @RequestParam(defaultValue = "2147483647") int threshold,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) Long productId,
      @RequestParam(required = false) java.math.BigDecimal minValue,
      @RequestParam(required = false) java.math.BigDecimal maxValue,
      @RequestParam(required = false) String stockState) {
    return metrics.stock(q, productId, threshold, page, minValue, maxValue, stockState);
  }

  @PutMapping("/api/admin/stock/{id}")
  public void adjustStock(@PathVariable long id, @Valid @RequestBody Stock r, Authentication a) {
    service.stock(id, r, service.actor(a.getName()));
  }

  @GetMapping("/api/admin/inventory-history")
  public Object history(@RequestParam Map<String, String> filters) {
    return search.history(filters);
  }
}

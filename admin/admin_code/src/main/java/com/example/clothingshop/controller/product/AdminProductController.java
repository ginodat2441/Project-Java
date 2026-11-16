package com.example.clothingshop.controller.product;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.service.admin.AdminService;
import com.example.clothingshop.service.product.AdminProductQueryService;
import com.example.clothingshop.service.product.ProductMetricsService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdminProductController {
  private final AdminService service;
  private final ProductMetricsService metrics;
  private final AdminProductQueryService queries;

  public AdminProductController(
      AdminService service, ProductMetricsService metrics, AdminProductQueryService queries) {
    this.service = service;
    this.metrics = metrics;
    this.queries = queries;
  }

  @GetMapping("/api/admin/lookups")
  public Map<String, Object> lookups() {
    return queries.lookups();
  }

  @PostMapping("/api/admin/colors")
  public Object createColor(@Valid @RequestBody Color r, Authentication a) {
    return service.color(r, service.actor(a.getName()));
  }

  @GetMapping("/api/admin/products")
  public Object products(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Integer status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(required = false) java.time.LocalDate from,
      @RequestParam(required = false) java.time.LocalDate to) {
    return metrics.list(q, categoryId, status, page, from, to);
  }

  @GetMapping("/api/admin/products/{id}")
  public Object product(@PathVariable long id) {
    return service.product(id);
  }

  @PostMapping("/api/admin/products")
  public long createProduct(@Valid @RequestBody Product r, Authentication a) {
    return service.product(null, r, service.actor(a.getName()));
  }

  @PutMapping("/api/admin/products/{id}")
  public long editProduct(@PathVariable long id, @Valid @RequestBody Product r, Authentication a) {
    return service.product(id, r, service.actor(a.getName()));
  }
}

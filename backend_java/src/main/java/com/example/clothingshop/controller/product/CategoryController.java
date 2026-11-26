package com.example.clothingshop.controller.product;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.service.admin.AdminSearchService;
import com.example.clothingshop.service.admin.AdminService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class CategoryController {
  private final AdminService service;
  private final AdminSearchService search;

  public CategoryController(AdminService service, AdminSearchService search) {
    this.service = service;
    this.search = search;
  }

  @GetMapping("/api/admin/categories")
  public Object categories(@RequestParam(defaultValue = "") String q) {
    return search.categories(q);
  }

  @GetMapping("/api/admin/categories/{id}/impact")
  public Object impact(@PathVariable long id) {
    return search.categoryImpact(id);
  }

  @PostMapping("/api/admin/categories")
  public long createCategory(@Valid @RequestBody Category r, Authentication a) {
    return service.category(null, r, service.actor(a.getName()));
  }

  @PutMapping("/api/admin/categories/{id}")
  public long editCategory(
      @PathVariable long id, @Valid @RequestBody Category r, Authentication a) {
    return service.category(id, r, service.actor(a.getName()));
  }
}

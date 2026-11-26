package com.example.clothingshop.controller.inventory;

import com.example.clothingshop.dto.inventory.SkuCostRequests.*;
import com.example.clothingshop.service.inventory.SkuCostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sku-cost")
public class SkuCostController {
  private final SkuCostService service;

  public SkuCostController(SkuCostService service) {
    this.service = service;
  }

  @GetMapping("/{id}")
  public Object preview(@PathVariable long id) {
    return service.preview(id);
  }

  @PostMapping("/{id}")
  public void snapshot(@PathVariable long id, @Valid @RequestBody Snapshot r, Authentication a) {
    service.snapshot(id, r, a);
  }
}

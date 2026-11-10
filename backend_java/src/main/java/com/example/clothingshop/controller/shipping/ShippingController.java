package com.example.clothingshop.controller.shipping;

import com.example.clothingshop.service.shipping.ShippingQuoteService;
import com.example.clothingshop.util.shipping.ProvinceCatalog;
import org.springframework.web.bind.annotation.*;

@RestController
public class ShippingController {
  private final ShippingQuoteService service;

  public ShippingController(ShippingQuoteService service) {
    this.service = service;
  }

  @GetMapping("/api/shipping/provinces")
  public Object provinces() {
    return ProvinceCatalog.ALL;
  }

  @GetMapping("/api/shipping/status")
  public Object status() {
    return service.status();
  }

  @GetMapping("/api/admin/shipping/status")
  public Object adminStatus() {
    return service.status();
  }
}

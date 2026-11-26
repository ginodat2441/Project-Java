package com.example.clothingshop.controller.shipping;

import com.example.clothingshop.service.shipping.ShippingQuoteService;
import com.example.clothingshop.util.shipping.ProvinceCatalog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    return service.adminStatus();
  }

  @GetMapping("/api/admin/shipping/wards")
  public java.util.List<String> wards(@RequestParam String province) {
    return service.wards(province);
  }

  @PostMapping("/api/admin/shipping/test-quote")
  public Object testQuote(@Valid @RequestBody TestQuote request) {
    var offers = service.testQuote(request.province(), request.ward(), request.address());
    return java.util.Map.of("services", offers, "status", service.adminStatus());
  }

  public record TestQuote(
      @NotBlank @Size(max = 100) String province,
      @NotBlank @Size(max = 100) String ward,
      @NotBlank @Size(max = 300) String address) {}
}

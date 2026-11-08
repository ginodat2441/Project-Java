package com.example.clothingshop.controller.store;

import com.example.clothingshop.dto.store.StoreRequests.*;
import com.example.clothingshop.service.store.StoreService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class StoreController {
  private final StoreService service;

  public StoreController(StoreService service) {
    this.service = service;
  }

  @PostMapping("/api/customer/login")
  public Object login(
      @Valid @RequestBody Login r,
      HttpSession session,
      jakarta.servlet.http.HttpServletRequest request) {
    return service.login(r, session, request);
  }

  @PostMapping("/api/customer/register")
  public Object register(
      @Valid @RequestBody Register r,
      HttpSession session,
      jakarta.servlet.http.HttpServletRequest request) {
    return service.register(r, session, request);
  }

  @GetMapping("/api/customer/me")
  public Object me(HttpSession session) {
    return service.me(session);
  }

  @PostMapping("/api/customer/logout")
  public void logout(HttpSession s) {
    service.logout(s);
  }

  @PostMapping("/api/checkout")
  public Object checkout(@Valid @RequestBody Checkout r, HttpSession session) {
    return service.checkout(r, session);
  }

  @GetMapping("/api/catalog")
  public Object catalog(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(defaultValue = "1") int page) {
    return service.catalog(q, categoryId, page);
  }

  @GetMapping("/api/catalog/categories")
  public Object categories() {
    return service.categories();
  }

  @GetMapping("/api/catalog/{id}")
  public Object product(@PathVariable long id) {
    return service.product(id);
  }

  @PostMapping("/api/catalog/quote")
  public Object quote(@Valid @RequestBody Quote r) {
    return service.quote(r);
  }
}

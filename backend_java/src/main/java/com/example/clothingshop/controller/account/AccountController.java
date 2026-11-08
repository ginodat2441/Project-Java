package com.example.clothingshop.controller.account;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.service.account.AccountQueryService;
import com.example.clothingshop.service.admin.AdminSearchService;
import com.example.clothingshop.service.admin.AdminService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccountController {
  private final AdminService service;
  private final AdminSearchService search;
  private final AccountQueryService queries;

  public AccountController(
      AdminService service, AdminSearchService search, AccountQueryService queries) {
    this.service = service;
    this.search = search;
    this.queries = queries;
  }

  @GetMapping("/api/admin/roles")
  public Object roles() {
    return queries.roles();
  }

  @GetMapping("/api/admin/managers")
  public Object managers(@RequestParam Map<String, String> filters) {
    return search.managers(filters);
  }

  @PostMapping("/api/admin/managers")
  public long createManager(@Valid @RequestBody Manager r, Authentication a) {
    return service.manager(r, service.actor(a.getName()));
  }

  @PutMapping("/api/admin/managers/{id}/access")
  public void access(@PathVariable long id, @Valid @RequestBody Access r, Authentication a) {
    service.access(id, r, service.actor(a.getName()));
  }

  @GetMapping("/api/admin/customers")
  public Object customers(
      @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "1") int page) {
    return queries.customers(q, page);
  }

  @GetMapping("/api/admin/customers/{id}")
  public Object customer(@PathVariable long id) {
    return queries.customer(id);
  }

  @PutMapping("/api/admin/customers/{id}/status")
  public void customerStatus(@PathVariable long id, @RequestParam int status, Authentication a) {
    service.customer(id, status, service.actor(a.getName()));
  }
}

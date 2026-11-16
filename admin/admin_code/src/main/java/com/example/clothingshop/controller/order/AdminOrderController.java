package com.example.clothingshop.controller.order;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.service.admin.AdminSearchService;
import com.example.clothingshop.service.admin.AdminService;
import com.example.clothingshop.service.order.AdminOrderQueryService;
import com.example.clothingshop.service.order.BillService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdminOrderController {
  private final AdminService service;
  private final BillService bills;
  private final AdminSearchService search;
  private final AdminOrderQueryService queries;

  public AdminOrderController(
      AdminService service,
      BillService bills,
      AdminSearchService search,
      AdminOrderQueryService queries) {
    this.service = service;
    this.bills = bills;
    this.search = search;
    this.queries = queries;
  }

  @GetMapping("/api/admin/orders")
  public Object orders(@RequestParam Map<String, String> filters) {
    return search.orders(filters);
  }

  @GetMapping("/api/admin/orders/{id}")
  public Object order(@PathVariable long id) {
    return queries.order(id);
  }

  @PutMapping("/api/admin/orders/{id}/status")
  public Object orderStatus(
      @PathVariable long id, @Valid @RequestBody OrderStatus r, Authentication a) {
    return bills.changeStatus(id, r.status(), service.actor(a.getName()), r.note());
  }

  @PutMapping("/api/admin/orders/{id}/shipping")
  public void shipping(@PathVariable long id, @Valid @RequestBody Shipping r, Authentication a) {
    bills.shipping(id, r, service.actor(a.getName()));
  }

  @PostMapping("/api/admin/orders/{id}/payment-failed")
  public Object paymentFailed(@PathVariable long id, Authentication a) {
    return bills.changeStatus(id, 4, service.actor(a.getName()), "Thanh toán thất bại");
  }
}

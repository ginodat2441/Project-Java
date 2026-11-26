package com.example.clothingshop.controller.order;

import com.example.clothingshop.service.admin.AdminService;
import com.example.clothingshop.service.inventory.InventoryService;
import com.example.clothingshop.service.order.RefundQueryService;
import com.example.clothingshop.service.order.RefundService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class WorkflowController {
  private final InventoryService inventory;
  private final AdminService admin;
  private final RefundService refunds;
  private final RefundQueryService queries;

  public WorkflowController(
      InventoryService i, AdminService a, RefundService r, RefundQueryService queries) {
    inventory = i;
    admin = a;
    refunds = r;
    this.queries = queries;
  }

  @PostMapping("/api/admin/orders/{id}/dispatch")
  public long dispatch(@PathVariable long id, Authentication a) {
    return inventory.prepareDispatch(id, admin.actor(a.getName()));
  }

  @GetMapping("/api/admin/orders/{id}/refunds")
  public Object refunds(@PathVariable long id) {
    return queries.refunds(id);
  }

  @PostMapping("/api/admin/orders/{id}/refunds")
  public long refund(
      @PathVariable long id, @Valid @RequestBody RefundService.Request r, Authentication a) {
    return refunds.save(id, null, r, admin.actor(a.getName()));
  }

  @PutMapping("/api/admin/orders/{id}/refunds/{refund}")
  public long update(
      @PathVariable long id,
      @PathVariable long refund,
      @Valid @RequestBody RefundService.Request r,
      Authentication a) {
    return refunds.save(id, refund, r, admin.actor(a.getName()));
  }
}

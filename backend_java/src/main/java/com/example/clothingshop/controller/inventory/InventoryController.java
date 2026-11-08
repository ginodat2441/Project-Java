package com.example.clothingshop.controller.inventory;

import static com.example.clothingshop.dto.inventory.InventoryRequests.*;

import com.example.clothingshop.service.admin.AdminService;
import com.example.clothingshop.service.inventory.InventoryQueryService;
import com.example.clothingshop.service.inventory.InventoryReportService;
import com.example.clothingshop.service.inventory.InventoryService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inventory")
public class InventoryController {
  private final InventoryService service;
  private final AdminService admin;
  private final InventoryQueryService queries;
  private final InventoryReportService summary;

  public InventoryController(
      InventoryService service,
      AdminService admin,
      InventoryQueryService queries,
      InventoryReportService summary) {
    this.service = service;
    this.admin = admin;
    this.queries = queries;
    this.summary = summary;
  }

  @GetMapping("/order-issues")
  public Object orderIssues(@RequestParam(defaultValue = "") String q) {
    return queries.orderIssues(q);
  }

  @GetMapping("/variants")
  public Object variants(@RequestParam(defaultValue = "") String q) {
    return queries.variants(q);
  }

  @GetMapping("/documents")
  public Object list(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int page) {
    return queries.list(q, type, status, page);
  }

  @GetMapping("/documents/{id}")
  public Object detail(@PathVariable long id) {
    return queries.detail(id);
  }

  @PostMapping("/documents")
  public long create(@Valid @RequestBody Document r, Authentication a) {
    return service.save(null, r, admin.actor(a.getName()));
  }

  @PutMapping("/documents/{id}")
  public long edit(@PathVariable long id, @Valid @RequestBody Document r, Authentication a) {
    return service.save(id, r, admin.actor(a.getName()));
  }

  @PostMapping("/documents/{id}/approve")
  public void approve(@PathVariable long id, Authentication a) {
    service.approve(id, admin.actor(a.getName()));
  }

  @PostMapping("/documents/{id}/cancel")
  public void cancel(@PathVariable long id, @Valid @RequestBody Note r, Authentication a) {
    service.cancel(id, r.reason(), admin.actor(a.getName()));
  }

  @PostMapping("/documents/{id}/reverse")
  public long reverse(@PathVariable long id, @Valid @RequestBody Note r, Authentication a) {
    return service.reverse(id, r.reason(), admin.actor(a.getName()));
  }

  @GetMapping("/summary")
  public Object summary(
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "1") int page) {
    return summary.report(from, to, q, page);
  }
}

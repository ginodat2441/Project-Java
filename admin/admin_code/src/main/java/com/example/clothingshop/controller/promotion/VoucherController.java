package com.example.clothingshop.controller.promotion;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.service.admin.AdminSearchService;
import com.example.clothingshop.service.admin.AdminService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class VoucherController {
  private final AdminService service;
  private final AdminSearchService search;

  public VoucherController(AdminService service, AdminSearchService search) {
    this.service = service;
    this.search = search;
  }

  @GetMapping("/api/admin/vouchers/{id}/orders")
  public Object voucherOrders(@PathVariable long id) {
    return search.voucherOrders(id);
  }

  @GetMapping("/api/admin/vouchers")
  public Object vouchers(@RequestParam Map<String, String> filters) {
    return search.vouchers(filters);
  }

  @PostMapping("/api/admin/vouchers")
  public long createVoucher(@Valid @RequestBody Voucher r, Authentication a) {
    return service.voucher(null, r, service.actor(a.getName()));
  }

  @PutMapping("/api/admin/vouchers/{id}")
  public long editVoucher(@PathVariable long id, @Valid @RequestBody Voucher r, Authentication a) {
    return service.voucher(id, r, service.actor(a.getName()));
  }
}

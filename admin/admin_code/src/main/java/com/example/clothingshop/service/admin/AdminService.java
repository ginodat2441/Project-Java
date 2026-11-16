package com.example.clothingshop.service.admin;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.common.*;
import com.example.clothingshop.service.account.CustomerService;
import com.example.clothingshop.service.account.ManagerService;
import com.example.clothingshop.service.auth.AdminActorService;
import com.example.clothingshop.service.inventory.InventoryMovementService;
import com.example.clothingshop.service.product.CategoryService;
import com.example.clothingshop.service.product.ColorService;
import com.example.clothingshop.service.product.ProductCommandService;
import com.example.clothingshop.service.promotion.VoucherService;
import java.util.*;
import org.springframework.stereotype.Service;

/** Compatibility facade; business rules are implemented in feature services. */
@Service
public class AdminService {
  private final AdminActorService adminActorService;
  private final ColorService colorService;
  private final CategoryService categoryService;
  private final ManagerService managerService;
  private final CustomerService customerService;
  private final ProductCommandService productCommandService;
  private final InventoryMovementService inventoryMovementService;
  private final VoucherService voucherService;

  public AdminService(
      AdminActorService adminActorService,
      ColorService colorService,
      CategoryService categoryService,
      ManagerService managerService,
      CustomerService customerService,
      ProductCommandService productCommandService,
      InventoryMovementService inventoryMovementService,
      VoucherService voucherService) {
    this.adminActorService = adminActorService;
    this.colorService = colorService;
    this.categoryService = categoryService;
    this.managerService = managerService;
    this.customerService = customerService;
    this.productCommandService = productCommandService;
    this.inventoryMovementService = inventoryMovementService;
    this.voucherService = voucherService;
  }

  public long actor(String username) {
    return adminActorService.actor(username);
  }

  public Map<String, Object> color(Color r, long actor) {
    return colorService.color(r, actor);
  }

  public long category(Long id, Category r, long actor) {
    return categoryService.category(id, r, actor);
  }

  public long manager(Manager r, long actor) {
    return managerService.manager(r, actor);
  }

  public void access(long id, Access r, long actor) {
    managerService.access(id, r, actor);
  }

  public void customer(long id, int status, long actor) {
    customerService.customer(id, status, actor);
  }

  public Map<String, Object> product(long id) {
    return productCommandService.product(id);
  }

  public long product(Long id, Product r, long actor) {
    return productCommandService.product(id, r, actor);
  }

  public void movement(long variant, int delta, String reason, Long actor, Long bill) {
    inventoryMovementService.movement(variant, delta, reason, actor, bill);
  }

  public void stock(long id, Stock r, long actor) {
    inventoryMovementService.stock(id, r, actor);
  }

  public long voucher(Long id, Voucher r, long actor) {
    return voucherService.voucher(id, r, actor);
  }
}

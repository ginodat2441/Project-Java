package com.example.clothingshop.controller.store;

import com.example.clothingshop.dto.store.CustomerRequests.*;
import com.example.clothingshop.service.store.CustomerAccountService;
import com.example.clothingshop.service.store.CustomerCartService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
public class CustomerClientController {
  private final CustomerCartService carts;
  private final CustomerAccountService accounts;
  public CustomerClientController(CustomerCartService carts, CustomerAccountService accounts) {
    this.carts = carts; this.accounts = accounts;
  }
  @GetMapping("/cart") public Object cart(HttpSession s) { return carts.get(s); }
  @PostMapping("/cart/items") public Object add(@Valid @RequestBody CartItem r, HttpSession s) { return carts.add(s, r.productDetailId(), r.quantity()); }
  @PutMapping("/cart/items/{id}") public Object quantity(@PathVariable long id, @Valid @RequestBody Quantity r, HttpSession s) { return carts.update(s, id, r.quantity()); }
  @DeleteMapping("/cart/items/{id}") public Object remove(@PathVariable long id, HttpSession s) { return carts.remove(s, id); }
  @DeleteMapping("/cart") public Object clear(HttpSession s) { return carts.clear(s); }
  @PostMapping("/cart/checkout") public Object checkout(@Valid @RequestBody com.example.clothingshop.dto.store.StoreRequests.Checkout r, HttpSession s) { return carts.checkout(s, r); }
  @PutMapping("/profile") public Object profile(@Valid @RequestBody Profile r, HttpSession s) { return accounts.profile(s, r); }
  @PutMapping("/password") public void password(@Valid @RequestBody Password r, HttpSession s) { accounts.password(s, r); }
  @GetMapping("/addresses") public Object addresses(HttpSession s) { return accounts.addresses(s); }
  @PostMapping("/addresses") public Object address(@Valid @RequestBody Address r, HttpSession s) { return accounts.address(s, null, r); }
  @PutMapping("/addresses/{id}") public Object address(@PathVariable long id, @Valid @RequestBody Address r, HttpSession s) { return accounts.address(s, id, r); }
  @DeleteMapping("/addresses/{id}") public Object removeAddress(@PathVariable long id, HttpSession s) { return accounts.removeAddress(s, id); }
  @PostMapping("/addresses/{id}/default") public Object defaultAddress(@PathVariable long id, HttpSession s) { return accounts.defaultAddress(s, id); }
  @GetMapping("/orders") public Object orders(HttpSession s) { return accounts.orders(s); }
  @GetMapping("/orders/{id}") public Object order(@PathVariable long id, HttpSession s) { return accounts.order(s, id); }
}

package com.example.clothingshop.admin;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import java.util.*;
@RestController
public class ShippingController {
 private final ShippingRateService shipping;private final AdminService admin;
 public ShippingController(ShippingRateService s,AdminService a){shipping=s;admin=a;}
 @GetMapping("/api/shipping/provinces") public Object provinces(){return shipping.list();}
 @GetMapping("/api/admin/shipping/rates") public Object rates(){return Map.of("mode","SHOP_RATE_TABLE","description","Phí do shop quy định; không phải báo giá trực tiếp của hãng vận chuyển", "items",shipping.list());}
 @PutMapping("/api/admin/shipping/rates/{code}") public void update(@PathVariable String code,@Valid @RequestBody ShippingRateService.Update r,Authentication a){shipping.update(code,r,admin.actor(a.getName()));}
}

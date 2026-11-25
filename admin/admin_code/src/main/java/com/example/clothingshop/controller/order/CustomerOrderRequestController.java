package com.example.clothingshop.controller.order;
import com.example.clothingshop.service.order.CustomerOrderRequestService;
import com.example.clothingshop.service.admin.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/order-requests")
public class CustomerOrderRequestController {
    private final CustomerOrderRequestService service;private final AdminService admin;
    public CustomerOrderRequestController(CustomerOrderRequestService service,AdminService admin){this.service=service;this.admin=admin;}
    @GetMapping public Object pending(){return service.pending();}
    @PutMapping("/{id}") public void review(@PathVariable long id,@Valid @RequestBody CustomerOrderRequestService.Review review,Authentication auth){service.review(id,review,admin.actor(auth.getName()));}
}

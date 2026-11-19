package com.example.clothingshop.controller;
import com.example.clothingshop.common.*;
import com.example.clothingshop.service.OrderRequestService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order-requests")
public class OrderRequestController {
    private final OrderRequestService service;
    public OrderRequestController(OrderRequestService service){this.service=service;}
    @GetMapping public Object mine(@AuthenticationPrincipal Jwt jwt){return ApiResponse.ok(service.overview(CustomerIdentity.customerId(jwt)));}
    @PostMapping public Object create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody OrderRequestService.Request request){
        return ApiResponse.ok("Yêu cầu đã được gửi đến cửa hàng. Đơn hàng chỉ thay đổi sau khi được duyệt.",service.create(CustomerIdentity.customerId(jwt),request));
    }
}

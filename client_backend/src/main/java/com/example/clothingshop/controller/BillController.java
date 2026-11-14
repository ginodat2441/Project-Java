package com.example.clothingshop.controller;

import com.example.clothingshop.dto.request.BillRequest;
import com.example.clothingshop.service.BillService;
import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.CustomerIdentity;
import com.example.clothingshop.view.BillHistoryView;
import com.example.clothingshop.view.BillLineView;
import com.example.clothingshop.view.BillView;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService service;

    public BillController(BillService service) { this.service = service; }

    @PostMapping
    public ApiResponse<BillView> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BillRequest req) {
        return ApiResponse.ok("Đặt hàng thành công",
                service.create(CustomerIdentity.customerId(jwt), req));
    }

    @PostMapping("/{id}/sepay-payment")
    public ApiResponse<BillView> createSePayPayment(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable long id) {
        long customerId = CustomerIdentity.customerId(jwt);
        service.createSePayPayment(customerId, id);
        return ApiResponse.ok("Đã chuẩn bị thanh toán SePay",
                service.get(customerId, id));
    }

    @GetMapping
    public ApiResponse<List<BillView>> mine(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(service.byCustomer(CustomerIdentity.customerId(jwt)));
    }

    @GetMapping("/{id}")
    public ApiResponse<BillView> get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return ApiResponse.ok(service.get(CustomerIdentity.customerId(jwt), id));
    }

    @GetMapping("/{id}/lines")
    public ApiResponse<List<BillLineView>> lines(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return ApiResponse.ok(service.lines(CustomerIdentity.customerId(jwt), id));
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<BillHistoryView>> history(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return ApiResponse.ok(service.history(CustomerIdentity.customerId(jwt), id));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<BillView> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return ApiResponse.ok("Đã hủy đơn hàng",
                service.cancel(CustomerIdentity.customerId(jwt), id));
    }
}

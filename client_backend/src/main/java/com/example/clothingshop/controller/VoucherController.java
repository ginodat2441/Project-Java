package com.example.clothingshop.controller;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.CustomerIdentity;
import com.example.clothingshop.service.BillService;
import com.example.clothingshop.view.CustomerVoucherView;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {
    private final BillService service;

    public VoucherController(BillService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<CustomerVoucherView>> available(
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(service.availableVouchers(CustomerIdentity.customerId(jwt)));
    }
}

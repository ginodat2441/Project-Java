package com.example.clothingshop.controller;

import com.example.clothingshop.dto.request.SePayIpnRequest;
import com.example.clothingshop.service.BillService;
import com.example.clothingshop.service.SePayService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SePayController {

    private final BillService billService;
    private final SePayService sePayService;

    public SePayController(BillService billService, SePayService sePayService) {
        this.billService = billService;
        this.sePayService = sePayService;
    }

    @PostMapping({"/api/payment", "/api/payments/sepay/ipn"})
    public ResponseEntity<Map<String, Object>> ipn(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) SePayIpnRequest request) {
        if (!sePayService.isApiKeyConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("success", false, "message", "SePay API key is not configured"));
        }
        if (!sePayService.isValidAuthorization(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid SePay API key"));
        }
        if (!sePayService.isValidPaymentNotification(request)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid SePay payment notification"));
        }
        if (!billService.confirmSePayPayment(request)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "SePay payment does not match a pending bill"));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}

package com.example.clothingshop.controller;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.CustomerIdentity;
import com.example.clothingshop.dto.request.AddressRequest;
import com.example.clothingshop.service.AddressService;
import com.example.clothingshop.view.AddressView;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService service;

    public AddressController(AddressService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AddressView>> list(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(service.list(CustomerIdentity.customerId(jwt)));
    }

    @GetMapping("/{id}")
    public ApiResponse<AddressView> get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return ApiResponse.ok(service.get(CustomerIdentity.customerId(jwt), id));
    }

    @PostMapping
    public ApiResponse<AddressView> create(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok("Tạo địa chỉ thành công",
                service.create(CustomerIdentity.customerId(jwt), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressView> update(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                           @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok("Cập nhật địa chỉ thành công",
                service.update(CustomerIdentity.customerId(jwt), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        service.delete(CustomerIdentity.customerId(jwt), id);
        return ApiResponse.ok("Đã xóa địa chỉ", null);
    }
}

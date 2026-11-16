package com.example.clothingshop.controller;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.CustomerIdentity;
import com.example.clothingshop.dto.request.CartCheckoutRequest;
import com.example.clothingshop.dto.request.CartItemRequest;
import com.example.clothingshop.dto.request.CartItemUpdateRequest;
import com.example.clothingshop.service.CartService;
import com.example.clothingshop.view.BillView;
import com.example.clothingshop.view.CartView;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Giữ URL theo customerId để tương thích, nhưng ownership luôn được đối chiếu với Bearer token.
 */
@RestController
@RequestMapping("/api/carts/customer/{customerId}")
public class CartController {

    private final CartService service;

    public CartController(CartService service) { this.service = service; }

    @GetMapping
    public ApiResponse<CartView> get(@AuthenticationPrincipal Jwt jwt, @PathVariable long customerId) {
        return ApiResponse.ok(service.get(CustomerIdentity.requireCustomer(jwt, customerId)));
    }

    @PostMapping("/items")
    public ApiResponse<CartView> addItem(@AuthenticationPrincipal Jwt jwt, @PathVariable long customerId,
                                         @Valid @RequestBody CartItemRequest req) {
        return ApiResponse.ok("Đã thêm vào giỏ",
                service.addItem(CustomerIdentity.requireCustomer(jwt, customerId), req));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<CartView> updateItem(@AuthenticationPrincipal Jwt jwt, @PathVariable long customerId,
                                            @PathVariable long itemId,
                                            @Valid @RequestBody CartItemUpdateRequest req) {
        return ApiResponse.ok("Đã cập nhật số lượng",
                service.updateItem(CustomerIdentity.requireCustomer(jwt, customerId), itemId, req));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartView> removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable long customerId,
                                            @PathVariable long itemId) {
        return ApiResponse.ok("Đã xóa khỏi giỏ",
                service.removeItem(CustomerIdentity.requireCustomer(jwt, customerId), itemId));
    }

    @DeleteMapping("/items")
    public ApiResponse<CartView> clear(@AuthenticationPrincipal Jwt jwt, @PathVariable long customerId) {
        return ApiResponse.ok("Đã làm trống giỏ",
                service.clear(CustomerIdentity.requireCustomer(jwt, customerId)));
    }

    @PostMapping("/checkout")
    public ApiResponse<BillView> checkout(@AuthenticationPrincipal Jwt jwt, @PathVariable long customerId,
                                          @Valid @RequestBody CartCheckoutRequest req) {
        return ApiResponse.ok("Đặt hàng thành công",
                service.checkout(CustomerIdentity.requireCustomer(jwt, customerId), req));
    }

    @PostMapping("/checkout/sepay")
    public ApiResponse<BillView> checkoutSePay(@AuthenticationPrincipal Jwt jwt,
                                               @PathVariable long customerId,
                                               @Valid @RequestBody CartCheckoutRequest req) {
        return ApiResponse.ok("Đã tạo đơn hàng, đang chuyển đến SePay",
                service.checkoutSePay(CustomerIdentity.requireCustomer(jwt, customerId), req));
    }
}

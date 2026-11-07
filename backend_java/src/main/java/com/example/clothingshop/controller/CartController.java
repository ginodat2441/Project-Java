package com.example.clothingshop.controller;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.dto.request.CartCheckoutRequest;
import com.example.clothingshop.dto.request.CartItemRequest;
import com.example.clothingshop.dto.request.CartItemUpdateRequest;
import com.example.clothingshop.service.CartService;
import com.example.clothingshop.view.BillView;
import com.example.clothingshop.view.CartView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Giỏ hàng theo khách: /api/carts/customer/{customerId}/...
 * (Chưa có đăng nhập nên customerId truyền trên URL; khi có Security thì lấy từ token.)
 */
@RestController
@RequestMapping("/api/carts/customer/{customerId}")
public class CartController {

    private final CartService service;

    public CartController(CartService service) { this.service = service; }

    @GetMapping
    public ApiResponse<CartView> get(@PathVariable long customerId) {
        return ApiResponse.ok(service.get(customerId));
    }

    @PostMapping("/items")
    public ApiResponse<CartView> addItem(@PathVariable long customerId,
                                         @Valid @RequestBody CartItemRequest req) {
        return ApiResponse.ok("Đã thêm vào giỏ", service.addItem(customerId, req));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<CartView> updateItem(@PathVariable long customerId, @PathVariable long itemId,
                                            @Valid @RequestBody CartItemUpdateRequest req) {
        return ApiResponse.ok("Đã cập nhật số lượng", service.updateItem(customerId, itemId, req));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartView> removeItem(@PathVariable long customerId, @PathVariable long itemId) {
        return ApiResponse.ok("Đã xóa khỏi giỏ", service.removeItem(customerId, itemId));
    }

    @DeleteMapping("/items")
    public ApiResponse<CartView> clear(@PathVariable long customerId) {
        return ApiResponse.ok("Đã làm trống giỏ", service.clear(customerId));
    }

    @PostMapping("/checkout")
    public ApiResponse<BillView> checkout(@PathVariable long customerId,
                                          @Valid @RequestBody CartCheckoutRequest req) {
        return ApiResponse.ok("Đặt hàng thành công", service.checkout(customerId, req));
    }
}

package com.example.clothingshop.view;

import com.example.clothingshop.entity.Cart;

import java.math.BigDecimal;
import java.util.List;

/** Giỏ hàng trả ra API: thông tin giỏ + danh sách dòng + tổng. */
public record CartView(Long id, Long customerId, List<CartItemView> items,
                       int totalItems, BigDecimal totalAmount) {

    public static CartView of(Cart cart, List<CartItemView> items) {
        int count = items.stream().mapToInt(CartItemView::quantity).sum();
        BigDecimal total = items.stream().map(CartItemView::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartView(cart.id(), cart.customerId(), items, count, total);
    }

    /** Khách chưa có giỏ nào đang dùng. */
    public static CartView empty(long customerId) {
        return new CartView(null, customerId, List.of(), 0, BigDecimal.ZERO);
    }
}

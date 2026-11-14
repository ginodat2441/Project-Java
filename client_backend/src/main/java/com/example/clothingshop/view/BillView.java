package com.example.clothingshop.view;

import com.example.clothingshop.entity.Bill;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** View = dữ liệu trả ra API cho hóa đơn (ẩn DELETE_FLAG, HANDLED_BY, ...). */
public record BillView(Long id, String billCode, Long customerId, String receiverName,
                       String receiverPhone, String shippingAddress, BigDecimal subTotal,
                       BigDecimal shippingFee, BigDecimal discountAmount, BigDecimal totalAmount,
                       String paymentMethod, Integer paymentStatus, Integer status,
                       LocalDateTime createdAt) {

    public static BillView from(Bill b) {
        return new BillView(b.id(), b.billCode(), b.customerId(), b.receiverName(),
                b.receiverPhone(), b.shippingAddress(), b.subTotal(), b.shippingFee(),
                b.discountAmount(), b.totalAmount(), b.paymentMethod(), b.paymentStatus(),
                b.status(), b.createdAt());
    }
}

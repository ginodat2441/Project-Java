package com.example.clothingshop.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerVoucherView(
        Long id,
        String code,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minOrder,
        BigDecimal maxDiscount,
        LocalDateTime startsAt,
        LocalDateTime endsAt) {}

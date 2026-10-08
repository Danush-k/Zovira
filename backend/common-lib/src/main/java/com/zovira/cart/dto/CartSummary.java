package com.zovira.cart.dto;

import java.math.BigDecimal;

public record CartSummary(
        int itemCount,
        BigDecimal mrpTotal,
        BigDecimal subtotal,
        BigDecimal itemDiscount,
        String couponCode,
        boolean couponApplied,
        BigDecimal couponDiscount,
        String couponMessage,
        String deliveryOption,
        BigDecimal shippingFee,
        BigDecimal freeShippingThreshold,
        BigDecimal amountToFreeShipping,
        BigDecimal taxIncluded,
        BigDecimal total,
        boolean hasIssues) {
}

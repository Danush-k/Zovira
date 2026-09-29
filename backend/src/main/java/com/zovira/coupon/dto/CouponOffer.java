package com.zovira.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** A coupon shown to shoppers, with whether it applies to their current cart and the saving. */
public record CouponOffer(
        String code,
        String description,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minOrderValue,
        BigDecimal maxDiscount,
        Instant expiresAt,
        boolean applicable,
        BigDecimal estimatedSaving,
        String message) {
}

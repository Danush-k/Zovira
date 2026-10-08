package com.zovira.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminCouponRow(
        Long id,
        String code,
        String description,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minOrderValue,
        BigDecimal maxDiscount,
        Instant startsAt,
        Instant expiresAt,
        Integer usageLimit,
        int perUserLimit,
        int usedCount,
        String scope,
        boolean userSpecific,
        boolean active,
        boolean live,
        BigDecimal totalDiscountGiven,
        List<String> categories,
        List<String> products) {
}

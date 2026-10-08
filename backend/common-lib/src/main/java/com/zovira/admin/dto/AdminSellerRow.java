package com.zovira.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminSellerRow(
        Long id,
        String storeName,
        String slug,
        String ownerName,
        String ownerEmail,
        String gstin,
        String city,
        String state,
        String status,
        String statusReason,
        BigDecimal ratingAverage,
        int ratingCount,
        long productCount,
        BigDecimal lifetimeRevenue,
        Instant appliedAt,
        Instant approvedAt) {
}

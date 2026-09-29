package com.zovira.seller.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerProfileResponse(
        Long id,
        String storeName,
        String slug,
        String description,
        String logoUrl,
        String gstin,
        String supportEmail,
        String supportPhone,
        String pickupLine1,
        String pickupCity,
        String pickupState,
        String pickupPincode,
        String status,
        String statusReason,
        BigDecimal ratingAverage,
        int ratingCount,
        Instant approvedAt,
        Instant createdAt) {
}

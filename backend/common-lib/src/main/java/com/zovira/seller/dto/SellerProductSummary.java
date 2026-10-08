package com.zovira.seller.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerProductSummary(
        Long id,
        String slug,
        String title,
        String imageUrl,
        String categoryName,
        String brandName,
        String status,
        BigDecimal price,
        BigDecimal mrp,
        int totalStock,
        int variantCount,
        boolean lowStock,
        BigDecimal ratingAverage,
        int ratingCount,
        int soldCount,
        Instant updatedAt) {
}

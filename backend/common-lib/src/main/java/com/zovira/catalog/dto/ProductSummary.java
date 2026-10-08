package com.zovira.catalog.dto;

import java.math.BigDecimal;

/** Compact product representation used by cards, rails and listings. */
public record ProductSummary(
        Long id,
        String slug,
        String title,
        String brand,
        String imageUrl,
        BigDecimal price,
        BigDecimal mrp,
        int discountPercent,
        BigDecimal ratingAverage,
        int ratingCount,
        boolean inStock,
        boolean has3dModel,
        Long defaultVariantId,
        int variantCount) {
}

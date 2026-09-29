package com.zovira.wishlist.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WishlistItemResponse(
        Long productId,
        String slug,
        String title,
        String brand,
        String imageUrl,
        BigDecimal price,
        BigDecimal mrp,
        int discountPercent,
        BigDecimal priceAtAdd,
        BigDecimal priceDrop,
        boolean inStock,
        boolean available,
        Long defaultVariantId,
        int variantCount,
        Instant addedAt) {
}

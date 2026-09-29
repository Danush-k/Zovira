package com.zovira.seller.dto;

import java.math.BigDecimal;
import java.util.Map;

public record InventoryRow(
        Long variantId,
        Long productId,
        String productTitle,
        String productSlug,
        String imageUrl,
        String variantName,
        Map<String, String> options,
        String sku,
        BigDecimal price,
        int available,
        int reserved,
        int lowStockThreshold,
        boolean active) {
}

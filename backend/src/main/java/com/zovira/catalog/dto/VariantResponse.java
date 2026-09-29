package com.zovira.catalog.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * A purchasable variant. {@code stockStatus} is IN_STOCK, LOW_STOCK or OUT_OF_STOCK; the exact
 * quantity is only disclosed through {@code maxPurchasable}, which is capped by the per-order limit.
 */
public record VariantResponse(
        Long id,
        String sku,
        String name,
        Map<String, String> options,
        BigDecimal price,
        BigDecimal mrp,
        int discountPercent,
        String stockStatus,
        int maxPurchasable,
        Integer lowStockQuantity,
        boolean isDefault) {

    public VariantResponse withStock(String stockStatus, int maxPurchasable, Integer lowStockQuantity) {
        return new VariantResponse(id, sku, name, options, price, mrp, discountPercent, stockStatus, maxPurchasable,
                lowStockQuantity, isDefault);
    }
}

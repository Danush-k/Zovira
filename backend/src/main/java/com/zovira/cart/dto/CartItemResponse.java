package com.zovira.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CartItemResponse(
        Long id,
        Long productId,
        String slug,
        String title,
        String brand,
        String imageUrl,
        Long variantId,
        String variantName,
        Map<String, String> options,
        String sku,
        int quantity,
        int maxQuantity,
        BigDecimal unitPrice,
        BigDecimal unitMrp,
        BigDecimal lineTotal,
        BigDecimal priceAtAdd,
        String stockStatus,
        boolean purchasable,
        List<String> issues,
        String sellerName,
        boolean codAvailable) {
}

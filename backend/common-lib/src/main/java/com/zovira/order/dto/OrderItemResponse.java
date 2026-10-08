package com.zovira.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productSlug,
        String title,
        String variantName,
        String sku,
        String imageUrl,
        BigDecimal unitPrice,
        BigDecimal unitMrp,
        int quantity,
        BigDecimal lineTotal,
        BigDecimal couponDiscount,
        String status,
        String sellerName,
        String shipmentNumber,
        boolean returnable,
        String returnableUntil,
        boolean reviewable) {
}

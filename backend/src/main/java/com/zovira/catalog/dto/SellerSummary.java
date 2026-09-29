package com.zovira.catalog.dto;

import java.math.BigDecimal;

public record SellerSummary(
        Long id,
        String storeName,
        String slug,
        BigDecimal ratingAverage,
        int ratingCount,
        String city,
        String state) {
}

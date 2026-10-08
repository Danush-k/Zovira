package com.zovira.catalog.dto;

import java.time.Instant;

public record StoreResponse(SellerSummary seller, String description, String logoUrl, Instant memberSince,
        long productCount) {
}

package com.zovira.catalog.dto;

public record BrandResponse(
        Long id,
        String name,
        String slug,
        String logoUrl,
        String description,
        boolean featured,
        String imageUrl,
        long productCount) {
}

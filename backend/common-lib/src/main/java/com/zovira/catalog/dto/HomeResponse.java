package com.zovira.catalog.dto;

import java.util.List;

public record HomeResponse(
        List<HeroSlide> hero,
        List<CategoryNode> categories,
        List<ProductSummary> deals,
        List<ProductSummary> trending,
        List<ProductSummary> bestSellers,
        List<ProductSummary> newArrivals,
        List<ProductSummary> topRated,
        List<ProductSummary> immersive,
        List<BrandResponse> brands) {
}

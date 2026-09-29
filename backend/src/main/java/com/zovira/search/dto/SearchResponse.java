package com.zovira.search.dto;

import com.zovira.catalog.dto.ProductSummary;
import com.zovira.common.web.PageResponse;
import java.math.BigDecimal;
import java.util.List;

public record SearchResponse(PageResponse<ProductSummary> results, Facets facets, String sort) {

    public record Facets(
            List<FacetValue> categories,
            List<FacetValue> brands,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<CountBucket> ratings,
            List<CountBucket> discounts,
            long inStockCount,
            long with3dCount) {
    }

    public record FacetValue(String slug, String name, long count) {
    }

    public record CountBucket(int value, long count) {
    }
}

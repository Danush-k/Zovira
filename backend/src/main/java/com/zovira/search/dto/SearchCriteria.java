package com.zovira.search.dto;

import java.math.BigDecimal;
import java.util.List;

/** Normalized product search request. All filters are optional. */
public record SearchCriteria(
        String query,
        String category,
        List<String> brands,
        String seller,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minRating,
        Integer minDiscount,
        boolean inStockOnly,
        boolean only3d,
        SearchSort sort,
        int page,
        int size) {

    public boolean hasQuery() {
        return query != null && !query.isBlank();
    }
}

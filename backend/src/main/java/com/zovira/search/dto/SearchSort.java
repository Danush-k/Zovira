package com.zovira.search.dto;

import java.util.Locale;

public enum SearchSort {
    RELEVANCE("rank DESC, p.sold_count DESC, p.id DESC"),
    POPULAR("p.sold_count DESC, p.id DESC"),
    PRICE_ASC("p.min_price ASC, p.id DESC"),
    PRICE_DESC("p.min_price DESC, p.id DESC"),
    NEWEST("p.published_at DESC NULLS LAST, p.id DESC"),
    RATING("p.rating_average DESC, p.rating_count DESC, p.id DESC"),
    DISCOUNT("p.discount_percent DESC, p.sold_count DESC, p.id DESC");

    private final String orderBy;

    SearchSort(String orderBy) {
        this.orderBy = orderBy;
    }

    /** Whitelisted ORDER BY fragment; never built from user input. */
    public String orderBy() {
        return orderBy;
    }

    public static SearchSort parse(String value, boolean hasQuery) {
        if (value == null || value.isBlank()) {
            return hasQuery ? RELEVANCE : POPULAR;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return hasQuery ? RELEVANCE : POPULAR;
        }
    }
}

package com.zovira.search.dto;

import java.math.BigDecimal;
import java.util.List;

public record SuggestResponse(List<ProductHit> products, List<Link> categories, List<Link> brands,
        List<String> queries) {

    public record ProductHit(String slug, String title, String imageUrl, BigDecimal price) {
    }

    public record Link(String slug, String name) {
    }
}

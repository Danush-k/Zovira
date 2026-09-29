package com.zovira.catalog.dto;

import java.util.List;

public record CategoryNode(
        Long id,
        String name,
        String slug,
        String icon,
        String imageUrl,
        String description,
        List<CategoryNode> children) {
}

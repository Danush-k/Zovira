package com.zovira.admin.dto;

import java.time.Instant;
import java.util.List;

public record AdminReviewRow(
        Long id,
        Long productId,
        String productTitle,
        String productSlug,
        String imageUrl,
        String customerName,
        int rating,
        String title,
        String body,
        List<String> images,
        boolean verifiedPurchase,
        String status,
        String moderationNote,
        int helpfulCount,
        Instant createdAt) {
}

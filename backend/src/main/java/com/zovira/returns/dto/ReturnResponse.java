package com.zovira.returns.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReturnResponse(
        Long id,
        String returnNumber,
        String orderNumber,
        String itemTitle,
        String imageUrl,
        String productSlug,
        int quantity,
        String reason,
        String comments,
        String status,
        BigDecimal refundAmount,
        String resolutionNote,
        String customerName,
        String sellerName,
        Instant createdAt,
        Instant resolvedAt) {
}

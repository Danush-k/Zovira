package com.zovira.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OrderSummaryResponse(
        String orderNumber,
        String status,
        String paymentStatus,
        String paymentMethod,
        BigDecimal totalAmount,
        int itemCount,
        Instant placedAt,
        LocalDate estimatedDeliveryDate,
        Instant deliveredAt,
        List<String> thumbnails,
        String firstItemTitle) {
}

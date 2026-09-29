package com.zovira.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminOrderRow(
        String orderNumber,
        String customerName,
        String customerEmail,
        String status,
        String paymentStatus,
        String paymentMethod,
        BigDecimal totalAmount,
        BigDecimal refundedAmount,
        int itemCount,
        String city,
        Instant placedAt) {
}

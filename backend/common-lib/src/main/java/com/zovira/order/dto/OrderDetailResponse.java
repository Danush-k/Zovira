package com.zovira.order.dto;

import com.zovira.user.dto.AddressResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OrderDetailResponse(
        String orderNumber,
        String status,
        String paymentStatus,
        String paymentMethod,
        String deliveryOption,
        AddressResponse shippingAddress,
        List<OrderItemResponse> items,
        List<ShipmentResponse> shipments,
        List<TimelineEntry> timeline,
        BigDecimal mrpTotal,
        BigDecimal subtotal,
        String couponCode,
        BigDecimal couponDiscount,
        BigDecimal shippingFee,
        BigDecimal codFee,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal refundedAmount,
        LocalDate estimatedDeliveryDate,
        Instant placedAt,
        Instant deliveredAt,
        Instant cancelledAt,
        String cancelReason,
        boolean cancellable,
        boolean paymentPending) {
}

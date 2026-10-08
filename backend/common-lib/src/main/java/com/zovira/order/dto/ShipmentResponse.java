package com.zovira.order.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ShipmentResponse(
        Long id,
        String shipmentNumber,
        String status,
        String sellerName,
        String carrier,
        String trackingNumber,
        LocalDate estimatedDeliveryDate,
        Instant shippedAt,
        Instant deliveredAt,
        List<Event> events,
        List<Long> itemIds) {

    public record Event(String status, String location, String description, Instant at) {
    }
}

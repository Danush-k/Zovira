package com.zovira.seller.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** One seller's shipment within a customer order, as shown in the seller's order queue. */
public record SellerOrderRow(
        Long shipmentId,
        String shipmentNumber,
        String orderNumber,
        String status,
        String orderStatus,
        String paymentMethod,
        String paymentStatus,
        String customerName,
        String city,
        String state,
        String pincode,
        LocalDate estimatedDeliveryDate,
        String carrier,
        String trackingNumber,
        Instant placedAt,
        BigDecimal itemsTotal,
        List<Item> items) {

    public record Item(Long id, String title, String variantName, String sku, String imageUrl, int quantity,
            BigDecimal lineTotal, String status) {
    }
}

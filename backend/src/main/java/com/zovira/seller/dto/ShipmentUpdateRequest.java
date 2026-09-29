package com.zovira.seller.dto;

import com.zovira.shipping.entity.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ShipmentUpdateRequest(
        @NotNull ShipmentStatus status,
        @Size(max = 60) String carrier,
        @Size(max = 60) String trackingNumber,
        @Size(max = 120) String location,
        @Size(max = 255) String note) {
}

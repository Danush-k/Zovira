package com.zovira.shipping.entity;

import com.zovira.common.entity.BaseEntity;
import java.time.Instant;

public class ShipmentEvent extends BaseEntity {

    @org.springframework.data.annotation.Transient
    private Shipment shipment;
    private Long shipmentId;
    private ShipmentStatus status;
    private String location;
    private String description;
    private Instant occurredAt = Instant.now();

    public ShipmentEvent() {
    }

    public ShipmentEvent(Shipment shipment, ShipmentStatus status, String location, String description,
            Instant occurredAt) {
        this.shipment = shipment;
        this.shipmentId = shipment != null ? shipment.getId() : null;
        this.status = status;
        this.location = location;
        this.description = description;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
        this.shipmentId = shipment != null ? shipment.getId() : null;
    }

    public Long getShipmentId() {
        if (shipmentId != null) return shipmentId;
        return shipment != null ? shipment.getId() : null;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}

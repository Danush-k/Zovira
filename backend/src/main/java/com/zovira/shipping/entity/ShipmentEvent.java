package com.zovira.shipping.entity;

import com.zovira.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shipment_events")
public class ShipmentEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false, updatable = false)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status;

    @Column(length = 120)
    private String location;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false)
    private Instant occurredAt;

    protected ShipmentEvent() {
    }

    ShipmentEvent(Shipment shipment, ShipmentStatus status, String location, String description,
            Instant occurredAt) {
        this.shipment = shipment;
        this.status = status;
        this.location = location;
        this.description = description;
        this.occurredAt = occurredAt;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}

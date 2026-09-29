package com.zovira.shipping.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.seller.entity.Seller;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The portion of an order fulfilled by a single seller. */
@Entity
@Table(name = "shipments")
public class Shipment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false, updatable = false)
    private Seller seller;

    @Column(nullable = false, unique = true, length = 24, updatable = false)
    private String shipmentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.PENDING;

    @Column(length = 60)
    private String carrier;

    @Column(length = 60)
    private String trackingNumber;

    private LocalDate estimatedDeliveryDate;

    private Instant processingAt;

    private Instant shippedAt;

    private Instant outForDeliveryAt;

    private Instant deliveredAt;

    private Instant cancelledAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "shipment")
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt ASC, id ASC")
    private List<ShipmentEvent> events = new ArrayList<>();

    protected Shipment() {
    }

    public Shipment(Order order, Seller seller, String shipmentNumber, LocalDate estimatedDeliveryDate) {
        this.order = order;
        this.seller = seller;
        this.shipmentNumber = shipmentNumber;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public void assignItem(OrderItem item) {
        item.setShipment(this);
        items.add(item);
    }

    public void advance(ShipmentStatus target, String location, String description, Instant now) {
        if (!status.canAdvanceTo(target)) {
            throw new BusinessException(ErrorCodes.INVALID_STATE,
                    "Shipment cannot move from " + status + " to " + target);
        }
        this.status = target;
        switch (target) {
            case PROCESSING -> processingAt = now;
            case SHIPPED -> shippedAt = now;
            case OUT_FOR_DELIVERY -> outForDeliveryAt = now;
            case DELIVERED -> deliveredAt = now;
            case CANCELLED -> cancelledAt = now;
            default -> {
                // PENDING is only an initial state
            }
        }
        events.add(new ShipmentEvent(this, target, location, description, now));
    }

    public void recordEvent(String location, String description, Instant now) {
        events.add(new ShipmentEvent(this, status, location, description, now));
    }

    public Order getOrder() {
        return order;
    }

    public Seller getSeller() {
        return seller;
    }

    public String getShipmentNumber() {
        return shipmentNumber;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public Instant getProcessingAt() {
        return processingAt;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public Instant getOutForDeliveryAt() {
        return outForDeliveryAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public List<ShipmentEvent> getEvents() {
        return events;
    }
}

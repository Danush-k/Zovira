package com.zovira.shipping.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.seller.entity.Seller;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The portion of an order fulfilled by a single seller. */
@Document(collection = "shipments")
public class Shipment extends AuditableEntity {

    private Order order;

    @Indexed
    private Long orderId;

    private Seller seller;

    @Indexed
    private Long sellerId;

    @Indexed(unique = true)
    private String shipmentNumber;

    private ShipmentStatus status = ShipmentStatus.PENDING;
    private String carrier;
    private String trackingNumber;
    private LocalDate estimatedDeliveryDate;
    private Instant processingAt;
    private Instant shippedAt;
    private Instant outForDeliveryAt;
    private Instant deliveredAt;
    private Instant cancelledAt;

    @Version
    private Long version;

    private List<OrderItem> items = new ArrayList<>();
    private List<ShipmentEvent> events = new ArrayList<>();

    public Shipment() {
    }

    public Shipment(Order order, Seller seller, String shipmentNumber, LocalDate estimatedDeliveryDate) {
        this.order = order;
        this.orderId = order != null ? order.getId() : null;
        this.seller = seller;
        this.sellerId = seller != null ? seller.getId() : null;
        this.shipmentNumber = shipmentNumber;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public void assignItem(OrderItem item) {
        item.setShipment(this);
        if (item.getId() == null) {
            long maxId = items.stream().mapToLong(i -> i.getId() != null ? i.getId() : 0).max().orElse(0);
            item.setId(maxId + 1);
        }
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
            }
        }
        ShipmentEvent ev = new ShipmentEvent(this, target, location, description, now);
        long maxId = events.stream().mapToLong(e -> e.getId() != null ? e.getId() : 0).max().orElse(0);
        ev.setId(maxId + 1);
        events.add(ev);
    }

    public void recordEvent(String location, String description, Instant now) {
        ShipmentEvent ev = new ShipmentEvent(this, status, location, description, now);
        long maxId = events.stream().mapToLong(e -> e.getId() != null ? e.getId() : 0).max().orElse(0);
        ev.setId(maxId + 1);
        events.add(ev);
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
        this.orderId = order != null ? order.getId() : null;
    }

    public Long getOrderId() {
        if (orderId != null) return orderId;
        return order != null ? order.getId() : null;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
        this.sellerId = seller != null ? seller.getId() : null;
    }

    public Long getSellerId() {
        if (sellerId != null) return sellerId;
        return seller != null ? seller.getId() : null;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getShipmentNumber() {
        return shipmentNumber;
    }

    public void setShipmentNumber(String shipmentNumber) {
        this.shipmentNumber = shipmentNumber;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
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

    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public Instant getProcessingAt() {
        return processingAt;
    }

    public void setProcessingAt(Instant processingAt) {
        this.processingAt = processingAt;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(Instant shippedAt) {
        this.shippedAt = shippedAt;
    }

    public Instant getOutForDeliveryAt() {
        return outForDeliveryAt;
    }

    public void setOutForDeliveryAt(Instant outForDeliveryAt) {
        this.outForDeliveryAt = outForDeliveryAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public List<ShipmentEvent> getEvents() {
        return events;
    }

    public void setEvents(List<ShipmentEvent> events) {
        this.events = events == null ? new ArrayList<>() : events;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

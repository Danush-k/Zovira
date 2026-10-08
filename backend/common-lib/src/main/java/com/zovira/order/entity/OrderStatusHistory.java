package com.zovira.order.entity;

import com.zovira.common.entity.BaseEntity;
import java.time.Instant;

public class OrderStatusHistory extends BaseEntity {

    @org.springframework.data.annotation.Transient
    private Order order;
    private Long orderId;
    private OrderStatus status;
    private String note;
    private Long actorId;
    private Instant createdAt = Instant.now();

    public OrderStatusHistory() {
    }

    public OrderStatusHistory(Order order, OrderStatus status, String note, Long actorId, Instant createdAt) {
        this.order = order;
        this.orderId = order != null ? order.getId() : null;
        this.status = status;
        this.note = note;
        this.actorId = actorId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

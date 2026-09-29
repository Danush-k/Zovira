package com.zovira.order.entity;

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
@Table(name = "order_status_history")
public class OrderStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OrderStatus status;

    @Column(length = 255)
    private String note;

    private Long actorId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderStatusHistory() {
    }

    OrderStatusHistory(Order order, OrderStatus status, String note, Long actorId, Instant createdAt) {
        this.order = order;
        this.status = status;
        this.note = note;
        this.actorId = actorId;
        this.createdAt = createdAt;
    }

    public Order getOrder() {
        return order;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public Long getActorId() {
        return actorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

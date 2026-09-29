package com.zovira.shipping.entity;

import com.zovira.order.entity.OrderStatus;

public enum ShipmentStatus {
    PENDING(0, OrderStatus.CONFIRMED),
    PROCESSING(1, OrderStatus.PROCESSING),
    SHIPPED(2, OrderStatus.SHIPPED),
    OUT_FOR_DELIVERY(3, OrderStatus.OUT_FOR_DELIVERY),
    DELIVERED(4, OrderStatus.DELIVERED),
    CANCELLED(-1, OrderStatus.CANCELLED);

    private final int rank;
    private final OrderStatus orderStatus;

    ShipmentStatus(int rank, OrderStatus orderStatus) {
        this.rank = rank;
        this.orderStatus = orderStatus;
    }

    public int rank() {
        return rank;
    }

    /** The order-level status implied when this is the least advanced active shipment. */
    public OrderStatus orderStatus() {
        return orderStatus;
    }

    public boolean canAdvanceTo(ShipmentStatus target) {
        if (this == CANCELLED || this == DELIVERED) {
            return false;
        }
        if (target == CANCELLED) {
            return this == PENDING || this == PROCESSING;
        }
        return target.rank > this.rank;
    }
}

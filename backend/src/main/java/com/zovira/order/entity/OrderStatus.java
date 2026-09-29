package com.zovira.order.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Order lifecycle. The fulfilment path is strictly forward-only
 * (PENDING_PAYMENT → PLACED → CONFIRMED → PROCESSING → SHIPPED → OUT_FOR_DELIVERY → DELIVERED);
 * CANCELLED, PAYMENT_FAILED and RETURNED are terminal side exits.
 */
public enum OrderStatus {
    PENDING_PAYMENT(0),
    PLACED(1),
    CONFIRMED(2),
    PROCESSING(3),
    SHIPPED(4),
    OUT_FOR_DELIVERY(5),
    DELIVERED(6),
    CANCELLED(-1),
    PAYMENT_FAILED(-1),
    RETURNED(-1);

    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(PENDING_PAYMENT, PLACED, CONFIRMED, PROCESSING);

    private final int rank;

    OrderStatus(int rank) {
        this.rank = rank;
    }

    public boolean isTerminal() {
        return this == CANCELLED || this == PAYMENT_FAILED || this == RETURNED;
    }

    public boolean isCancellable() {
        return CANCELLABLE.contains(this);
    }

    public boolean canTransitionTo(OrderStatus target) {
        if (this == target || isTerminal()) {
            return false;
        }
        return switch (target) {
            case CANCELLED -> isCancellable();
            case PAYMENT_FAILED -> this == PENDING_PAYMENT;
            case RETURNED -> this == DELIVERED;
            default -> target.rank > this.rank;
        };
    }
}

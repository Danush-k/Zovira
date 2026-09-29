package com.zovira.order.event;

/**
 * Published after order state changes commit. Notification, email and analytics listeners react
 * without the order module knowing about them.
 */
public record OrderEvent(Long orderId, Long userId, String orderNumber, Type type, String note) {

    public enum Type {
        PLACED,
        PAYMENT_CONFIRMED,
        PAYMENT_FAILED,
        PROCESSING,
        SHIPPED,
        OUT_FOR_DELIVERY,
        DELIVERED,
        CANCELLED,
        REFUNDED,
        RETURN_UPDATED
    }
}

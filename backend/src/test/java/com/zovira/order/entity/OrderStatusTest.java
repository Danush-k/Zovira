package com.zovira.order.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrderStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING_PAYMENT, PLACED, true",
            "PLACED, CONFIRMED, true",
            "CONFIRMED, SHIPPED, true",
            "SHIPPED, OUT_FOR_DELIVERY, true",
            "OUT_FOR_DELIVERY, DELIVERED, true",
            "DELIVERED, SHIPPED, false",
            "SHIPPED, CANCELLED, false",
            "PROCESSING, CANCELLED, true",
            "PENDING_PAYMENT, PAYMENT_FAILED, true",
            "CONFIRMED, PAYMENT_FAILED, false",
            "DELIVERED, RETURNED, true",
            "CANCELLED, CONFIRMED, false",
            "CONFIRMED, CONFIRMED, false"
    })
    void enforcesTheLifecycle(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    void terminalStatesAreTerminal() {
        assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(OrderStatus.RETURNED.isTerminal()).isTrue();
        assertThat(OrderStatus.DELIVERED.isTerminal()).isFalse();
    }
}

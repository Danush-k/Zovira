package com.zovira.payment.service;

import com.zovira.coupon.service.CouponLedger;
import com.zovira.inventory.service.InventoryService;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItemStatus;
import com.zovira.order.entity.OrderStatus;
import com.zovira.order.entity.PaymentStatus;
import com.zovira.order.event.OrderEvent;
import com.zovira.payment.entity.Payment;
import com.zovira.payment.entity.PaymentTransactionStatus;
import java.time.Clock;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Applies the order-side consequences of payment outcomes. Idempotent for repeated notifications. */
@Component
public class OrderPaymentHandler {

    private final InventoryService inventory;
    private final CouponLedger couponLedger;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OrderPaymentHandler(InventoryService inventory, CouponLedger couponLedger, ApplicationEventPublisher events,
            Clock clock) {
        this.inventory = inventory;
        this.couponLedger = couponLedger;
        this.events = events;
        this.clock = clock;
    }

    /** Payment captured: confirm the order and turn reserved stock into sold stock. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void captured(Payment payment, String providerPaymentId) {
        if (payment.isCaptured()) {
            return;
        }
        Instant now = clock.instant();
        payment.markCaptured(providerPaymentId, now);
        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.PAID);
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.transitionTo(OrderStatus.PLACED, null, null, now);
            order.transitionTo(OrderStatus.CONFIRMED, "Payment received via " + payment.getMethod().name(), null, now);
            inventory.commit(order.getItems());
            events.publishEvent(new OrderEvent(order.getId(), order.getUser().getId(), order.getOrderNumber(),
                    OrderEvent.Type.PAYMENT_CONFIRMED, null));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void failed(Payment payment, String providerPaymentId, String reason) {
        if (payment.getStatus() == PaymentTransactionStatus.CREATED) {
            payment.markFailed(providerPaymentId, reason);
        }
    }

    /** No successful payment within the window: cancel and put the stock back on sale. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void expire(Order order) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }
        order.transitionTo(OrderStatus.PAYMENT_FAILED, "Payment was not completed in time", null, clock.instant());
        order.setPaymentStatus(PaymentStatus.FAILED);
        inventory.release(order.getItems());
        order.getItems().forEach(i -> i.setStatus(OrderItemStatus.CANCELLED));
        couponLedger.reverse(order);
        events.publishEvent(new OrderEvent(order.getId(), order.getUser().getId(), order.getOrderNumber(),
                OrderEvent.Type.PAYMENT_FAILED, null));
    }
}

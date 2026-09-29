package com.zovira.payment.service;

import com.zovira.order.entity.OrderStatus;
import com.zovira.order.repository.OrderRepository;
import com.zovira.payment.gateway.PaymentProperties;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Cancels orders whose online payment never completed and returns their reserved stock to sale. */
@Component
public class PaymentExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpiryJob.class);

    private final OrderRepository orders;
    private final OrderPaymentHandler handler;
    private final PaymentProperties properties;
    private final TransactionTemplate tx;
    private final Clock clock;

    public PaymentExpiryJob(OrderRepository orders, OrderPaymentHandler handler, PaymentProperties properties,
            TransactionTemplate tx, Clock clock) {
        this.orders = orders;
        this.handler = handler;
        this.properties = properties;
        this.tx = tx;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${zovira.jobs.payment-expiry-ms:60000}", initialDelay = 30000)
    public void expireUnpaidOrders() {
        var ids = orders.findIdsByStatusPlacedBefore(OrderStatus.PENDING_PAYMENT,
                clock.instant().minus(properties.pendingPaymentTimeout()));
        for (Long id : ids) {
            try {
                tx.executeWithoutResult(s -> orders.findById(id).ifPresent(handler::expire));
            } catch (RuntimeException e) {
                log.warn("Could not expire order {}: {}", id, e.getMessage());
            }
        }
        if (!ids.isEmpty()) {
            log.info("Expired {} unpaid orders", ids.size());
        }
    }
}

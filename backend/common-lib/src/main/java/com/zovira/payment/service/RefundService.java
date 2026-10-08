package com.zovira.payment.service;

import com.zovira.common.util.SecureTokens;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.PaymentMethod;
import com.zovira.order.entity.PaymentStatus;
import com.zovira.order.event.OrderEvent;
import com.zovira.payment.entity.Payment;
import com.zovira.payment.entity.PaymentTransactionStatus;
import com.zovira.payment.entity.Refund;
import com.zovira.payment.gateway.PaymentGateway;
import com.zovira.payment.repository.PaymentRepository;
import com.zovira.payment.repository.RefundRepository;
import com.zovira.returns.entity.ReturnRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues refunds. Online payments are refunded through the gateway to the original method;
 * cash-on-delivery orders that were paid in cash are refunded by bank transfer (recorded as a
 * manual refund). A failed gateway refund is recorded as FAILED for operations to retry.
 */
@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final PaymentGateway gateway;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RefundService(PaymentRepository payments, RefundRepository refunds, PaymentGateway gateway,
            ApplicationEventPublisher events, Clock clock) {
        this.payments = payments;
        this.refunds = refunds;
        this.gateway = gateway;
        this.events = events;
        this.clock = clock;
    }

    /** Returns the refund, or empty when nothing was ever paid (for example an unpaid COD order). */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Refund> refund(Order order, BigDecimal amount, String reason, ReturnRequest returnRequest) {
        if (amount.signum() <= 0 || order.getPaymentStatus() == PaymentStatus.PENDING
                || order.getPaymentStatus() == PaymentStatus.FAILED) {
            return Optional.empty();
        }
        BigDecimal refundable = order.getTotalAmount().subtract(order.getRefundedAmount());
        BigDecimal value = amount.min(refundable);
        if (value.signum() <= 0) {
            return Optional.empty();
        }
        Payment payment = payments.findFirstByOrderIdAndStatusOrderByIdDesc(order.getId(), PaymentTransactionStatus.CAPTURED)
                .orElse(null);
        Refund refund = new Refund(SecureTokens.reference("RF", 10), order, payment, returnRequest, value, reason);
        refunds.save(refund);
        try {
            if (payment == null || order.getPaymentMethod() == PaymentMethod.COD
                    || payment.getProvider() == com.zovira.payment.entity.PaymentProvider.COD) {
                refund.markProcessed("MANUAL-" + refund.getRefundNumber(), clock.instant());
            } else {
                refund.markProcessed(gateway.refund(payment, value), clock.instant());
                payment.setStatus(order.getRefundedAmount().add(value).compareTo(order.getTotalAmount()) >= 0
                        ? PaymentTransactionStatus.REFUNDED : PaymentTransactionStatus.PARTIALLY_REFUNDED);
            }
            order.recordRefund(value);
            events.publishEvent(new OrderEvent(order.getId(), order.getUser().getId(), order.getOrderNumber(),
                    OrderEvent.Type.REFUNDED, value.toPlainString()));
        } catch (RuntimeException e) {
            log.error("Refund {} failed for order {}", refund.getRefundNumber(), order.getOrderNumber(), e);
            refund.markFailed(e.getMessage());
        }
        return Optional.of(refund);
    }
}

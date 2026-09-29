package com.zovira.payment.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.order.entity.Order;
import com.zovira.returns.entity.ReturnRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refunds")
public class Refund extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 24, updatable = false)
    private String refundNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    /** Null for cash-on-delivery orders, which are refunded outside the payment gateway. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", updatable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", updatable = false)
    private ReturnRequest returnRequest;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RefundStatus status = RefundStatus.PENDING;

    @Column(length = 64)
    private String providerRefundId;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(length = 255)
    private String failureReason;

    private Instant processedAt;

    protected Refund() {
    }

    public Refund(String refundNumber, Order order, Payment payment, ReturnRequest returnRequest, BigDecimal amount,
            String reason) {
        this.refundNumber = refundNumber;
        this.order = order;
        this.payment = payment;
        this.returnRequest = returnRequest;
        this.amount = amount;
        this.reason = reason;
    }

    public void markProcessed(String providerRefundId, Instant now) {
        this.status = RefundStatus.PROCESSED;
        this.providerRefundId = providerRefundId;
        this.failureReason = null;
        this.processedAt = now;
    }

    public void markFailed(String reason) {
        this.status = RefundStatus.FAILED;
        this.failureReason = reason;
    }

    public String getRefundNumber() {
        return refundNumber;
    }

    public Order getOrder() {
        return order;
    }

    public Payment getPayment() {
        return payment;
    }

    public ReturnRequest getReturnRequest() {
        return returnRequest;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public String getProviderRefundId() {
        return providerRefundId;
    }

    public String getReason() {
        return reason;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}

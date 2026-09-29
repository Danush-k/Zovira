package com.zovira.payment.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

/** One payment attempt against an order. An order may have several failed attempts and one capture. */
@Entity
@Table(name = "payments")
public class Payment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentTransactionStatus status = PaymentTransactionStatus.CREATED;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(length = 64)
    private String providerOrderId;

    @Column(length = 64)
    private String providerPaymentId;

    @Column(length = 255)
    private String failureReason;

    private Instant paidAt;

    @Version
    private long version;

    protected Payment() {
    }

    public Payment(Order order, Long userId, PaymentProvider provider, PaymentMethod method, BigDecimal amount) {
        this.order = order;
        this.userId = userId;
        this.provider = provider;
        this.method = method;
        this.amount = amount;
    }

    public boolean isCaptured() {
        return status == PaymentTransactionStatus.CAPTURED;
    }

    public void markCaptured(String providerPaymentId, Instant now) {
        this.status = PaymentTransactionStatus.CAPTURED;
        this.providerPaymentId = providerPaymentId;
        this.failureReason = null;
        this.paidAt = now;
    }

    public void markFailed(String providerPaymentId, String reason) {
        this.status = PaymentTransactionStatus.FAILED;
        if (providerPaymentId != null) {
            this.providerPaymentId = providerPaymentId;
        }
        this.failureReason = reason;
    }

    public Order getOrder() {
        return order;
    }

    public Long getUserId() {
        return userId;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentTransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProviderOrderId() {
        return providerOrderId;
    }

    public void setProviderOrderId(String providerOrderId) {
        this.providerOrderId = providerOrderId;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}

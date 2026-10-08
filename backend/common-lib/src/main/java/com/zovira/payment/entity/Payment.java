package com.zovira.payment.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.PaymentMethod;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;

/** One payment attempt against an order. An order may have several failed attempts and one capture. */
@Document(collection = "payments")
public class Payment extends AuditableEntity {

    private Order order;

    @Indexed
    private Long orderId;

    @Indexed
    private Long userId;

    private PaymentProvider provider;
    private PaymentMethod method;
    private PaymentTransactionStatus status = PaymentTransactionStatus.CREATED;
    private BigDecimal amount;
    private String currency = "INR";
    private String providerOrderId;
    private String providerPaymentId;
    private String failureReason;
    private Instant paidAt;

    @Version
    private Long version;

    public Payment() {
    }

    public Payment(Order order, Long userId, PaymentProvider provider, PaymentMethod method, BigDecimal amount) {
        this.order = order;
        this.orderId = order != null ? order.getId() : null;
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

    public void setOrder(Order order) {
        this.order = order;
        this.orderId = order != null ? order.getId() : null;
    }

    public Long getOrderId() {
        if (orderId != null) return orderId;
        return order != null ? order.getId() : null;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
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

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

package com.zovira.payment.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.order.entity.Order;
import com.zovira.returns.entity.ReturnRequest;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "refunds")
public class Refund extends AuditableEntity {

    @Indexed(unique = true)
    private String refundNumber;

    private Order order;

    @Indexed
    private Long orderId;

    private Payment payment;

    @Indexed
    private Long paymentId;

    private ReturnRequest returnRequest;

    @Indexed
    private Long returnRequestId;

    private BigDecimal amount;
    private RefundStatus status = RefundStatus.PENDING;
    private String providerRefundId;
    private String reason;
    private String failureReason;
    private Instant processedAt;

    public Refund() {
    }

    public Refund(String refundNumber, Order order, Payment payment, ReturnRequest returnRequest, BigDecimal amount,
            String reason) {
        this.refundNumber = refundNumber;
        this.order = order;
        this.orderId = order != null ? order.getId() : null;
        this.payment = payment;
        this.paymentId = payment != null ? payment.getId() : null;
        this.returnRequest = returnRequest;
        this.returnRequestId = returnRequest != null ? returnRequest.getId() : null;
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

    public void setRefundNumber(String refundNumber) {
        this.refundNumber = refundNumber;
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

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
        this.paymentId = payment != null ? payment.getId() : null;
    }

    public Long getPaymentId() {
        if (paymentId != null) return paymentId;
        return payment != null ? payment.getId() : null;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public ReturnRequest getReturnRequest() {
        return returnRequest;
    }

    public void setReturnRequest(ReturnRequest returnRequest) {
        this.returnRequest = returnRequest;
        this.returnRequestId = returnRequest != null ? returnRequest.getId() : null;
    }

    public Long getReturnRequestId() {
        if (returnRequestId != null) return returnRequestId;
        return returnRequest != null ? returnRequest.getId() : null;
    }

    public void setReturnRequestId(Long returnRequestId) {
        this.returnRequestId = returnRequestId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public void setStatus(RefundStatus status) {
        this.status = status;
    }

    public String getProviderRefundId() {
        return providerRefundId;
    }

    public void setProviderRefundId(String providerRefundId) {
        this.providerRefundId = providerRefundId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}

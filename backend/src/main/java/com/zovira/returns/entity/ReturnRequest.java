package com.zovira.returns.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.seller.entity.Seller;
import com.zovira.user.entity.User;
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

@Entity
@Table(name = "return_requests")
public class ReturnRequest extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 24, updatable = false)
    private String returnNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false, updatable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false, updatable = false)
    private Seller seller;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReturnReason reason;

    @Column(length = 1000)
    private String comments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReturnStatus status = ReturnStatus.REQUESTED;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(length = 500)
    private String resolutionNote;

    private Long resolvedBy;

    private Instant resolvedAt;

    @Version
    private long version;

    protected ReturnRequest() {
    }

    public ReturnRequest(String returnNumber, OrderItem orderItem, int quantity, ReturnReason reason,
            String comments, BigDecimal refundAmount) {
        this.returnNumber = returnNumber;
        this.order = orderItem.getOrder();
        this.orderItem = orderItem;
        this.user = orderItem.getOrder().getUser();
        this.seller = orderItem.getSeller();
        this.quantity = quantity;
        this.reason = reason;
        this.comments = comments;
        this.refundAmount = refundAmount;
    }

    public void approve(Long actorId, String note, Instant now) {
        requireStatus(ReturnStatus.REQUESTED);
        resolve(ReturnStatus.APPROVED, actorId, note, now);
    }

    public void reject(Long actorId, String note, Instant now) {
        requireStatus(ReturnStatus.REQUESTED);
        resolve(ReturnStatus.REJECTED, actorId, note, now);
    }

    public void markPickedUp(Long actorId, Instant now) {
        requireStatus(ReturnStatus.APPROVED);
        resolve(ReturnStatus.PICKED_UP, actorId, resolutionNote, now);
    }

    public void markRefunded(Instant now) {
        requireStatus(ReturnStatus.PICKED_UP);
        this.status = ReturnStatus.REFUNDED;
        this.resolvedAt = now;
    }

    private void requireStatus(ReturnStatus expected) {
        if (status != expected) {
            throw new BusinessException(ErrorCodes.INVALID_STATE,
                    "Return " + returnNumber + " is " + status + ", expected " + expected);
        }
    }

    private void resolve(ReturnStatus next, Long actorId, String note, Instant now) {
        this.status = next;
        this.resolvedBy = actorId;
        this.resolutionNote = note;
        this.resolvedAt = now;
    }

    public String getReturnNumber() {
        return returnNumber;
    }

    public Order getOrder() {
        return order;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public User getUser() {
        return user;
    }

    public Seller getSeller() {
        return seller;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReturnReason getReason() {
        return reason;
    }

    public String getComments() {
        return comments;
    }

    public ReturnStatus getStatus() {
        return status;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}

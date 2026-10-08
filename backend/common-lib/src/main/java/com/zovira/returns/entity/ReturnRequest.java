package com.zovira.returns.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.seller.entity.Seller;
import com.zovira.user.entity.User;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "return_requests")
public class ReturnRequest extends AuditableEntity {

    @Indexed(unique = true)
    private String returnNumber;

    private Order order;

    @Indexed
    private Long orderId;

    private OrderItem orderItem;

    @Indexed
    private Long orderItemId;

    private User user;

    @Indexed
    private Long userId;

    private Seller seller;

    @Indexed
    private Long sellerId;

    private int quantity;
    private ReturnReason reason;
    private String comments;
    private ReturnStatus status = ReturnStatus.REQUESTED;
    private BigDecimal refundAmount;
    private String resolutionNote;
    private Long resolvedBy;
    private Instant resolvedAt;

    @Version
    private Long version;

    public ReturnRequest() {
    }

    public ReturnRequest(String returnNumber, OrderItem orderItem, int quantity, ReturnReason reason,
            String comments, BigDecimal refundAmount) {
        this.returnNumber = returnNumber;
        this.orderItem = orderItem;
        this.orderItemId = orderItem != null ? orderItem.getId() : null;
        if (orderItem != null && orderItem.getOrder() != null) {
            this.order = orderItem.getOrder();
            this.orderId = orderItem.getOrder().getId();
            this.user = orderItem.getOrder().getUser();
            this.userId = orderItem.getOrder().getUserId();
        }
        if (orderItem != null) {
            this.seller = orderItem.getSeller();
            this.sellerId = orderItem.getSellerId();
        }
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

    public void setReturnNumber(String returnNumber) {
        this.returnNumber = returnNumber;
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

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
        this.orderItemId = orderItem != null ? orderItem.getId() : null;
    }

    public Long getOrderItemId() {
        if (orderItemId != null) return orderItemId;
        return orderItem != null ? orderItem.getId() : null;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
    }

    public Long getUserId() {
        if (userId != null) return userId;
        return user != null ? user.getId() : null;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
        this.sellerId = seller != null ? seller.getId() : null;
    }

    public Long getSellerId() {
        if (sellerId != null) return sellerId;
        return seller != null ? seller.getId() : null;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public ReturnReason getReason() {
        return reason;
    }

    public void setReason(ReturnReason reason) {
        this.reason = reason;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public ReturnStatus getStatus() {
        return status;
    }

    public void setStatus(ReturnStatus status) {
        this.status = status;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

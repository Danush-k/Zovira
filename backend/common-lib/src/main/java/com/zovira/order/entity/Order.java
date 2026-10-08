package com.zovira.order.entity;

import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.entity.AuditableEntity;
import com.zovira.shipping.entity.Shipment;
import com.zovira.user.entity.User;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "orders")
public class Order extends AuditableEntity {

    @Indexed(unique = true)
    private String orderNumber;

    private User user;

    @Indexed
    private Long userId;

    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    private DeliveryOption deliveryOption = DeliveryOption.STANDARD;
    private ShippingAddress shippingAddress;
    private int itemCount;
    private BigDecimal mrpTotal;
    private BigDecimal subtotal;
    private String couponCode;
    private BigDecimal couponDiscount = BigDecimal.ZERO;
    private BigDecimal shippingFee = BigDecimal.ZERO;
    private BigDecimal codFee = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount;
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    private String currency = "INR";
    private LocalDate estimatedDeliveryDate;
    private String idempotencyKey;
    private Instant placedAt;
    private Instant confirmedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    private String cancelReason;

    @Version
    private Long version;

    private List<OrderItem> items = new ArrayList<>();
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();
    private List<Shipment> shipments = new ArrayList<>();

    public Order() {
    }

    public Order(String orderNumber, User user, PaymentMethod paymentMethod, DeliveryOption deliveryOption,
            ShippingAddress shippingAddress, Instant placedAt) {
        this.orderNumber = orderNumber;
        this.user = user;
        this.userId = user != null ? user.getId() : null;
        this.paymentMethod = paymentMethod;
        this.deliveryOption = deliveryOption;
        this.shippingAddress = shippingAddress;
        this.placedAt = placedAt;
    }

    /** Sets the initial status and records it in the history. */
    public void start(OrderStatus initialStatus, String note, Instant now) {
        this.status = initialStatus;
        statusHistory.add(new OrderStatusHistory(this, initialStatus, note, null, now));
    }

    /** Moves the order along its lifecycle, rejecting illegal transitions. */
    public void transitionTo(OrderStatus target, String note, Long actorId, Instant now) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessException(ErrorCodes.INVALID_STATE,
                    "Order cannot move from " + status + " to " + target);
        }
        this.status = target;
        switch (target) {
            case CONFIRMED -> confirmedAt = now;
            case DELIVERED -> deliveredAt = now;
            case CANCELLED, PAYMENT_FAILED -> {
                cancelledAt = now;
                cancelReason = note;
            }
            default -> {
            }
        }
        statusHistory.add(new OrderStatusHistory(this, target, note, actorId, now));
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        if (item.getId() == null) {
            long maxId = items.stream().mapToLong(i -> i.getId() != null ? i.getId() : 0).max().orElse(0);
            item.setId(maxId + 1);
        }
        items.add(item);
    }

    public void addShipment(Shipment shipment) {
        shipments.add(shipment);
    }

    public void applyTotals(int itemCount, BigDecimal mrpTotal, BigDecimal subtotal, String couponCode,
            BigDecimal couponDiscount, BigDecimal shippingFee, BigDecimal codFee, BigDecimal taxAmount,
            BigDecimal totalAmount) {
        this.itemCount = itemCount;
        this.mrpTotal = mrpTotal;
        this.subtotal = subtotal;
        this.couponCode = couponCode;
        this.couponDiscount = couponDiscount;
        this.shippingFee = shippingFee;
        this.codFee = codFee;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
    }

    public void recordRefund(BigDecimal amount) {
        this.refundedAmount = (this.refundedAmount != null ? this.refundedAmount : BigDecimal.ZERO).add(amount);
        this.paymentStatus = (this.refundedAmount != null && this.totalAmount != null && this.refundedAmount.compareTo(totalAmount) >= 0)
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
    }

    public boolean isOwnedBy(Long userId) {
        return getUserId() != null && getUserId().equals(userId);
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public DeliveryOption getDeliveryOption() {
        return deliveryOption;
    }

    public void setDeliveryOption(DeliveryOption deliveryOption) {
        this.deliveryOption = deliveryOption;
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(ShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public BigDecimal getMrpTotal() {
        return mrpTotal;
    }

    public void setMrpTotal(BigDecimal mrpTotal) {
        this.mrpTotal = mrpTotal;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public BigDecimal getCouponDiscount() {
        return couponDiscount;
    }

    public void setCouponDiscount(BigDecimal couponDiscount) {
        this.couponDiscount = couponDiscount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }

    public BigDecimal getCodFee() {
        return codFee;
    }

    public void setCodFee(BigDecimal codFee) {
        this.codFee = codFee;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(Instant placedAt) {
        this.placedAt = placedAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public List<OrderStatusHistory> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<OrderStatusHistory> statusHistory) {
        this.statusHistory = statusHistory == null ? new ArrayList<>() : statusHistory;
    }

    public List<Shipment> getShipments() {
        return shipments;
    }

    public void setShipments(List<Shipment> shipments) {
        this.shipments = shipments == null ? new ArrayList<>() : shipments;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

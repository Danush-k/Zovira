package com.zovira.order.entity;

import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.entity.AuditableEntity;
import com.zovira.shipping.entity.Shipment;
import com.zovira.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 24, updatable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeliveryOption deliveryOption = DeliveryOption.STANDARD;

    @Embedded
    private ShippingAddress shippingAddress;

    @Column(nullable = false)
    private int itemCount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal mrpTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(length = 40)
    private String couponCode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal couponDiscount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal codFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    private LocalDate estimatedDeliveryDate;

    @Column(length = 64, updatable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant placedAt;

    private Instant confirmedAt;

    private Instant deliveredAt;

    private Instant cancelledAt;

    @Column(length = 255)
    private String cancelReason;

    @Version
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Shipment> shipments = new ArrayList<>();

    protected Order() {
    }

    public Order(String orderNumber, User user, PaymentMethod paymentMethod, DeliveryOption deliveryOption,
            ShippingAddress shippingAddress, Instant placedAt) {
        this.orderNumber = orderNumber;
        this.user = user;
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
                // no timestamp column for intermediate states
            }
        }
        statusHistory.add(new OrderStatusHistory(this, target, note, actorId, now));
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
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
        this.refundedAmount = this.refundedAmount.add(amount);
        this.paymentStatus = refundedAmount.compareTo(totalAmount) >= 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public User getUser() {
        return user;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
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

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public int getItemCount() {
        return itemCount;
    }

    public BigDecimal getMrpTotal() {
        return mrpTotal;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public BigDecimal getCouponDiscount() {
        return couponDiscount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getCodFee() {
        return codFee;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public String getCurrency() {
        return currency;
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

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public List<OrderStatusHistory> getStatusHistory() {
        return statusHistory;
    }

    public List<Shipment> getShipments() {
        return shipments;
    }
}

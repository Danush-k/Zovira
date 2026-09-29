package com.zovira.coupon.entity;

import com.zovira.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coupon_redemptions")
public class CouponRedemption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false, updatable = false)
    private Coupon coupon;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, unique = true, updatable = false)
    private Long orderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected CouponRedemption() {
    }

    public CouponRedemption(Coupon coupon, Long userId, Long orderId, BigDecimal discountAmount, Instant createdAt) {
        this.coupon = coupon;
        this.userId = userId;
        this.orderId = orderId;
        this.discountAmount = discountAmount;
        this.createdAt = createdAt;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

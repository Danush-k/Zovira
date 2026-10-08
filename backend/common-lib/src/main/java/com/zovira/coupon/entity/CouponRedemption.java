package com.zovira.coupon.entity;

import com.zovira.common.entity.BaseEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "coupon_redemptions")
public class CouponRedemption extends BaseEntity {

    private Coupon coupon;

    @Indexed
    private Long couponId;

    @Indexed
    private Long userId;

    @Indexed(unique = true)
    private Long orderId;

    private BigDecimal discountAmount;
    private Instant createdAt = Instant.now();

    public CouponRedemption() {
    }

    public CouponRedemption(Coupon coupon, Long userId, Long orderId, BigDecimal discountAmount, Instant createdAt) {
        this.coupon = coupon;
        this.couponId = coupon != null ? coupon.getId() : null;
        this.userId = userId;
        this.orderId = orderId;
        this.discountAmount = discountAmount;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public void setCoupon(Coupon coupon) {
        this.coupon = coupon;
        this.couponId = coupon != null ? coupon.getId() : null;
    }

    public Long getCouponId() {
        if (couponId != null) return couponId;
        return coupon != null ? coupon.getId() : null;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

package com.zovira.coupon.service;

import com.zovira.coupon.entity.Coupon;
import com.zovira.coupon.entity.CouponRedemption;
import com.zovira.coupon.repository.CouponRedemptionRepository;
import com.zovira.coupon.repository.CouponRepository;
import com.zovira.order.entity.Order;
import java.time.Clock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records coupon usage against orders and reverses it when an order never completes. */
@Component
public class CouponLedger {

    private final CouponRedemptionRepository redemptions;
    private final CouponRepository coupons;
    private final Clock clock;

    public CouponLedger(CouponRedemptionRepository redemptions, CouponRepository coupons, Clock clock) {
        this.redemptions = redemptions;
        this.coupons = coupons;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void redeem(Coupon coupon, Order order) {
        coupon.incrementUsage();
        redemptions.save(new CouponRedemption(coupon, order.getUser().getId(), order.getId(), order.getCouponDiscount(),
                clock.instant()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reverse(Order order) {
        redemptions.findByOrderId(order.getId()).ifPresent(r -> {
            coupons.findById(r.getCoupon().getId()).ifPresent(Coupon::decrementUsage);
            redemptions.delete(r);
        });
    }
}

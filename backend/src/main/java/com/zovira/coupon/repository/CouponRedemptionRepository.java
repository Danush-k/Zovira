package com.zovira.coupon.repository;

import java.util.Optional;
import com.zovira.coupon.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    long countByCouponIdAndUserId(Long couponId, Long userId);

    Optional<CouponRedemption> findByOrderId(Long orderId);
}

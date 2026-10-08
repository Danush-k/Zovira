package com.zovira.coupon.repository;

import com.zovira.coupon.entity.CouponRedemption;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CouponRedemptionRepository extends MongoRepository<CouponRedemption, Long> {

    long countByCouponIdAndUserId(Long couponId, Long userId);

    Optional<CouponRedemption> findByOrderId(Long orderId);
}

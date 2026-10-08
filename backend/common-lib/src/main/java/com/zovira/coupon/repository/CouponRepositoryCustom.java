package com.zovira.coupon.repository;

import com.zovira.coupon.entity.Coupon;
import java.time.Instant;
import java.util.List;

public interface CouponRepositoryCustom {

    List<Coupon> findLive(Instant now);
}

package com.zovira.coupon.repository;

import com.zovira.coupon.entity.Coupon;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CouponRepository extends MongoRepository<Coupon, Long>, CouponRepositoryCustom {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}

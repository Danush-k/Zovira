package com.zovira.coupon.repository;

import com.zovira.coupon.entity.Coupon;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long>, JpaSpecificationExecutor<Coupon> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Query("""
            select c from Coupon c
            where c.active = true and c.startsAt <= :now and (c.expiresAt is null or c.expiresAt > :now)
            order by c.discountValue desc
            """)
    List<Coupon> findLive(@Param("now") Instant now);
}

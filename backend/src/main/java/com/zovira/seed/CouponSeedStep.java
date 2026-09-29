package com.zovira.seed;

import com.zovira.catalog.repository.CategoryRepository;
import com.zovira.coupon.entity.Coupon;
import com.zovira.coupon.entity.CouponScope;
import com.zovira.coupon.entity.DiscountType;
import com.zovira.coupon.repository.CouponRepository;
import com.zovira.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Demo coupons covering every rule type: percentage, fixed, capped, scoped, user-specific and expired. */
@Component
@Order(20)
class CouponSeedStep implements SeedStep {

    private final CouponRepository coupons;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final Clock clock;

    CouponSeedStep(CouponRepository coupons, CategoryRepository categories, UserRepository users, Clock clock) {
        this.coupons = coupons;
        this.categories = categories;
        this.users = users;
        this.clock = clock;
    }

    @Override
    public String name() {
        return "coupons";
    }

    @Override
    public boolean shouldRun() {
        return coupons.count() == 0 && users.count() > 0;
    }

    @Override
    public void run() {
        Instant now = clock.instant();
        Instant start = now.minus(Duration.ofDays(30));

        Coupon welcome = coupon("WELCOME200", "Flat ₹200 off your first order above ₹1,499", DiscountType.FIXED, "200", start);
        welcome.setMinOrderValue(new BigDecimal("1499"));
        welcome.setPerUserLimit(1);

        Coupon zovira10 = coupon("ZOVIRA10", "10% off up to ₹500 on orders above ₹999", DiscountType.PERCENTAGE, "10", start);
        zovira10.setMinOrderValue(new BigDecimal("999"));
        zovira10.setMaxDiscount(new BigDecimal("500"));
        zovira10.setPerUserLimit(3);
        zovira10.setUsageLimit(5000);

        Coupon tech15 = coupon("TECH15", "15% off up to ₹3,000 on mobiles, laptops and electronics", DiscountType.PERCENTAGE, "15", start);
        tech15.setScope(CouponScope.CATEGORY);
        tech15.setMinOrderValue(new BigDecimal("4999"));
        tech15.setMaxDiscount(new BigDecimal("3000"));
        tech15.setPerUserLimit(2);
        tech15.setExpiresAt(now.plus(Duration.ofDays(45)));
        categories.findBySlug("mobiles").ifPresent(tech15.getCategories()::add);
        categories.findBySlug("laptops").ifPresent(tech15.getCategories()::add);
        categories.findBySlug("electronics").ifPresent(tech15.getCategories()::add);

        Coupon books = coupon("READMORE", "Flat ₹75 off on books above ₹499", DiscountType.FIXED, "75", start);
        books.setScope(CouponScope.CATEGORY);
        books.setMinOrderValue(new BigDecimal("499"));
        books.setPerUserLimit(5);
        categories.findBySlug("books").ifPresent(books.getCategories()::add);

        Coupon vip = coupon("AARAVVIP", "Exclusive: 20% off up to ₹1,000 for Aarav", DiscountType.PERCENTAGE, "20", start);
        vip.setUserSpecific(true);
        vip.setMaxDiscount(new BigDecimal("1000"));
        users.findByEmail("aarav@zovira.test").ifPresent(vip.getUsers()::add);

        Coupon expired = coupon("MONSOON25", "Monsoon sale: 25% off (ended)", DiscountType.PERCENTAGE, "25", now.minus(Duration.ofDays(90)));
        expired.setExpiresAt(now.minus(Duration.ofDays(30)));

        coupons.saveAll(java.util.List.of(welcome, zovira10, tech15, books, vip, expired));
    }

    private static Coupon coupon(String code, String description, DiscountType type, String value, Instant startsAt) {
        return new Coupon(code, description, type, new BigDecimal(value), startsAt);
    }
}

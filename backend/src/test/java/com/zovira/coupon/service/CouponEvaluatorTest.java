package com.zovira.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.zovira.catalog.entity.Category;
import com.zovira.coupon.entity.Coupon;
import com.zovira.coupon.entity.CouponScope;
import com.zovira.coupon.entity.DiscountType;
import com.zovira.coupon.service.CouponEvaluator.Line;
import com.zovira.user.entity.User;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CouponEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    private final Category electronics = category(1L, "Electronics", null);
    private final Category phones = category(2L, "Smartphones", electronics);
    private final Category books = category(3L, "Books", null);

    private static Category category(Long id, String name, Category parent) {
        Category c = new Category(name, name.toLowerCase());
        ReflectionTestUtils.setField(c, "id", id);
        c.setParent(parent);
        return c;
    }

    private static Coupon percent(String value) {
        return new Coupon("SAVE", "Save", DiscountType.PERCENTAGE, new BigDecimal(value), NOW.minus(Duration.ofDays(1)));
    }

    private List<Line> cart() {
        return List.of(new Line(10L, phones, new BigDecimal("20000")), new Line(20L, books, new BigDecimal("500")));
    }

    @Test
    void percentageDiscountIsCappedByMaxDiscount() {
        Coupon c = percent("10");
        c.setMaxDiscount(new BigDecimal("500"));

        var result = CouponEvaluator.evaluate(c, cart(), 1L, 0, NOW);

        assertThat(result.valid()).isTrue();
        assertThat(result.discount()).isEqualByComparingTo("500");
    }

    @Test
    void categoryScopeIncludesSubcategoriesOnly() {
        Coupon c = percent("10");
        c.setScope(CouponScope.CATEGORY);
        c.getCategories().add(electronics);

        var result = CouponEvaluator.evaluate(c, cart(), 1L, 0, NOW);

        assertThat(result.eligibleSubtotal()).isEqualByComparingTo("20000");
        assertThat(result.discount()).isEqualByComparingTo("2000");
    }

    @Test
    void fixedDiscountNeverExceedsEligibleAmount() {
        Coupon c = new Coupon("BIG", "Big", DiscountType.FIXED, new BigDecimal("900"), NOW.minus(Duration.ofDays(1)));
        c.setScope(CouponScope.CATEGORY);
        c.getCategories().add(books);

        assertThat(CouponEvaluator.evaluate(c, cart(), 1L, 0, NOW).discount()).isEqualByComparingTo("500");
    }

    @Test
    void rejectsExpiredNotStartedAndBelowMinimum() {
        Coupon expired = percent("10");
        expired.setExpiresAt(NOW.minusSeconds(1));
        assertThat(CouponEvaluator.evaluate(expired, cart(), 1L, 0, NOW).message()).contains("expired");

        Coupon future = new Coupon("SOON", "Soon", DiscountType.PERCENTAGE, BigDecimal.TEN, NOW.plusSeconds(60));
        assertThat(CouponEvaluator.evaluate(future, cart(), 1L, 0, NOW).valid()).isFalse();

        Coupon minimum = percent("10");
        minimum.setMinOrderValue(new BigDecimal("50000"));
        assertThat(CouponEvaluator.evaluate(minimum, cart(), 1L, 0, NOW).message()).contains("29500");
    }

    @Test
    void enforcesPerUserLimitAndUserSpecificCoupons() {
        Coupon c = percent("10");
        assertThat(CouponEvaluator.evaluate(c, cart(), 1L, 1, NOW).valid()).isFalse();

        Coupon vip = percent("10");
        vip.setUserSpecific(true);
        User owner = new User("vip@example.com", "x", "Vip");
        ReflectionTestUtils.setField(owner, "id", 7L);
        vip.getUsers().add(owner);
        assertThat(CouponEvaluator.evaluate(vip, cart(), 8L, 0, NOW).valid()).isFalse();
        assertThat(CouponEvaluator.evaluate(vip, cart(), 7L, 0, NOW).valid()).isTrue();
    }

    @Test
    void globalUsageLimitStopsRedemption() {
        Coupon c = percent("10");
        c.setUsageLimit(1);
        c.incrementUsage();
        assertThat(CouponEvaluator.evaluate(c, cart(), 1L, 0, NOW).message()).contains("usage limit");
    }
}

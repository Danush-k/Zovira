package com.zovira.coupon.service;

import com.zovira.catalog.entity.Category;
import com.zovira.coupon.entity.Coupon;
import com.zovira.coupon.entity.DiscountType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure coupon rules, free of persistence so every branch is unit-testable. The server always
 * recomputes discounts; nothing the client sends about discounts is trusted.
 */
public final class CouponEvaluator {

    /** A priced cart line as seen by the coupon engine. */
    public record Line(Long productId, Category category, BigDecimal lineTotal) {
    }

    public record Result(boolean valid, BigDecimal discount, String message, BigDecimal eligibleSubtotal) {

        static Result invalid(String message) {
            return new Result(false, BigDecimal.ZERO, message, BigDecimal.ZERO);
        }
    }

    private CouponEvaluator() {
    }

    public static Result evaluate(Coupon coupon, List<Line> lines, Long userId, long userRedemptions, Instant now) {
        if (coupon == null || !coupon.isActive()) {
            return Result.invalid("This coupon code isn't valid.");
        }
        if (now.isBefore(coupon.getStartsAt())) {
            return Result.invalid("This coupon isn't active yet.");
        }
        if (coupon.getExpiresAt() != null && !now.isBefore(coupon.getExpiresAt())) {
            return Result.invalid("This coupon has expired.");
        }
        if (!coupon.hasRemainingUses()) {
            return Result.invalid("This coupon has reached its usage limit.");
        }
        if (coupon.isUserSpecific()
                && (userId == null || coupon.getUsers().stream().noneMatch(u -> u.getId().equals(userId)))) {
            return Result.invalid("This coupon isn't available on your account.");
        }
        if (userRedemptions >= coupon.getPerUserLimit()) {
            return Result.invalid("You've already used this coupon.");
        }

        BigDecimal cartSubtotal = lines.stream().map(Line::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal eligible = eligibleSubtotal(coupon, lines);
        if (eligible.signum() == 0) {
            return Result.invalid("This coupon doesn't apply to the items in your cart.");
        }
        if (cartSubtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            BigDecimal shortBy = coupon.getMinOrderValue().subtract(cartSubtotal);
            return Result.invalid("Add items worth ₹" + shortBy.setScale(0, RoundingMode.UP).toPlainString()
                    + " more to use this coupon.");
        }

        BigDecimal discount = coupon.getDiscountType() == DiscountType.PERCENTAGE
                ? eligible.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : coupon.getDiscountValue();
        if (coupon.getMaxDiscount() != null) {
            discount = discount.min(coupon.getMaxDiscount());
        }
        discount = discount.min(eligible).setScale(2, RoundingMode.HALF_UP);
        return new Result(true, discount, coupon.getDescription(), eligible);
    }

    static BigDecimal eligibleSubtotal(Coupon coupon, List<Line> lines) {
        return sum(lines.stream().filter(l -> isEligible(coupon, l)).toList());
    }

    /** Whether a cart line falls within the coupon's scope. */
    public static boolean isEligible(Coupon coupon, Line line) {
        return switch (coupon.getScope()) {
            case ALL -> true;
            case PRODUCT -> coupon.getProducts().stream().anyMatch(p -> p.getId().equals(line.productId()));
            case CATEGORY -> inCategory(line.category(),
                    coupon.getCategories().stream().map(Category::getId).collect(Collectors.toSet()));
        };
    }

    /** A line qualifies if its category or any ancestor is one of the coupon's categories. */
    private static boolean inCategory(Category category, Set<Long> ids) {
        for (Category c = category; c != null; c = c.getParent()) {
            if (ids.contains(c.getId())) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal sum(List<Line> lines) {
        return lines.stream().map(Line::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

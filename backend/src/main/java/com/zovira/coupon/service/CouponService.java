package com.zovira.coupon.service;

import com.zovira.cart.entity.Cart;
import com.zovira.cart.repository.CartRepository;
import com.zovira.cart.service.CartPricer;
import com.zovira.cart.service.CartPricer.PricingLine;
import com.zovira.coupon.dto.CouponOffer;
import com.zovira.coupon.entity.Coupon;
import com.zovira.coupon.repository.CouponRedemptionRepository;
import com.zovira.coupon.repository.CouponRepository;
import com.zovira.order.entity.DeliveryOption;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final CartRepository carts;
    private final CartPricer pricer;
    private final Clock clock;

    public CouponService(CouponRepository coupons, CouponRedemptionRepository redemptions, CartRepository carts,
            CartPricer pricer, Clock clock) {
        this.coupons = coupons;
        this.redemptions = redemptions;
        this.carts = carts;
        this.pricer = pricer;
        this.clock = clock;
    }

    /** Public offers shown on product pages: live coupons that are not tied to specific accounts. */
    @Transactional(readOnly = true)
    public List<CouponOffer> publicOffers() {
        return coupons.findLive(clock.instant()).stream()
                .filter(c -> !c.isUserSpecific() && c.hasRemainingUses())
                .map(c -> offer(c, false, null, null))
                .toList();
    }

    /** Offers for the signed-in shopper, evaluated against their current cart, best saving first. */
    @Transactional(readOnly = true)
    public List<CouponOffer> availableFor(Long userId) {
        Cart cart = carts.findByUserId(userId).orElse(null);
        List<PricingLine> lines = cart == null ? List.of() : cart.getItems().stream()
                .map(i -> new PricingLine(i.getId(), i.getVariant().getId(), i.getQuantity(), i.getPriceAtAdd(),
                        i.isSavedForLater()))
                .toList();
        return coupons.findLive(clock.instant()).stream()
                .filter(c -> c.hasRemainingUses())
                .filter(c -> !c.isUserSpecific() || c.getUsers().stream().anyMatch(u -> u.getId().equals(userId)))
                .filter(c -> redemptions.countByCouponIdAndUserId(c.getId(), userId) < c.getPerUserLimit())
                .map(c -> {
                    var priced = pricer.price(lines, userId, c.getCode(), DeliveryOption.STANDARD);
                    var result = priced.couponResult();
                    return offer(c, result != null && result.valid(), result == null ? null : result.discount(),
                            result == null ? null : result.message());
                })
                .sorted(Comparator.comparing(CouponOffer::applicable).reversed()
                        .thenComparing(o -> o.estimatedSaving() == null ? BigDecimal.ZERO : o.estimatedSaving(),
                                Comparator.reverseOrder()))
                .toList();
    }

    private static CouponOffer offer(Coupon c, boolean applicable, BigDecimal saving, String message) {
        return new CouponOffer(c.getCode(), c.getDescription(), c.getDiscountType().name(), c.getDiscountValue(),
                c.getMinOrderValue(), c.getMaxDiscount(), c.getExpiresAt(), applicable,
                applicable ? saving : null, applicable ? null : message);
    }
}

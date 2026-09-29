package com.zovira.coupon.controller;

import com.zovira.coupon.dto.CouponOffer;
import com.zovira.coupon.service.CouponService;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "Coupons", description = "Offers available to shoppers")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/offers")
    @Operation(summary = "Public coupon offers")
    public List<CouponOffer> offers() {
        return couponService.publicOffers();
    }

    @GetMapping("/available")
    @Operation(summary = "Coupons for the signed-in shopper, checked against their cart")
    public List<CouponOffer> available(@CurrentUser AuthUser user) {
        return couponService.availableFor(user.id());
    }
}

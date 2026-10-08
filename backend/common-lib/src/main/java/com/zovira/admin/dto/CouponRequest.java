package com.zovira.admin.dto;

import com.zovira.coupon.entity.CouponScope;
import com.zovira.coupon.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CouponRequest(
        @NotBlank(message = "Enter a coupon code")
        @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "Use 4 to 20 letters and numbers") String code,
        @NotBlank(message = "Describe the offer") @Size(max = 255) String description,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin(value = "1", message = "Discount must be greater than zero") BigDecimal discountValue,
        @DecimalMin("0") BigDecimal minOrderValue,
        BigDecimal maxDiscount,
        Instant startsAt,
        Instant expiresAt,
        Integer usageLimit,
        @Min(1) int perUserLimit,
        @NotNull CouponScope scope,
        List<Long> categoryIds,
        List<Long> productIds,
        List<Long> userIds,
        boolean active) {
}

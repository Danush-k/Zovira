package com.zovira.cart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplyCouponRequest(@NotBlank(message = "Enter a coupon code") @Size(max = 40) String code) {
}

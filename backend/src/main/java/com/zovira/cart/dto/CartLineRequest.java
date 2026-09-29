package com.zovira.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartLineRequest(@NotNull Long variantId, @Min(1) @Max(10) int quantity) {
}

package com.zovira.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Change quantity and/or swap to another variant of the same product. */
public record UpdateCartItemRequest(@Min(1) @Max(10) Integer quantity, Long variantId) {
}

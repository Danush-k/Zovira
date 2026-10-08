package com.zovira.wishlist.dto;

import jakarta.validation.constraints.NotNull;

public record AddToWishlistRequest(@NotNull Long productId, Long variantId) {
}

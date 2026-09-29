package com.zovira.cart.dto;

import java.util.List;

public record CartResponse(List<CartItemResponse> items, List<CartItemResponse> savedForLater, CartSummary summary) {
}

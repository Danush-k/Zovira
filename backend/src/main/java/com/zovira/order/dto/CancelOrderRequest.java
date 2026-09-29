package com.zovira.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(@NotBlank(message = "Tell us why you're cancelling") @Size(max = 200) String reason) {
}

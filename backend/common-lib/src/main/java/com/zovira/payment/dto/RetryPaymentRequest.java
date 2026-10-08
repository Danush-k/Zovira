package com.zovira.payment.dto;

import com.zovira.order.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record RetryPaymentRequest(@NotNull PaymentMethod method) {
}

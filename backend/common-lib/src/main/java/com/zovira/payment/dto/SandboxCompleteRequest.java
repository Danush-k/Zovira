package com.zovira.payment.dto;

import com.zovira.order.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record SandboxCompleteRequest(boolean success, @NotNull PaymentMethod method) {
}

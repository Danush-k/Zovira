package com.zovira.checkout.dto;

import com.zovira.order.entity.DeliveryOption;
import com.zovira.order.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotNull(message = "Choose a delivery address") Long addressId,
        @NotNull DeliveryOption deliveryOption,
        PaymentMethod paymentMethod,
        @Size(max = 64) String idempotencyKey) {
}

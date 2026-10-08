package com.zovira.payment.dto;

import java.math.BigDecimal;

/** Everything the browser needs to open the provider's checkout for one payment attempt. */
public record PaymentIntent(
        Long paymentId,
        String provider,
        String providerOrderId,
        String keyId,
        BigDecimal amount,
        long amountInPaise,
        String currency,
        String orderNumber,
        String customerName,
        String customerEmail,
        String customerPhone) {
}

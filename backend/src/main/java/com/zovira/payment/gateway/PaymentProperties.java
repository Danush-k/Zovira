package com.zovira.payment.gateway;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "zovira.payments")
public record PaymentProperties(
        @DefaultValue("sandbox") String provider,
        @DefaultValue("INR") String currency,
        @DefaultValue("30m") Duration pendingPaymentTimeout,
        @DefaultValue Razorpay razorpay) {

    public record Razorpay(String keyId, String keySecret, String webhookSecret,
            @DefaultValue("https://api.razorpay.com/v1") String baseUrl) {
    }

    public boolean isRazorpay() {
        return "razorpay".equalsIgnoreCase(provider);
    }
}

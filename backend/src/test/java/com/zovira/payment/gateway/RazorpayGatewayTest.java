package com.zovira.payment.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RazorpayGatewayTest {

    private final RazorpayGateway gateway = new RazorpayGateway(RestClient.builder(),
            new PaymentProperties.Razorpay("rzp_test_key", "key_secret_value", "webhook_secret", "https://api.razorpay.com/v1"));

    @Test
    void acceptsOnlyTheSignatureComputedWithTheKeySecret() {
        String valid = RazorpayGateway.hmac("order_123|pay_456", "key_secret_value");

        assertThat(gateway.verifyPaymentSignature("order_123", "pay_456", valid)).isTrue();
        assertThat(gateway.verifyPaymentSignature("order_123", "pay_999", valid)).isFalse();
        assertThat(gateway.verifyPaymentSignature("order_123", "pay_456", RazorpayGateway.hmac("order_123|pay_456", "wrong"))).isFalse();
        assertThat(gateway.verifyPaymentSignature("order_123", "pay_456", null)).isFalse();
    }

    @Test
    void verifiesWebhookBodiesWithTheWebhookSecret() {
        String body = "{\"event\":\"payment.captured\"}";
        assertThat(gateway.verifyWebhookSignature(body, RazorpayGateway.hmac(body, "webhook_secret"))).isTrue();
        assertThat(gateway.verifyWebhookSignature(body + " ", RazorpayGateway.hmac(body, "webhook_secret"))).isFalse();
    }

    @Test
    void convertsRupeesToPaiseExactly() {
        assertThat(RazorpayGateway.toPaise(new BigDecimal("1499.50"))).isEqualTo(149950);
    }

    @Test
    void refusesToStartWithoutKeys() {
        assertThatThrownBy(() -> new RazorpayGateway(RestClient.builder(),
                new PaymentProperties.Razorpay("", "", null, "https://api.razorpay.com/v1")))
                .isInstanceOf(IllegalStateException.class);
    }
}

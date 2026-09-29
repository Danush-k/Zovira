package com.zovira.payment.gateway;

import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.order.entity.Order;
import com.zovira.payment.entity.Payment;
import com.zovira.payment.entity.PaymentProvider;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Razorpay integration over its REST API. Payments are only trusted after the HMAC-SHA256
 * signature returned by Checkout (or the webhook signature) is verified with the key secret.
 */
public class RazorpayGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayGateway.class);

    private final RestClient client;
    private final PaymentProperties.Razorpay config;

    public RazorpayGateway(RestClient.Builder builder, PaymentProperties.Razorpay config) {
        if (blank(config.keyId()) || blank(config.keySecret())) {
            throw new IllegalStateException("RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET are required when PAYMENT_PROVIDER=razorpay");
        }
        this.config = config;
        this.client = builder.baseUrl(config.baseUrl())
                .defaultHeaders(h -> h.setBasicAuth(config.keyId(), config.keySecret()))
                .build();
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.RAZORPAY;
    }

    public String keyId() {
        return config.keyId();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String createOrder(Payment payment, Order order) {
        Map<String, Object> body = Map.of(
                "amount", toPaise(payment.getAmount()),
                "currency", payment.getCurrency(),
                "receipt", order.getOrderNumber(),
                "notes", Map.of("orderNumber", order.getOrderNumber()));
        try {
            Map<String, Object> response = client.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(Map.class);
            return (String) response.get("id");
        } catch (RestClientException e) {
            log.error("Razorpay order creation failed for {}", order.getOrderNumber(), e);
            throw new BusinessException(ErrorCodes.PAYMENT_FAILED, "We couldn't start the payment. Please try again.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String refund(Payment payment, BigDecimal amount) {
        try {
            Map<String, Object> response = client.post().uri("/payments/{id}/refund", payment.getProviderPaymentId())
                    .contentType(MediaType.APPLICATION_JSON).body(Map.of("amount", toPaise(amount)))
                    .retrieve().body(Map.class);
            return (String) response.get("id");
        } catch (RestClientException e) {
            log.error("Razorpay refund failed for payment {}", payment.getProviderPaymentId(), e);
            throw new BusinessException(ErrorCodes.PAYMENT_FAILED, "The refund could not be issued by the payment provider.");
        }
    }

    /** Checkout handler signature: HMAC_SHA256(order_id + "|" + payment_id, key_secret). */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        return matches(hmac(orderId + "|" + paymentId, config.keySecret()), signature);
    }

    /** Webhook signature: HMAC_SHA256(raw request body, webhook_secret). */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        return !blank(config.webhookSecret()) && matches(hmac(rawBody, config.webhookSecret()), signature);
    }

    static String hmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }

    /** Constant-time comparison to avoid leaking signature prefixes through timing. */
    private static boolean matches(String expected, String actual) {
        return actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    static long toPaise(BigDecimal rupees) {
        return rupees.movePointRight(2).longValueExact();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}

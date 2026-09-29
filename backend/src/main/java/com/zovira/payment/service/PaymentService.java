package com.zovira.payment.service;

import com.zovira.common.exception.ApiException;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.NotFoundException;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderStatus;
import com.zovira.order.entity.PaymentMethod;
import com.zovira.order.repository.OrderRepository;
import com.zovira.payment.dto.PaymentConfigResponse;
import com.zovira.payment.dto.PaymentIntent;
import com.zovira.payment.dto.PaymentResult;
import com.zovira.payment.dto.RazorpayVerifyRequest;
import com.zovira.payment.entity.Payment;
import com.zovira.payment.entity.PaymentProvider;
import com.zovira.payment.gateway.PaymentGateway;
import com.zovira.payment.gateway.RazorpayGateway;
import com.zovira.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentGateway gateway;
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final OrderPaymentHandler handler;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentGateway gateway, PaymentRepository payments, OrderRepository orders,
            OrderPaymentHandler handler, ObjectMapper objectMapper) {
        this.gateway = gateway;
        this.payments = payments;
        this.orders = orders;
        this.handler = handler;
        this.objectMapper = objectMapper;
    }

    public PaymentConfigResponse config() {
        boolean razorpay = gateway instanceof RazorpayGateway;
        return new PaymentConfigResponse(gateway.provider().name(),
                razorpay ? ((RazorpayGateway) gateway).keyId() : null, !razorpay,
                List.of("UPI", "CARD", "NETBANKING", "WALLET", "COD"));
    }

    /** Opens a new payment attempt for an order awaiting payment. */
    @Transactional
    public PaymentIntent initiate(Order order, PaymentMethod method) {
        Payment payment = payments.save(new Payment(order, order.getUser().getId(), gateway.provider(), method,
                order.getTotalAmount()));
        payment.setProviderOrderId(gateway.createOrder(payment, order));
        return intent(payment, order);
    }

    @Transactional
    public PaymentIntent retry(Long userId, String orderNumber, PaymentMethod method) {
        if (method == PaymentMethod.COD) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "Choose an online payment method to retry");
        }
        Order order = orders.findByOrderNumberAndUserId(orderNumber, userId).orElseThrow(() -> NotFoundException.of("Order"));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCodes.INVALID_STATE, "This order is not awaiting payment");
        }
        return initiate(order, method);
    }

    /** Browser callback after Razorpay Checkout succeeds; trusted only if the signature verifies. */
    @Transactional
    public PaymentResult verifyRazorpay(Long userId, RazorpayVerifyRequest request) {
        if (!(gateway instanceof RazorpayGateway razorpay)) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "Razorpay is not enabled");
        }
        Payment payment = payments.findByProviderAndProviderOrderId(PaymentProvider.RAZORPAY, request.razorpayOrderId())
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> NotFoundException.of("Payment"));
        if (!razorpay.verifyPaymentSignature(request.razorpayOrderId(), request.razorpayPaymentId(),
                request.razorpaySignature())) {
            log.warn("Rejected Razorpay signature for order {}", request.razorpayOrderId());
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.PAYMENT_FAILED, "Payment verification failed");
        }
        handler.captured(payment, request.razorpayPaymentId());
        return result(payment.getOrder());
    }

    /** Server-to-server confirmation; covers users who close the tab before the browser callback. */
    @Transactional
    public void handleRazorpayWebhook(String rawBody, String signature) {
        if (!(gateway instanceof RazorpayGateway razorpay) || !razorpay.verifyWebhookSignature(rawBody, signature)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Invalid webhook signature");
        }
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText();
            JsonNode entity = root.path("payload").path("payment").path("entity");
            String orderId = entity.path("order_id").asText(null);
            String paymentId = entity.path("id").asText(null);
            if (orderId == null) {
                return;
            }
            payments.findByProviderAndProviderOrderId(PaymentProvider.RAZORPAY, orderId).ifPresent(payment -> {
                switch (event) {
                    case "payment.captured", "order.paid" -> handler.captured(payment, paymentId);
                    case "payment.failed" -> handler.failed(payment, paymentId,
                            entity.path("error_description").asText("Payment failed"));
                    default -> log.debug("Ignoring Razorpay event {}", event);
                }
            });
        } catch (java.io.IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Malformed webhook payload");
        }
    }

    /** Test-mode completion from the sandbox payment screen. Rejected unless the sandbox gateway is active. */
    @Transactional
    public PaymentResult completeSandbox(Long userId, Long paymentId, boolean success, PaymentMethod method) {
        if (gateway.provider() != PaymentProvider.SANDBOX) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.RESOURCE_NOT_FOUND, "Not found");
        }
        Payment payment = payments.findById(paymentId).filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> NotFoundException.of("Payment"));
        payment.setMethod(method);
        if (success) {
            handler.captured(payment, "sbx_pay_" + paymentId);
        } else {
            handler.failed(payment, null, "Payment declined in test mode");
        }
        return result(payment.getOrder());
    }

    private PaymentIntent intent(Payment payment, Order order) {
        return new PaymentIntent(payment.getId(), payment.getProvider().name(), payment.getProviderOrderId(),
                gateway instanceof RazorpayGateway r ? r.keyId() : null, payment.getAmount(),
                payment.getAmount().movePointRight(2).longValueExact(), payment.getCurrency(), order.getOrderNumber(),
                order.getShippingAddress().getName(), order.getUser().getEmail(), order.getShippingAddress().getPhone());
    }

    private static PaymentResult result(Order order) {
        return new PaymentResult(order.getOrderNumber(), order.getStatus().name(), order.getPaymentStatus().name());
    }
}

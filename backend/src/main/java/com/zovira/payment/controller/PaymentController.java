package com.zovira.payment.controller;

import com.zovira.payment.dto.PaymentConfigResponse;
import com.zovira.payment.dto.PaymentIntent;
import com.zovira.payment.dto.PaymentResult;
import com.zovira.payment.dto.RazorpayVerifyRequest;
import com.zovira.payment.dto.RetryPaymentRequest;
import com.zovira.payment.dto.SandboxCompleteRequest;
import com.zovira.payment.service.PaymentService;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment configuration, verification, retries and provider webhooks")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/config")
    @Operation(summary = "Active payment provider and public key for the browser checkout")
    public PaymentConfigResponse config() {
        return paymentService.config();
    }

    @PostMapping("/razorpay/verify")
    @Operation(summary = "Verify a Razorpay Checkout payment signature and confirm the order")
    public PaymentResult verify(@CurrentUser AuthUser user, @Valid @RequestBody RazorpayVerifyRequest request) {
        return paymentService.verifyRazorpay(user.id(), request);
    }

    @PostMapping("/webhooks/razorpay")
    @Operation(summary = "Razorpay webhook (signature verified)")
    public ResponseEntity<Void> webhook(@RequestBody String body,
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signature) {
        paymentService.handleRazorpayWebhook(body, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{orderNumber}/retry")
    @Operation(summary = "Start a new payment attempt for an unpaid order")
    public PaymentIntent retry(@CurrentUser AuthUser user, @PathVariable String orderNumber,
            @Valid @RequestBody RetryPaymentRequest request) {
        return paymentService.retry(user.id(), orderNumber, request.method());
    }

    @PostMapping("/sandbox/{paymentId}/complete")
    @Operation(summary = "Complete a test-mode payment (sandbox gateway only)")
    public PaymentResult completeSandbox(@CurrentUser AuthUser user, @PathVariable Long paymentId,
            @Valid @RequestBody SandboxCompleteRequest request) {
        return paymentService.completeSandbox(user.id(), paymentId, request.success(), request.method());
    }
}

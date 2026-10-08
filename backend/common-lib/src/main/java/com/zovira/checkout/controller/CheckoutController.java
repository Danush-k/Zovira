package com.zovira.checkout.controller;

import com.zovira.checkout.dto.CheckoutPreview;
import com.zovira.checkout.dto.CheckoutRequest;
import com.zovira.checkout.dto.PlaceOrderResponse;
import com.zovira.checkout.service.CheckoutService;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Checkout", description = "Order preview and placement")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/api/v1/checkout/preview")
    @Operation(summary = "Totals, delivery dates and payment options for an address and delivery choice")
    public CheckoutPreview preview(@CurrentUser AuthUser user, @Valid @RequestBody CheckoutRequest request) {
        return checkoutService.preview(user.id(), request);
    }

    @PostMapping("/api/v1/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place an order from the cart (Idempotency-Key header prevents duplicates)")
    public PlaceOrderResponse place(@CurrentUser AuthUser user, @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        CheckoutRequest effective = idempotencyKey == null ? request
                : new CheckoutRequest(request.addressId(), request.deliveryOption(), request.paymentMethod(),
                        idempotencyKey.length() > 64 ? idempotencyKey.substring(0, 64) : idempotencyKey);
        return checkoutService.place(user.id(), effective);
    }
}

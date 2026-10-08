package com.zovira.checkout.dto;

import com.zovira.payment.dto.PaymentIntent;

public record PlaceOrderResponse(String orderNumber, String status, boolean paymentRequired, PaymentIntent payment) {
}

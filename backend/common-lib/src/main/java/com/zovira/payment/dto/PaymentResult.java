package com.zovira.payment.dto;

public record PaymentResult(String orderNumber, String orderStatus, String paymentStatus) {
}

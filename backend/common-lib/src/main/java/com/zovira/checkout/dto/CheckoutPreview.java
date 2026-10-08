package com.zovira.checkout.dto;

import com.zovira.cart.dto.CartResponse;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CheckoutPreview(
        CartResponse cart,
        LocalDate standardDeliveryDate,
        LocalDate expressDeliveryDate,
        boolean expressAvailable,
        boolean serviceable,
        boolean codAvailable,
        String codUnavailableReason,
        BigDecimal codFee,
        boolean emailVerified) {
}

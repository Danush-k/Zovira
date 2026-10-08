package com.zovira.delivery.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DeliveryEstimate(
        String pincode,
        boolean serviceable,
        String zone,
        Option standard,
        Option express,
        boolean codAvailable) {

    public record Option(boolean available, LocalDate date, int days, BigDecimal fee, BigDecimal freeAbove) {
    }
}

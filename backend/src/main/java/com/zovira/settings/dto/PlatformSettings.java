package com.zovira.settings.dto;

import java.math.BigDecimal;

/** Typed view over the operator-editable {@code platform_settings} table. */
public record PlatformSettings(
        BigDecimal freeShippingThreshold,
        BigDecimal standardShippingFee,
        BigDecimal expressShippingFee,
        int standardDeliveryDays,
        int expressDeliveryDays,
        boolean codEnabled,
        BigDecimal codFee,
        BigDecimal codMaxOrder,
        int maxQuantityPerItem,
        boolean reviewsAutoPublish) {
}

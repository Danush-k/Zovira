package com.zovira.payment.dto;

import java.util.List;

public record PaymentConfigResponse(String provider, String keyId, boolean sandbox, List<String> methods) {
}

package com.zovira.payment.gateway;

import com.zovira.common.util.SecureTokens;
import com.zovira.order.entity.Order;
import com.zovira.payment.entity.Payment;
import com.zovira.payment.entity.PaymentProvider;
import java.math.BigDecimal;

/**
 * Local development gateway. It never moves money: the storefront shows a clearly labelled test
 * payment screen and the outcome is reported back to the API, exercising the same capture,
 * failure and refund paths as a real provider.
 */
public class SandboxGateway implements PaymentGateway {

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.SANDBOX;
    }

    @Override
    public String createOrder(Payment payment, Order order) {
        return "sbx_order_" + SecureTokens.generate().substring(0, 16);
    }

    @Override
    public String refund(Payment payment, BigDecimal amount) {
        return "sbx_rfnd_" + SecureTokens.generate().substring(0, 16);
    }
}

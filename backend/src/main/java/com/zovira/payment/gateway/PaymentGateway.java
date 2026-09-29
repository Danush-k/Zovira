package com.zovira.payment.gateway;

import com.zovira.order.entity.Order;
import com.zovira.payment.entity.Payment;
import com.zovira.payment.entity.PaymentProvider;
import java.math.BigDecimal;

/** An online payment provider. Card and bank details never touch Zovira's servers. */
public interface PaymentGateway {

    PaymentProvider provider();

    /** Creates the provider-side order the browser checkout will pay against; returns its id. */
    String createOrder(Payment payment, Order order);

    /** Refunds part or all of a captured payment; returns the provider's refund id. */
    String refund(Payment payment, BigDecimal amount);
}

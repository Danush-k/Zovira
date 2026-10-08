package com.zovira.payment.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PaymentGatewayConfig {

    @Bean
    PaymentGateway paymentGateway(PaymentProperties properties, RestClient.Builder restClient) {
        return properties.isRazorpay() ? new RazorpayGateway(restClient, properties.razorpay()) : new SandboxGateway();
    }
}

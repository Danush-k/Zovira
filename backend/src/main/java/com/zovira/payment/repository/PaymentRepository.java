package com.zovira.payment.repository;

import com.zovira.payment.entity.Payment;
import com.zovira.payment.entity.PaymentProvider;
import com.zovira.payment.entity.PaymentTransactionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByProviderAndProviderOrderId(PaymentProvider provider, String providerOrderId);

    List<Payment> findByOrderIdOrderByIdDesc(Long orderId);

    Optional<Payment> findFirstByOrderIdAndStatusOrderByIdDesc(Long orderId, PaymentTransactionStatus status);
}

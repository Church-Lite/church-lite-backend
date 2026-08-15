package com.smartverse.churchlitebackend.repository.payment;

import com.smartverse.churchlitebackend_gen.entities.SubscriptionPaymentEntity;
import com.smartverse.churchlitebackend_gen.enums.BillingCycle;
import com.smartverse.churchlitebackend_gen.enums.PaymentStatus;
import com.smartverse.churchlitebackend_gen.repositories.SubscriptionPaymentRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Primary
@Repository
public interface SubscriptionPaymentCustomRepository extends SubscriptionPaymentRepository {
    Optional<SubscriptionPaymentEntity> findFirstByTenantAndPlanCodeAndBillingCycleAndStatusOrderByCreatedAtDesc(
            String tenant, String planCode, BillingCycle billingCycle, PaymentStatus status);

    List<SubscriptionPaymentEntity> findAllByTenantOrderByCreatedAtDesc(String tenant);

    List<SubscriptionPaymentEntity> findAllByTenantAndStatusOrderByCoverageEndAtDesc(
            String tenant, PaymentStatus status);
}

package com.smartverse.churchlitebackend.repository.payment;

import com.smartverse.churchlitebackend_gen.entities.PaymentEventInboxEntity;
import com.smartverse.churchlitebackend_gen.repositories.PaymentEventInboxRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Primary
@Repository
public interface PaymentEventInboxCustomRepository extends PaymentEventInboxRepository {
    Optional<PaymentEventInboxEntity> findByPaymentId(UUID paymentId);

    Optional<PaymentEventInboxEntity> findByTransactionNsu(String transactionNsu);

    List<PaymentEventInboxEntity> findAllByStatus(String status);
}

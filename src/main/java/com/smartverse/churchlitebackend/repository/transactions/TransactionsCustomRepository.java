package com.smartverse.churchlitebackend.repository.transactions;

import com.smartverse.churchlitebackend_gen.entities.FinancialEntity;
import com.smartverse.churchlitebackend_gen.entities.TransactionsEntity;
import com.smartverse.churchlitebackend_gen.repositories.TransactionsRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

@Primary
@Repository
public interface TransactionsCustomRepository extends TransactionsRepository {
    Optional<TransactionsEntity> findByFinancial(FinancialEntity financial);
    List<TransactionsEntity> findByCashId(UUID cashId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TransactionsEntity> findByTransferIdOrderByTransactionOperation(UUID transferId);
    List<TransactionsEntity> findByTransferIdIsNotNullOrderByDateTransactionDescTransferIdDesc();
}

package com.smartverse.churchlitebackend.repository.transactions;

import com.smartverse.churchlitebackend_gen.entities.FinancialEntity;
import com.smartverse.churchlitebackend_gen.entities.TransactionsEntity;
import com.smartverse.churchlitebackend_gen.repositories.TransactionsRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Primary
@Repository
public interface TransactionsCustomRepository extends TransactionsRepository {
    Optional<TransactionsEntity> findByFinancial(FinancialEntity financial);
}

package com.smartverse.churchlitebackend.repository.transactions;

import com.smartverse.churchlitebackend_gen.FinancialEntity;
import com.smartverse.churchlitebackend_gen.TransactionsEntity;
import com.smartverse.churchlitebackend_gen.TransactionsRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Primary
@Repository
public interface TransactionsCustomRepository extends TransactionsRepository {
    Optional<TransactionsEntity> findByFinancial(FinancialEntity financial);
}

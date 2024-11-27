package com.smartverse.churchlitebackend.repository.cashtransactions;

import com.smartverse.churchlitebackend_gen.CashEntity;
import com.smartverse.churchlitebackend_gen.CashTransactionsEntity;
import com.smartverse.churchlitebackend_gen.CashTransactionsRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Primary
public interface CashTransactionsCustomRepository extends CashTransactionsRepository {

    @Query(nativeQuery = true, value = " select exists (select  * from cash_transactions WHERE cash = ?1 and end_date is null) ")
    boolean existsByOpening(UUID id);

    @Query(nativeQuery = true, value = "SELECT ID FROM cash_transactions WHERE end_date is null and cash = ?1")
    UUID getLastOpeningId(UUID id);

    @Query(nativeQuery = true, value = "select sum(transactions.value) as valor, type_financial " +
            "from transactions " +
            "    inner join financial on financial.id = transactions.financial " +
            "WHERE cash_transaction = ?1 " +
            "group by financial.type_financial ")
    Optional<List<Map<String, Object>>> getValuesCash(UUID cashTransaction);

    @Query(nativeQuery = true, value = "select initial_balance " +
            "from cash_transactions " +
            "    inner join transactions on transactions.cash_transaction = cash_transactions.id " +
            "WHERE cash_transactions.id = ?1 " +
            "limit 1")
    Double getInitialBalance(UUID id);

    @Query(nativeQuery = true, value = "select sum(transactions.value) as valor, type_financial " +
            "from transactions " +
            "    inner join financial on financial.id = transactions.financial " +
            "WHERE financial.cash = ?1 " +
            "group by financial.type_financial ")
    Optional<List<Map<String, Object>>> getbalanceBankAccount(UUID cashTransaction);

    Optional<CashTransactionsEntity> findByCashAndEndDate(CashEntity cash, Date endDate);
}

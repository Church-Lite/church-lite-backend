package com.smartverse.churchlitebackend.service.transactions;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.repository.cashtransactions.CashTransactionsCustomRepository;
import com.smartverse.churchlitebackend.repository.transactions.TransactionsCustomRepository;
import com.smartverse.churchlitebackend_gen.entities.CashEntity;
import com.smartverse.churchlitebackend_gen.entities.FinancialEntity;
import com.smartverse.churchlitebackend_gen.entities.TransactionsEntity;
import com.smartverse.churchlitebackend_gen.enums.TransactionOperation;
import com.smartverse.churchlitebackend_gen.enums.TypeCash;
import com.smartverse.churchlitebackend_gen.enums.TypeFinancial;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TransactionsBusinessService {

    @Autowired
    private TransactionsCustomRepository transactionsRepository;

    @Autowired
    private CashTransactionsCustomRepository cashTransactionsCustomRepository;

    public void save(FinancialEntity entity) {

        if(entity.getPaymentReceiptDate() != null){
            verifyStatusCash(entity.getCash());
            var transaction = new TransactionsEntity();
            transaction.setPerson(entity.getPerson());
            transaction.setFinancial(entity);
            transaction.setDescription("LANÇAMENTO: " + entity.getPlanAccount().getDescription().toUpperCase());
            transaction.setDateTransaction(entity.getIssueDate());
            transaction.setValue(entity.getValue());

            if(entity.getTypeFinancial() == TypeFinancial.REVENUE){
                transaction.setTransactionOperation(TransactionOperation.REVENUE);
            } else{
                transaction.setTransactionOperation(TransactionOperation.EXPENSE);
            }
            transaction.setCashTransaction(cashTransactionsCustomRepository.getLastOpeningId(entity.getCash().getId()));

            transactionsRepository.save(transaction);
        }
    }

    public void update(FinancialEntity entity) {

        var transaction = transactionsRepository.findByFinancial(entity).orElse(null);

        if(transaction != null){

            if(entity.getPaymentReceiptDate() != null){
                verifyStatusCash(entity.getCash());
                transaction.setPerson(entity.getPerson());
                transaction.setFinancial(entity);
                transaction.setValue(entity.getValue());
                transactionsRepository.save(transaction);
            } else {
                transactionsRepository.delete(transaction);
            }

        } else{
            this.save(entity);
        }
    }

    public void delete(FinancialEntity entity) {
        if (entity != null && entity.getCash() != null) verifyStatusCash(entity.getCash());
        transactionsRepository.findByFinancial(entity).ifPresent(transaction -> transactionsRepository.delete(transaction));
    }

    private void verifyStatusCash(CashEntity cashEntity){
        if(cashEntity.getTypeCash() == TypeCash.CASH && cashEntity.getStatus() != TransactionOperation.OPEN_CASH){
            throw new ServiceException(HttpStatus.BAD_REQUEST, cashEntity.getStatus() == TransactionOperation.PENDING_APPROVAL ? "cash_pending_approval" : "Caixa selecionado está fechado");
        }
    }
}

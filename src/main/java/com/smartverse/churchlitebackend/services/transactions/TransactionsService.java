package com.smartverse.churchlitebackend.services.transactions;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.repository.cashtransactions.CashTransactionsCustomRepository;
import com.smartverse.churchlitebackend.repository.transactions.TransactionsCustomRepository;
import com.smartverse.churchlitebackend_gen.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TransactionsService {

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
            transaction.setDescription("LANÇAMENTO DE DUPLICATA - " + entity.getPlanAccount().getDescription().toUpperCase());
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
        transactionsRepository.findByFinancial(entity).ifPresent(transaction -> transactionsRepository.delete(transaction));
    }

    private void verifyStatusCash(CashEntity cashEntity){
        if(cashEntity.getStatus() == TransactionOperation.CLOSE_CASH){
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Caixa selecionado está fechado");
        }
    }
}

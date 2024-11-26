package com.smartverse.churchlitebackend.handlers.cashtransactions;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.repository.cashtransactions.CashTransactionsCustomRepository;
import com.smartverse.churchlitebackend_gen.GetIDCashTransaction;
import com.smartverse.churchlitebackend_gen.GetIDCashTransactionOutput;
import com.smartverse.churchlitebackend_gen.GetSumValuesCash;
import com.smartverse.churchlitebackend_gen.GetSumValuesCashOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class CashTransactionsCustomHandler implements GetIDCashTransaction, GetSumValuesCash {

    @Autowired
    private CashTransactionsCustomRepository cashTransactionsCustomRepository;

    @Override
    public ResponseEntity<GetIDCashTransactionOutput> getIDCashTransaction(UUID cash) {
        var output = new GetIDCashTransactionOutput();
        output.cashTransaction = cashTransactionsCustomRepository.getLastOpeningId(cash);
        if(output.cashTransaction == null) {
            throw new ServiceException(HttpStatus.NOT_FOUND,"Caixa não encontrado");
        }
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<GetSumValuesCashOutput> getSumValuesCash(UUID cashTransaction) {
        var output = new GetSumValuesCashOutput();
        var values = cashTransactionsCustomRepository.getValuesCash(cashTransaction);
        values.ifPresent(item -> item.forEach(e -> {
            if(e.get("type_financial").equals("0")){
                output.revenues = Double.parseDouble(e.get("valor").toString());
            } else if(e.get("type_financial").equals("1")){
                output.expenses = Double.parseDouble(e.get("valor").toString());
            }
        }));
        output.initialBalance = cashTransactionsCustomRepository.getInitialBalance(cashTransaction);
        return ResponseEntity.ok(output);
    }
}

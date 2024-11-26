package com.smartverse.churchlitebackend.handlers.cashtransactions;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.repository.cashtransactions.CashTransactionsCustomRepository;
import com.smartverse.churchlitebackend_gen.CashTransactionsHandler;
import com.smartverse.churchlitebackend_gen.GetIDCashTransaction;
import com.smartverse.churchlitebackend_gen.GetIDCashTransactionOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class CashTransactionsCustomHandler implements GetIDCashTransaction {

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
}

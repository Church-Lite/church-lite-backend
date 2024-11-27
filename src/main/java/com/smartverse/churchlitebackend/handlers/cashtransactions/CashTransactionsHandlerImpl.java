package com.smartverse.churchlitebackend.handlers.cashtransactions;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.repository.cashtransactions.CashTransactionsCustomRepository;
import com.smartverse.churchlitebackend_gen.CashRepository;
import com.smartverse.churchlitebackend_gen.CashTransactionsDTO;
import com.smartverse.churchlitebackend_gen.CashTransactionsHandler;
import com.smartverse.churchlitebackend_gen.TransactionOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CashTransactionsHandlerImpl extends CashTransactionsHandler {


    @Autowired
    CashTransactionsCustomRepository cashTransactionsCustomRepository;

    @Autowired
    CashRepository cashRepository;

    @Override
    public CashTransactionsDTO save(CashTransactionsDTO obj) {
        var entity = dtoConverter.toEntity(obj, null);
        if(!cashTransactionsCustomRepository.existsByOpening(entity.getCash().getId())){
            entityManager.persist(entity);
            var cash = entity.getCash();
            cash.setStatus(TransactionOperation.OPEN_CASH);
            cashRepository.save(cash);
            return dtoConverter.toDTO(entity, null);
        }
        else if(entity.getEndDate() != null){
            entityManager.merge(entity);
            var cash = entity.getCash();
            cash.setStatus(TransactionOperation.CLOSE_CASH);
            cashRepository.save(cash);
            return dtoConverter.toDTO(entity, null);
        }
        else {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Já existe um caixa aberto");
        }

    }
}

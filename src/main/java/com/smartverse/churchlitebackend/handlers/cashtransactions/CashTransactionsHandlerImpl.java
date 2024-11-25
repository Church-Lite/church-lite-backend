package com.smartverse.churchlitebackend.handlers.cashtransactions;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.repository.cashtransactions.CashTransactionsCustomRepository;
import com.smartverse.churchlitebackend_gen.CashTransactionsDTO;
import com.smartverse.churchlitebackend_gen.CashTransactionsHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CashTransactionsHandlerImpl extends CashTransactionsHandler {


    @Autowired
    CashTransactionsCustomRepository cashTransactionsCustomRepository;

    @Override
    public CashTransactionsDTO save(CashTransactionsDTO obj) {
        var entity = dtoConverter.toEntity(obj, null);
        if(cashTransactionsCustomRepository.existsByOpening(entity.getCash().getId().toString())){
            entityManager.persist(entity);
            return dtoConverter.toDTO(entity, null);
        } else {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Já existe um caixa aberto");
        }

    }
}

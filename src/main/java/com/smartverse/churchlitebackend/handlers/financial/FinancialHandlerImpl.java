package com.smartverse.churchlitebackend.handlers.financial;

import com.smartverse.churchlitebackend.services.transactions.TransactionsService;
import com.smartverse.churchlitebackend_gen.FinancialDTO;
import com.smartverse.churchlitebackend_gen.FinancialHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FinancialHandlerImpl extends FinancialHandler {


    @Autowired
    TransactionsService financialService;

    @Override
    public FinancialDTO save(FinancialDTO obj) {
        var entity = dtoConverter.toEntity(obj, null);
        entityManager.persist(entity);
        // Inclui nas transações
        financialService.save(entity);
        return dtoConverter.toDTO(entity, null);
    }

}

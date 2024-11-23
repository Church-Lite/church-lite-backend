package com.smartverse.churchlitebackend.handlers.financial;

import com.smartverse.churchlitebackend.services.transactions.TransactionsService;
import com.smartverse.churchlitebackend_gen.FinancialDTO;
import com.smartverse.churchlitebackend_gen.FinancialHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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

    @Override
    public FinancialDTO update(FinancialDTO obj, UUID id) {
        var entity = dtoConverter.toEntity(obj, null);
        entity.setId(id);
        entityManager.merge(entity);
        // Inclui altera as transações
        financialService.update(entity);
        return dtoConverter.toDTO(entity, null);
    }

    @Override
    public void delete(UUID id) {
        var entity = repository.findById(id).orElse(null);
        repository.deleteById(id);
        financialService.delete(entity);
    }

}

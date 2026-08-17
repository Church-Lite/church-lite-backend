package com.smartverse.churchlitebackend.service.financial;

import com.smartverse.churchlitebackend.service.transactions.TransactionsBusinessService;
import com.smartverse.churchlitebackend_gen.dtos.FinancialDTO;
import com.smartverse.churchlitebackend_gen.services.FinancialService;
import com.potatotech.authorization.exception.ServiceException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FinancialBusinessService extends FinancialService {

    private final TransactionsBusinessService transactionsService;
    private final EntityManager entityManager;

    public FinancialBusinessService(
            TransactionsBusinessService transactionsService,
            EntityManager entityManager) {
        this.transactionsService = transactionsService;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public FinancialDTO save(FinancialDTO obj) {
        validatePlanAccountNature(obj);
        var entity = dtoConverter.toEntity(obj, null);
        entityManager.persist(entity);
        // Inclui nas transações
        transactionsService.save(entity);
        return dtoConverter.toDTO(entity, null);
    }

    @Override
    @Transactional
    public FinancialDTO update(FinancialDTO obj, UUID id) {
        validatePlanAccountNature(obj);
        var entity = dtoConverter.toEntity(obj, null);
        entity.setId(id);
        entityManager.merge(entity);
        // Altera nas transações
        transactionsService.update(entity);
        return dtoConverter.toDTO(entity, null);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        var entity = repository.findById(id).orElse(null);
        repository.deleteById(id);
        transactionsService.delete(entity);
    }

    private void validatePlanAccountNature(FinancialDTO obj) {
        if (obj.getPlanAccount() == null || obj.getPlanAccount().getId() == null || obj.getTypeFinancial() == null) return;
        var planAccount = entityManager.find(com.smartverse.churchlitebackend_gen.entities.PlanAccountEntity.class, obj.getPlanAccount().getId());
        if (planAccount == null || !planAccount.getFinancialNature().name().equals(obj.getTypeFinancial().name())) {
            throw new ServiceException(HttpStatus.UNPROCESSABLE_ENTITY, "financial_plan_account_nature_mismatch");
        }
    }
}

package com.smartverse.churchlitebackend.handlers.cashtransactions;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.repository.cashtransactions.CashTransactionsCustomRepository;
import com.smartverse.churchlitebackend_gen.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CashTransactionsHandlerImpl extends CashTransactionsHandler {


    @Autowired
    CashTransactionsCustomRepository cashTransactionsCustomRepository;

    @Autowired
    CashRepository cashRepository;
    @Autowired ChurchConfigurationRepository churchConfigurationRepository;
    @Autowired ChurchResponsibleUserRepository responsibleUserRepository;
    @Autowired CashClosingApprovalRepository approvalRepository;

    @Override
    @Transactional
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
            var cash = entity.getCash();
            var configuration = churchConfigurationRepository.findAll().stream().findFirst().orElse(null);
            if (configuration == null || configuration.getCashApprovalPolicy() == CashApprovalPolicy.DISABLED) {
                entityManager.merge(entity);
                cash.setStatus(TransactionOperation.CLOSE_CASH);
                cashRepository.save(cash);
                return dtoConverter.toDTO(entity, null);
            }
            var approvers = responsibleUserRepository.findAll().stream().filter(ChurchResponsibleUserEntity::isFinancialApprover).toList();
            if (approvers.isEmpty()) throw new ServiceException(HttpStatus.BAD_REQUEST, "cash_approvers_required");
            entity.setEndDate(null);
            var pending = entityManager.merge(entity);
            cash.setStatus(TransactionOperation.PENDING_APPROVAL);
            cashRepository.save(cash);
            approvalRepository.deleteAll(approvalRepository.findAll().stream().filter(item -> item.getCashTransaction().getId().equals(pending.getId())).toList());
            approvers.forEach(item -> {var approval = new CashClosingApprovalEntity(); approval.setCashTransaction(pending); approval.setApprover(item.getUserConfiguration()); approval.setApproved(false); approvalRepository.save(approval);});
            return dtoConverter.toDTO(pending, null);
        }
        else {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Já existe um caixa aberto");
        }

    }
}

package com.smartverse.churchlitebackend.service.cashtransactions;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.repository.cashtransactions.CashTransactionsCustomRepository;
import com.smartverse.churchlitebackend_gen.dtos.CashTransactionsDTO;
import com.smartverse.churchlitebackend_gen.entities.CashClosingApprovalEntity;
import com.smartverse.churchlitebackend_gen.entities.ChurchResponsibleUserEntity;
import com.smartverse.churchlitebackend_gen.enums.CashApprovalPolicy;
import com.smartverse.churchlitebackend_gen.enums.TransactionOperation;
import com.smartverse.churchlitebackend_gen.repositories.CashClosingApprovalRepository;
import com.smartverse.churchlitebackend_gen.repositories.CashRepository;
import com.smartverse.churchlitebackend_gen.repositories.ChurchConfigurationRepository;
import com.smartverse.churchlitebackend_gen.repositories.ChurchResponsibleUserRepository;
import com.smartverse.churchlitebackend_gen.services.CashTransactionsService;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashTransactionsBusinessService extends CashTransactionsService {

    private final CashTransactionsCustomRepository cashTransactionsCustomRepository;
    private final CashRepository cashRepository;
    private final ChurchConfigurationRepository churchConfigurationRepository;
    private final ChurchResponsibleUserRepository responsibleUserRepository;
    private final CashClosingApprovalRepository approvalRepository;
    private final EntityManager entityManager;

    public CashTransactionsBusinessService(
            CashTransactionsCustomRepository cashTransactionsCustomRepository,
            CashRepository cashRepository,
            ChurchConfigurationRepository churchConfigurationRepository,
            ChurchResponsibleUserRepository responsibleUserRepository,
            CashClosingApprovalRepository approvalRepository,
            EntityManager entityManager) {
        this.cashTransactionsCustomRepository = cashTransactionsCustomRepository;
        this.cashRepository = cashRepository;
        this.churchConfigurationRepository = churchConfigurationRepository;
        this.responsibleUserRepository = responsibleUserRepository;
        this.approvalRepository = approvalRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public CashTransactionsDTO save(CashTransactionsDTO obj) {
        var entity = dtoConverter.toEntity(obj, null);

        if (!cashTransactionsCustomRepository.existsByOpening(entity.getCash().getId())) {
            entityManager.persist(entity);
            var cash = entity.getCash();
            cash.setStatus(TransactionOperation.OPEN_CASH);
            cashRepository.save(cash);
            return dtoConverter.toDTO(entity, null);
        }

        if (entity.getEndDate() != null) {
            var cash = entity.getCash();
            var configuration = churchConfigurationRepository.findAll().stream().findFirst().orElse(null);

            if (configuration == null || configuration.getCashApprovalPolicy() == CashApprovalPolicy.DISABLED) {
                entityManager.merge(entity);
                cash.setStatus(TransactionOperation.CLOSE_CASH);
                cashRepository.save(cash);
                return dtoConverter.toDTO(entity, null);
            }

            var approvers = responsibleUserRepository.findAll().stream()
                    .filter(ChurchResponsibleUserEntity::isFinancialApprover)
                    .toList();
            if (approvers.isEmpty()) {
                throw new ServiceException(HttpStatus.BAD_REQUEST, "cash_approvers_required");
            }

            entity.setEndDate(null);
            var pending = entityManager.merge(entity);
            cash.setStatus(TransactionOperation.PENDING_APPROVAL);
            cashRepository.save(cash);

            var previousApprovals = approvalRepository.findAll().stream()
                    .filter(item -> item.getCashTransaction().getId().equals(pending.getId()))
                    .toList();
            approvalRepository.deleteAll(previousApprovals);

            approvers.forEach(item -> {
                var approval = new CashClosingApprovalEntity();
                approval.setCashTransaction(pending);
                approval.setApprover(item.getUserConfiguration());
                approval.setApproved(false);
                approvalRepository.save(approval);
            });

            return dtoConverter.toDTO(pending, null);
        }

        throw new ServiceException(HttpStatus.BAD_REQUEST, "Já existe um caixa aberto");
    }
}

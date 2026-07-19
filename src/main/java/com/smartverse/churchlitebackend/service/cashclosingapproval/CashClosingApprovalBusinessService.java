package com.smartverse.churchlitebackend.service.cashclosingapproval;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.config.context.RequestUserContext;
import com.smartverse.churchlitebackend_gen.dtos.CashClosingApprovalDTO;
import com.smartverse.churchlitebackend_gen.entities.CashClosingApprovalEntity;
import com.smartverse.churchlitebackend_gen.enums.CashApprovalPolicy;
import com.smartverse.churchlitebackend_gen.enums.TransactionOperation;
import com.smartverse.churchlitebackend_gen.repositories.CashRepository;
import com.smartverse.churchlitebackend_gen.repositories.CashTransactionsRepository;
import com.smartverse.churchlitebackend_gen.repositories.ChurchConfigurationRepository;
import com.smartverse.churchlitebackend_gen.repositories.UserConfigurationRepository;
import com.smartverse.churchlitebackend_gen.services.CashClosingApprovalService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class CashClosingApprovalBusinessService extends CashClosingApprovalService {

    private final UserConfigurationRepository userRepository;
    private final ChurchConfigurationRepository configurationRepository;
    private final CashTransactionsRepository cashTransactionsRepository;
    private final CashRepository cashRepository;

    public CashClosingApprovalBusinessService(
            UserConfigurationRepository userRepository,
            ChurchConfigurationRepository configurationRepository,
            CashTransactionsRepository cashTransactionsRepository,
            CashRepository cashRepository) {
        this.userRepository = userRepository;
        this.configurationRepository = configurationRepository;
        this.cashTransactionsRepository = cashTransactionsRepository;
        this.cashRepository = cashRepository;
    }

    @Override
    @Transactional
    public CashClosingApprovalDTO save(CashClosingApprovalDTO input) {
        var transactionId = input.getCashTransaction() == null ? null : input.getCashTransaction().getId();
        if (transactionId == null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "cash_closing_required");
        }

        var user = userRepository.findAll().stream()
                .filter(item -> RequestUserContext.getRequired().equals(item.getHash()))
                .findFirst()
                .orElseThrow(() -> new ServiceException(HttpStatus.FORBIDDEN, "cash_approval_not_allowed"));

        var approval = repository.findAll().stream()
                .filter(item -> item.getCashTransaction().getId().equals(transactionId)
                        && item.getApprover().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ServiceException(HttpStatus.FORBIDDEN, "cash_approval_not_allowed"));

        if (!approval.isApproved()) {
            approval.setApproved(true);
            approval.setApprovedAt(LocalDateTime.now());
            repository.save(approval);
        }

        var configuration = configurationRepository.findAll().stream().findFirst().orElseThrow();
        var approvals = repository.findAll().stream()
                .filter(item -> item.getCashTransaction().getId().equals(transactionId))
                .toList();
        var completed = configuration.getCashApprovalPolicy() == CashApprovalPolicy.ANY
                || approvals.stream().allMatch(CashClosingApprovalEntity::isApproved);

        if (completed) {
            var transaction = cashTransactionsRepository.findById(transactionId).orElseThrow();
            transaction.setEndDate(LocalDate.now());
            cashTransactionsRepository.save(transaction);

            var cash = transaction.getCash();
            cash.setStatus(TransactionOperation.CLOSE_CASH);
            cashRepository.save(cash);
        }

        return dtoConverter.toDTO(approval, null);
    }
}

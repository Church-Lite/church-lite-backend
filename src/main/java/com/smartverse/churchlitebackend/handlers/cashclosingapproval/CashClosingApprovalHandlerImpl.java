package com.smartverse.churchlitebackend.handlers.cashclosingapproval;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.config.context.RequestUserContext;
import com.smartverse.churchlitebackend_gen.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
public class CashClosingApprovalHandlerImpl extends CashClosingApprovalHandler {
    @Autowired UserConfigurationRepository userRepository;
    @Autowired ChurchConfigurationRepository configurationRepository;
    @Autowired CashTransactionsRepository cashTransactionsRepository;
    @Autowired CashRepository cashRepository;

    @Override
    @Transactional
    public CashClosingApprovalDTO save(CashClosingApprovalDTO input) {
        var transactionId = input.getCashTransaction() == null ? null : input.getCashTransaction().getId();
        if (transactionId == null) throw new ServiceException(HttpStatus.BAD_REQUEST, "cash_closing_required");
        var user = userRepository.findAll().stream().filter(item -> RequestUserContext.getRequired().equals(item.getHash())).findFirst().orElseThrow(() -> new ServiceException(HttpStatus.FORBIDDEN, "cash_approval_not_allowed"));
        var approval = repository.findAll().stream().filter(item -> item.getCashTransaction().getId().equals(transactionId) && item.getApprover().getId().equals(user.getId())).findFirst().orElseThrow(() -> new ServiceException(HttpStatus.FORBIDDEN, "cash_approval_not_allowed"));
        if (!approval.isApproved()) {approval.setApproved(true); approval.setApprovedAt(LocalDateTime.now()); repository.save(approval);}
        var configuration = configurationRepository.findAll().stream().findFirst().orElseThrow();
        var approvals = repository.findAll().stream().filter(item -> item.getCashTransaction().getId().equals(transactionId)).toList();
        var completed = configuration.getCashApprovalPolicy() == CashApprovalPolicy.ANY || approvals.stream().allMatch(CashClosingApprovalEntity::isApproved);
        if (completed) {var transaction = cashTransactionsRepository.findById(transactionId).orElseThrow(); transaction.setEndDate(LocalDate.now()); cashTransactionsRepository.save(transaction); var cash = transaction.getCash(); cash.setStatus(TransactionOperation.CLOSE_CASH); cashRepository.save(cash);}
        return dtoConverter.toDTO(approval, null);
    }
}

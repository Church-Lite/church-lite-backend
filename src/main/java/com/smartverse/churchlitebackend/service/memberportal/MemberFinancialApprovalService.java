package com.smartverse.churchlitebackend.service.memberportal;

import com.smartverse.churchlitebackend.config.context.RequestUserContext;
import com.smartverse.churchlitebackend.model.memberportal.MemberFinancialApprovalModels.SaveRequest;
import com.smartverse.churchlitebackend.model.memberportal.MemberFinancialApprovalModels.Statement;
import com.smartverse.churchlitebackend.model.memberportal.MemberFinancialApprovalModels.VoteRequest;
import com.smartverse.churchlitebackend.repository.memberportal.MemberFinancialApprovalRepository;
import com.smartverse.churchlitebackend.repository.memberportal.MemberTransparencyRepository;
import com.smartverse.churchlitebackend_gen.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.enums.MemberFinancialStatementStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class MemberFinancialApprovalService {
    private final MemberFinancialApprovalRepository repository;
    private final MemberTransparencyRepository transparencyRepository;

    public MemberFinancialApprovalService(MemberFinancialApprovalRepository repository,
                                          MemberTransparencyRepository transparencyRepository) {
        this.repository = repository;
        this.transparencyRepository = transparencyRepository;
    }

    @Transactional(readOnly = true)
    public List<com.smartverse.churchlitebackend.model.memberportal.MemberFinancialApprovalModels.CashClosing>
    availableClosings() {
        return repository.findAvailableClosings();
    }

    @Transactional
    public Statement save(SaveRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()
                || request.title().length() > 180 || request.cashClosingIds() == null
                || request.cashClosingIds().isEmpty() || request.cashClosingIds().stream().anyMatch(java.util.Objects::isNull)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "member_financial_statement_invalid");
        }
        var id = request.id() == null ? UUID.randomUUID() : request.id();
        if (request.id() == null) {
            repository.insertStatement(id, request.title().trim(), request.description(), UUID.randomUUID());
        } else {
            requireChanged(repository.updateDraft(id, request.title().trim(), request.description()));
            repository.deleteCashClosings(id);
        }
        request.cashClosingIds().forEach(cashClosingId ->
                requireChanged(repository.addCashClosing(id, cashClosingId)));
        return statement(id, null);
    }

    @Transactional
    public Statement publish(UUID id) {
        requireId(id);
        requireApprovalEnabled();
        requireChanged(repository.transition(id, MemberFinancialStatementStatus.DRAFT,
                MemberFinancialStatementStatus.OPEN, "published_at", LocalDateTime.now()));
        return statement(id, null);
    }

    @Transactional
    public Statement close(UUID id) {
        requireId(id);
        requireChanged(repository.transition(id, MemberFinancialStatementStatus.OPEN,
                MemberFinancialStatementStatus.CLOSED, "closed_at", LocalDateTime.now()));
        return statement(id, null);
    }

    @Transactional(readOnly = true)
    public List<Statement> adminList() {
        return repository.findAdminStatementIds().stream().map(id -> statement(id, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<Statement> memberList() {
        if (!transparencyRepository.isMemberApprovalEnabled()) return List.of();
        var accessId = RequestUserContext.getRequired();
        return repository.findPublishedStatementIds().stream().map(id -> statement(id, accessId)).toList();
    }

    @Transactional
    public Statement vote(UUID id, VoteRequest request) {
        requireId(id);
        if (request == null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "member_financial_statement_invalid");
        }
        requireApprovalEnabled();
        var accessId = RequestUserContext.getRequired();
        var fingerprint = fingerprint(id, accessId, repository.findAnonymousSalt(id));
        try {
            requireChanged(repository.insertVote(id, fingerprint, request.approved()));
        } catch (DuplicateKeyException exception) {
            throw new ServiceException(HttpStatus.CONFLICT, "member_financial_approval_already_voted");
        }
        return statement(id, accessId);
    }

    private Statement statement(UUID id, UUID accessId) {
        var data = repository.findStatement(id).orElseThrow(() ->
                new ServiceException(HttpStatus.NOT_FOUND, "member_financial_statement_not_found"));
        var totals = repository.findFinancialTotals(id);
        var votes = repository.findVoteTotals(id);
        var alreadyVoted = accessId != null
                && repository.hasVote(id, fingerprint(id, accessId, data.anonymousSalt()));
        var percentage = votes.votes() == 0 ? 0 : votes.approvals() * 100d / votes.votes();
        return new Statement(data.id(), data.title(), data.description(), data.status(), data.publishedAt(),
                data.closedAt(), repository.findCashClosings(id), totals.revenue(), totals.expense(),
                votes.approvals(), votes.rejections(), votes.votes(), percentage, alreadyVoted);
    }

    private void requireApprovalEnabled() {
        if (!transparencyRepository.isMemberApprovalEnabled()) {
            throw new ServiceException(HttpStatus.CONFLICT, "member_financial_approval_disabled");
        }
    }

    private void requireChanged(int changed) {
        if (changed == 0) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "member_financial_statement_invalid");
        }
    }

    private void requireId(UUID id) {
        if (id == null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "member_financial_statement_invalid");
        }
    }

    private String fingerprint(UUID statementId, UUID accessId, UUID salt) {
        try {
            var value = statementId + ":" + accessId + ":" + salt;
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create anonymous vote fingerprint", exception);
        }
    }
}

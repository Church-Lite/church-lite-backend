package com.smartverse.churchlitebackend.controller.memberportal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartverse.churchlitebackend.model.memberportal.MemberFinancialApprovalModels.SaveRequest;
import com.smartverse.churchlitebackend.service.memberportal.MemberFinancialApprovalService;
import com.smartverse.churchlitebackend_gen.dtos.MemberApprovalCashClosingDTO;
import com.smartverse.churchlitebackend_gen.dtos.MemberFinancialStatementDTO;
import com.smartverse.churchlitebackend_gen.endpoints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MemberFinancialApprovalController implements GetMemberApprovalCashClosings,
        GetMemberFinancialStatements, SaveMemberFinancialStatement, PublishMemberFinancialStatement,
        CloseMemberFinancialStatement {
    private static final TypeReference<List<MemberApprovalCashClosingDTO>> CASH_CLOSING_LIST = new TypeReference<>() {};
    private static final TypeReference<List<MemberFinancialStatementDTO>> STATEMENT_LIST = new TypeReference<>() {};

    private final MemberFinancialApprovalService service;
    private final ObjectMapper objectMapper;

    public MemberFinancialApprovalController(MemberFinancialApprovalService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<GetMemberApprovalCashClosingsOutput> getMemberApprovalCashClosings() {
        var output = new GetMemberApprovalCashClosingsOutput();
        output.cashClosings = objectMapper.convertValue(service.availableClosings(), CASH_CLOSING_LIST);
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<GetMemberFinancialStatementsOutput> getMemberFinancialStatements() {
        var output = new GetMemberFinancialStatementsOutput();
        output.statements = objectMapper.convertValue(service.adminList(), STATEMENT_LIST);
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<SaveMemberFinancialStatementOutput> saveMemberFinancialStatement(
            SaveMemberFinancialStatementInput input) {
        var statement = service.save(new SaveRequest(input.id, input.title, input.description, input.cashClosingIds));
        var output = new SaveMemberFinancialStatementOutput();
        output.statement = statement(statement);
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<PublishMemberFinancialStatementOutput> publishMemberFinancialStatement(
            PublishMemberFinancialStatementInput input) {
        var output = new PublishMemberFinancialStatementOutput();
        output.statement = statement(service.publish(input.id));
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<CloseMemberFinancialStatementOutput> closeMemberFinancialStatement(
            CloseMemberFinancialStatementInput input) {
        var output = new CloseMemberFinancialStatementOutput();
        output.statement = statement(service.close(input.id));
        return ResponseEntity.ok(output);
    }

    private MemberFinancialStatementDTO statement(Object source) {
        return objectMapper.convertValue(source, MemberFinancialStatementDTO.class);
    }
}

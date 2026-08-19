package com.smartverse.churchlitebackend.controller.memberportal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartverse.churchlitebackend.model.memberportal.MemberFinancialApprovalModels.VoteRequest;
import com.smartverse.churchlitebackend.service.memberportal.MemberFinancialApprovalService;
import com.smartverse.churchlitebackend_gen.dtos.MemberFinancialStatementDTO;
import com.smartverse.churchlitebackend_gen.endpoints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MemberFinancialApprovalPortalController implements GetMemberFinancialApprovals,
        VoteMemberFinancialApproval {
    private static final TypeReference<List<MemberFinancialStatementDTO>> STATEMENT_LIST = new TypeReference<>() {};

    private final MemberFinancialApprovalService service;
    private final ObjectMapper objectMapper;

    public MemberFinancialApprovalPortalController(MemberFinancialApprovalService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<GetMemberFinancialApprovalsOutput> getMemberFinancialApprovals() {
        var output = new GetMemberFinancialApprovalsOutput();
        output.statements = objectMapper.convertValue(service.memberList(), STATEMENT_LIST);
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<VoteMemberFinancialApprovalOutput> voteMemberFinancialApproval(
            VoteMemberFinancialApprovalInput input) {
        var output = new VoteMemberFinancialApprovalOutput();
        output.statement = objectMapper.convertValue(service.vote(input.id, new VoteRequest(input.approved)),
                MemberFinancialStatementDTO.class);
        return ResponseEntity.ok(output);
    }
}

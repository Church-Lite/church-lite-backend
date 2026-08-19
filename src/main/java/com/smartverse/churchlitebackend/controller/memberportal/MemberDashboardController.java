package com.smartverse.churchlitebackend.controller.memberportal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartverse.churchlitebackend.service.memberportal.MemberDashboardService;
import com.smartverse.churchlitebackend_gen.dtos.MemberDashboardDTO;
import com.smartverse.churchlitebackend_gen.endpoints.GetMemberDashboard;
import com.smartverse.churchlitebackend_gen.endpoints.GetMemberDashboardOutput;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MemberDashboardController implements GetMemberDashboard {
    private final MemberDashboardService service;
    private final ObjectMapper objectMapper;

    public MemberDashboardController(MemberDashboardService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<GetMemberDashboardOutput> getMemberDashboard() {
        var output = new GetMemberDashboardOutput();
        output.dashboard = objectMapper.convertValue(service.dashboard(), MemberDashboardDTO.class);
        return ResponseEntity.ok(output);
    }
}

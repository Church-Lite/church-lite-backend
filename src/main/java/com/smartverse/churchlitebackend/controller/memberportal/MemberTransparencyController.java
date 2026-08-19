package com.smartverse.churchlitebackend.controller.memberportal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartverse.churchlitebackend.model.memberportal.MemberTransparencyModels.UpdateRequest;
import com.smartverse.churchlitebackend.model.memberportal.MemberTransparencyModels.VisibilityUpdate;
import com.smartverse.churchlitebackend.service.memberportal.MemberTransparencyService;
import com.smartverse.churchlitebackend_gen.dtos.MemberFinancialPortalDTO;
import com.smartverse.churchlitebackend_gen.dtos.MemberTransparencyConfigurationDTO;
import com.smartverse.churchlitebackend_gen.endpoints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class MemberTransparencyController implements GetMemberTransparencyConfiguration,
        UpdateMemberTransparencyConfiguration, GetMemberTransparency {
    private final MemberTransparencyService service;
    private final ObjectMapper objectMapper;

    public MemberTransparencyController(MemberTransparencyService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<GetMemberTransparencyConfigurationOutput> getMemberTransparencyConfiguration() {
        var output = new GetMemberTransparencyConfigurationOutput();
        output.configuration = configuration(service.get());
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<UpdateMemberTransparencyConfigurationOutput> updateMemberTransparencyConfiguration(
            UpdateMemberTransparencyConfigurationInput input) {
        var configuration = input == null ? null : input.configuration;
        List<VisibilityUpdate> visibilities = configuration == null || configuration.planAccounts == null ? List.of()
                : configuration.planAccounts.stream()
                .map(item -> new VisibilityUpdate(item.id, item.visibility)).toList();
        var request = configuration == null ? null : new UpdateRequest(configuration.mode,
                configuration.memberApprovalEnabled, visibilities);
        var result = service.update(request);
        var output = new UpdateMemberTransparencyConfigurationOutput();
        output.configuration = configuration(result);
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<GetMemberTransparencyOutput> getMemberTransparency(LocalDate endDate, LocalDate startDate) {
        var output = new GetMemberTransparencyOutput();
        output.transparency = objectMapper.convertValue(service.financial(startDate, endDate),
                MemberFinancialPortalDTO.class);
        return ResponseEntity.ok(output);
    }

    private MemberTransparencyConfigurationDTO configuration(Object source) {
        return objectMapper.convertValue(source, MemberTransparencyConfigurationDTO.class);
    }
}

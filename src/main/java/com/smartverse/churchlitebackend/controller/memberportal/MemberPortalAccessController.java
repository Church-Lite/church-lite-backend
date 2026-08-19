package com.smartverse.churchlitebackend.controller.memberportal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartverse.churchlitebackend.model.memberportal.MemberPortalModels.RegistrationRequest;
import com.smartverse.churchlitebackend.service.memberportal.MemberPortalAccessService;
import com.smartverse.churchlitebackend_gen.dtos.MemberRegistrationContextDTO;
import com.smartverse.churchlitebackend_gen.endpoints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class MemberPortalAccessController implements GetMemberPortalLink, GetMemberPortalMemberLink,
        GetMemberPortalUserLink, SendMemberPortalLink, LinkMemberPortalUser, GetMemberRegistrationContext, RegisterMemberAccess {
    private final MemberPortalAccessService service;
    private final ObjectMapper objectMapper;

    public MemberPortalAccessController(MemberPortalAccessService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<GetMemberPortalLinkOutput> getMemberPortalLink() {
        var link = service.getOrCreateChurchLink();
        var output = new GetMemberPortalLinkOutput();
        output.churchId = link.churchId();
        output.path = link.path();
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<GetMemberPortalMemberLinkOutput> getMemberPortalMemberLink(UUID memberId) {
        var output = new GetMemberPortalMemberLinkOutput();
        output.path = service.memberLink(memberId);
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<GetMemberPortalUserLinkOutput> getMemberPortalUserLink(UUID userId) {
        var link = service.linkedMemberByUser(userId);
        var output = new GetMemberPortalUserLinkOutput();
        output.memberId = link.memberId();
        output.personId = link.personId();
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<SendMemberPortalLinkOutput> sendMemberPortalLink(SendMemberPortalLinkInput input) {
        service.sendMemberLink(input.memberId);
        var output = new SendMemberPortalLinkOutput();
        output.sent = true;
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<LinkMemberPortalUserOutput> linkMemberPortalUser(LinkMemberPortalUserInput input) {
        service.linkExistingUser(input.memberId, input.userId);
        var output = new LinkMemberPortalUserOutput();
        output.linked = true;
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<GetMemberRegistrationContextOutput> getMemberRegistrationContext(UUID memberId,
                                                                                            UUID churchId) {
        var output = new GetMemberRegistrationContextOutput();
        output.context = objectMapper.convertValue(service.context(churchId, memberId),
                MemberRegistrationContextDTO.class);
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<RegisterMemberAccessOutput> registerMemberAccess(RegisterMemberAccessInput input) {
        var request = new RegistrationRequest(input.name, input.email, input.cpf, input.phone,
                input.password, input.passwordConfirmation);
        var result = service.register(input.churchId, input.memberId, request);
        var output = new RegisterMemberAccessOutput();
        output.accepted = result.accepted();
        output.existingAccess = result.existingAccess();
        return ResponseEntity.ok(output);
    }
}

package com.smartverse.churchlitebackend.controller.memberportal;

import com.potatotech.authorization.stereotype.Anonymous;
import com.smartverse.churchlitebackend.controller.memberportal.MemberPortalModels.*;
import com.smartverse.churchlitebackend.service.memberportal.MemberPortalAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
public class MemberPortalAccessController {
    private final MemberPortalAccessService service;

    public MemberPortalAccessController(MemberPortalAccessService service) {
        this.service = service;
    }

    @GetMapping("/memberPortal/link")
    public ResponseEntity<PortalLinkResponse> link() {
        return ResponseEntity.ok(service.getOrCreateChurchLink());
    }

    @GetMapping("/memberPortal/link/{memberId}")
    public ResponseEntity<Map<String, String>> memberLink(@PathVariable UUID memberId) {
        return ResponseEntity.ok(Map.of("path", service.memberLink(memberId)));
    }

    @PostMapping("/memberPortal/send/{memberId}")
    public ResponseEntity<Void> send(@PathVariable UUID memberId) {
        service.sendMemberLink(memberId);
        return ResponseEntity.noContent().build();
    }

    @Anonymous
    @GetMapping("/member-access/{churchId}")
    public ResponseEntity<RegistrationContext> context(@PathVariable UUID churchId) {
        return ResponseEntity.ok(service.context(churchId, null));
    }

    @Anonymous
    @GetMapping("/member-access/{churchId}/{memberId}")
    public ResponseEntity<RegistrationContext> memberContext(@PathVariable UUID churchId, @PathVariable UUID memberId) {
        return ResponseEntity.ok(service.context(churchId, memberId));
    }

    @Anonymous
    @PostMapping("/member-access/{churchId}")
    public ResponseEntity<RegistrationResponse> register(@PathVariable UUID churchId, @RequestBody RegistrationRequest request) {
        return ResponseEntity.ok(service.register(churchId, null, request));
    }

    @Anonymous
    @PostMapping("/member-access/{churchId}/{memberId}")
    public ResponseEntity<RegistrationResponse> activate(@PathVariable UUID churchId, @PathVariable UUID memberId,
                                                          @RequestBody RegistrationRequest request) {
        return ResponseEntity.ok(service.register(churchId, memberId, request));
    }
}

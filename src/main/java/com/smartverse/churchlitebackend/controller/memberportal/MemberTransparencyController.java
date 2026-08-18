package com.smartverse.churchlitebackend.controller.memberportal;

import com.smartverse.churchlitebackend.controller.memberportal.MemberTransparencyModels.Configuration;
import com.smartverse.churchlitebackend.controller.memberportal.MemberTransparencyModels.UpdateRequest;
import com.smartverse.churchlitebackend.service.memberportal.MemberTransparencyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/memberPortal/transparency")
public class MemberTransparencyController {
    private final MemberTransparencyService service;

    public MemberTransparencyController(MemberTransparencyService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<Configuration> get() { return ResponseEntity.ok(service.get()); }

    @PutMapping
    public ResponseEntity<Configuration> update(@RequestBody UpdateRequest request) {
        return ResponseEntity.ok(service.update(request));
    }

}

package com.smartverse.churchlitebackend.controller.memberportal;

import com.smartverse.churchlitebackend.controller.memberportal.MemberDashboardModels.Dashboard;
import com.smartverse.churchlitebackend.service.memberportal.MemberDashboardService;
import com.smartverse.churchlitebackend.service.memberportal.MemberTransparencyService;
import com.smartverse.churchlitebackend.controller.memberportal.MemberTransparencyModels.FinancialPortal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;

@RestController
public class MemberDashboardController {
    private final MemberDashboardService service;
    private final MemberTransparencyService transparencyService;

    public MemberDashboardController(MemberDashboardService service, MemberTransparencyService transparencyService) {
        this.service = service;
        this.transparencyService = transparencyService;
    }

    @GetMapping("/member-api/dashboard")
    public ResponseEntity<Dashboard> dashboard() { return ResponseEntity.ok(service.dashboard()); }

    @GetMapping("/member-api/transparency")
    public ResponseEntity<FinancialPortal> transparency(@RequestParam(required = false) LocalDate startDate,
                                                         @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(transparencyService.financial(startDate, endDate));
    }
}

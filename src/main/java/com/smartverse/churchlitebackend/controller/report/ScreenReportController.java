package com.smartverse.churchlitebackend.controller.report;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.service.report.SmartReportGenerationService;
import com.smartverse.churchlitebackend_gen.endpoints.GenerateScreenReport;
import com.smartverse.churchlitebackend_gen.endpoints.GenerateScreenReportInput;
import com.smartverse.churchlitebackend_gen.endpoints.GenerateScreenReportOutput;
import com.smartverse.churchlitebackend_gen.endpoints.GetScreenReports;
import com.smartverse.churchlitebackend_gen.endpoints.GetScreenReportsOutput;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScreenReportController implements GetScreenReports, GenerateScreenReport {
    private final SmartReportGenerationService screenReportService;

    public ScreenReportController(SmartReportGenerationService screenReportService) {
        this.screenReportService = screenReportService;
    }

    @Override
    public ResponseEntity<GetScreenReportsOutput> getScreenReports(String screen) {
        return ResponseEntity.ok(screenReportService.list(screen));
    }

    @Override
    public ResponseEntity<GenerateScreenReportOutput> generateScreenReport(
            GenerateScreenReportInput input) {
        if (input == null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "screen_report_request_required");
        }
        return ResponseEntity.ok(screenReportService.generate(input.reportId, input.data));
    }
}

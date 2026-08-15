package com.smartverse.churchlitebackend.client.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "smartReport", url = "${smart-report.base-url}")
public interface SmartReportClient {

    @PostMapping("/generateReport")
    GenerateReportResponse generateReport(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestBody GenerateReportRequest request);

    record GenerateReportRequest(
            Map<String, Object> data,
            @JsonProperty("idreport") UUID reportId) {
    }

    record GenerateReportResponse(byte[] report) {
    }
}

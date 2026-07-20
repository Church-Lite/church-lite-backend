package com.smartverse.churchlitebackend.controller.dashboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartverse.churchlitebackend.model.dashboard.DashboardModels.FinancialFilter;
import com.smartverse.churchlitebackend_gen.enums.SubscriptionFeature;
import com.smartverse.churchlitebackend.service.dashboard.DashboardService;
import com.smartverse.churchlitebackend.service.subscription.SubscriptionService;
import com.smartverse.churchlitebackend_gen.endpoints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class DashboardHandlerImpl implements GetDashboardFinancial, GetDashboardAgenda {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;
    private final SubscriptionService subscriptionService;

    public DashboardHandlerImpl(
            DashboardService dashboardService,
            ObjectMapper objectMapper,
            SubscriptionService subscriptionService) {
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
        this.subscriptionService = subscriptionService;
    }

    @Override
    public ResponseEntity<GetDashboardFinancialOutput> getDashboardFinancial(UUID planoContaId, UUID centroCustoId, boolean somenteCaixasAbertos, UUID caixaId, UUID contaBancariaId, UUID bancoId, LocalDate dataFinal, LocalDate dataInicial) {
        subscriptionService.requireFeature(SubscriptionFeature.EXECUTIVE_DASHBOARD);
        var output = new GetDashboardFinancialOutput();
        output.data = objectMapper.convertValue(dashboardService.financial(new FinancialFilter(dataInicial, dataFinal, bancoId, contaBancariaId, caixaId, somenteCaixasAbertos, centroCustoId, planoContaId)), MAP_TYPE);
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<GetDashboardAgendaOutput> getDashboardAgenda() {
        subscriptionService.requireFeature(SubscriptionFeature.EXECUTIVE_DASHBOARD);
        var output = new GetDashboardAgendaOutput();
        output.data = objectMapper.convertValue(dashboardService.agenda(), MAP_TYPE);
        return ResponseEntity.ok(output);
    }
}

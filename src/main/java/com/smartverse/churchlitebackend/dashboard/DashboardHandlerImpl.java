package com.smartverse.churchlitebackend.dashboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartverse.churchlitebackend.dashboard.DashboardModels.FinancialFilter;
import com.smartverse.churchlitebackend_gen.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class DashboardHandlerImpl implements GetDashboardFinancial, GetDashboardAgenda {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    public DashboardHandlerImpl(DashboardService dashboardService, ObjectMapper objectMapper) {
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<GetDashboardFinancialOutput> getDashboardFinancial(UUID planoContaId,
            UUID centroCustoId, boolean somenteCaixasAbertos, UUID caixaId, UUID contaBancariaId,
            UUID bancoId, LocalDate dataFinal, LocalDate dataInicial) {
        var output = new GetDashboardFinancialOutput();
        output.data = objectMapper.convertValue(dashboardService.financial(new FinancialFilter(dataInicial,
                dataFinal, bancoId, contaBancariaId, caixaId, somenteCaixasAbertos,
                centroCustoId, planoContaId)), MAP_TYPE);
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<GetDashboardAgendaOutput> getDashboardAgenda() {
        var output = new GetDashboardAgendaOutput();
        output.data = objectMapper.convertValue(dashboardService.agenda(), MAP_TYPE);
        return ResponseEntity.ok(output);
    }
}

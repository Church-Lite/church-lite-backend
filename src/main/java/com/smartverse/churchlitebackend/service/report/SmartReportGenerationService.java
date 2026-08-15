package com.smartverse.churchlitebackend.service.report;

import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.client.report.SmartReportClient;
import com.smartverse.churchlitebackend.config.database.TenantSchemaInterceptor;
import com.smartverse.churchlitebackend.config.migration.DBMigration;
import com.smartverse.churchlitebackend.repository.report.IntegrationConfigurationCustomRepository;
import com.smartverse.churchlitebackend.repository.report.ScreenReportCustomRepository;
import com.smartverse.churchlitebackend_gen.dtos.ScreenReportOptionDTO;
import com.smartverse.churchlitebackend_gen.endpoints.GenerateScreenReportOutput;
import com.smartverse.churchlitebackend_gen.endpoints.GetScreenReportsOutput;
import com.smartverse.churchlitebackend_gen.entities.ScreenReportEntity;
import com.smartverse.churchlitebackend_gen.enums.IntegrationService;
import feign.FeignException;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class SmartReportGenerationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartReportGenerationService.class);
    private static final String ADMIN_TENANT = "admin";

    private final SmartReportClient smartReportClient;
    private final ScreenReportCustomRepository screenReportRepository;
    private final IntegrationConfigurationCustomRepository integrationRepository;
    private final IntegrationConfigurationParser configurationParser;
    private final DBMigration dbMigration;
    private final TenantSchemaInterceptor tenantSchemaInterceptor;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public SmartReportGenerationService(
            SmartReportClient smartReportClient,
            ScreenReportCustomRepository screenReportRepository,
            IntegrationConfigurationCustomRepository integrationRepository,
            IntegrationConfigurationParser configurationParser,
            DBMigration dbMigration,
            TenantSchemaInterceptor tenantSchemaInterceptor,
            EntityManager entityManager,
            TransactionTemplate transactionTemplate) {
        this.smartReportClient = smartReportClient;
        this.screenReportRepository = screenReportRepository;
        this.integrationRepository = integrationRepository;
        this.configurationParser = configurationParser;
        this.dbMigration = dbMigration;
        this.tenantSchemaInterceptor = tenantSchemaInterceptor;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
    }

    public GetScreenReportsOutput list(String screen) {
        requireTenant();
        String normalizedScreen = requireText(screen, "screen_report_screen_required");
        var output = new GetScreenReportsOutput();
        output.reports = transactionTemplate.execute(status -> screenReportRepository
                .findAllByScreenAndActiveTrueOrderByDisplayOrderAscNameAsc(normalizedScreen)
                .stream()
                .map(this::toOption)
                .toList());
        return output;
    }

    public GenerateScreenReportOutput generate(UUID reportId, Map<String, Object> data) {
        requireTenant();
        if (reportId == null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "screen_report_id_required");
        }
        if (data == null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "screen_report_data_required");
        }

        ScreenReportEntity configuredReport = transactionTemplate.execute(status -> screenReportRepository
                .findById(reportId)
                .filter(ScreenReportEntity::isActive)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "screen_report_not_found")));
        String apiKey = loadSmartReportApiKey();

        SmartReportClient.GenerateReportResponse response;
        try {
            response = smartReportClient.generateReport(
                    apiKey,
                    new SmartReportClient.GenerateReportRequest(data, configuredReport.getSmartReportId()));
        } catch (FeignException exception) {
            LOGGER.warn("SmartReport generation failed with status {}", exception.status());
            throw new ServiceException(HttpStatus.BAD_GATEWAY, "smart_report_service_unavailable");
        }
        if (response == null || response.report() == null || response.report().length == 0) {
            throw new ServiceException(HttpStatus.BAD_GATEWAY, "smart_report_invalid_response");
        }

        var output = new GenerateScreenReportOutput();
        output.report = response.report();
        return output;
    }

    private String loadSmartReportApiKey() {
        return inAdminTransaction(() -> {
            var integration = integrationRepository.findByService(IntegrationService.SMART_REPORT)
                    .orElseThrow(() -> new ServiceException(
                            HttpStatus.SERVICE_UNAVAILABLE, "smart_report_integration_not_configured"));
            var values = configurationParser.parse(integration.getConfiguration());
            String apiKey = firstPresent(values, "API_KEY", "X_API_KEY", "TOKEN");
            if (apiKey == null) {
                throw new ServiceException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "smart_report_api_key_not_configured");
            }
            return apiKey;
        });
    }

    private String firstPresent(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String value = values.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private ScreenReportOptionDTO toOption(ScreenReportEntity entity) {
        var option = new ScreenReportOptionDTO();
        option.id = entity.getId();
        option.name = entity.getName();
        option.screen = entity.getScreen();
        return option;
    }

    private String requireTenant() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.isBlank() || ADMIN_TENANT.equalsIgnoreCase(tenant)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "report_tenant_not_identified");
        }
        return tenant;
    }

    private String requireText(String value, String error) {
        if (value == null || value.isBlank()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, error);
        }
        return value.trim();
    }

    private <T> T inAdminTransaction(Supplier<T> operation) {
        String previousTenant = TenantContext.getCurrentTenant();
        try {
            selectTenant(ADMIN_TENANT);
            return transactionTemplate.execute(status -> operation.get());
        } finally {
            entityManager.clear();
            TenantContext.setCurrentTenant(previousTenant);
            if (previousTenant != null && !previousTenant.isBlank()) {
                dbMigration.loadMigrateTenants(previousTenant);
                tenantSchemaInterceptor.switchSchema();
            }
        }
    }

    private void selectTenant(String tenant) {
        entityManager.clear();
        dbMigration.loadMigrateTenants(tenant);
        TenantContext.setCurrentTenant(tenant);
        tenantSchemaInterceptor.switchSchema();
    }
}

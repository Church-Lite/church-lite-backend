package com.smartverse.churchlitebackend.service.memberportal;

import com.smartverse.churchlitebackend.model.memberportal.MemberTransparencyModels.*;
import com.smartverse.churchlitebackend.repository.memberportal.MemberTransparencyRepository;
import com.smartverse.churchlitebackend.repository.memberportal.MemberTransparencyRepository.FinancialRow;
import com.smartverse.churchlitebackend_gen.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.enums.MemberPortalPlanVisibility;
import com.smartverse.churchlitebackend_gen.enums.MemberPortalTransparencyMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MemberTransparencyService {
    private final MemberTransparencyRepository repository;

    public MemberTransparencyService(MemberTransparencyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Configuration get() {
        var settings = repository.findSettings();
        return new Configuration(settings.mode(), settings.memberApprovalEnabled(),
                repository.findPlanAccountVisibilities());
    }

    @Transactional
    public Configuration update(UpdateRequest request) {
        if (request == null || request.mode() == null) invalid();
        repository.saveSettings(request.mode(), request.memberApprovalEnabled());

        if (request.mode() == MemberPortalTransparencyMode.PARTIAL) {
            var values = Objects.requireNonNullElse(request.planAccounts(), List.<VisibilityUpdate>of());
            for (var item : values) {
                if (item == null || item.id() == null || item.visibility() == null) invalid();
                var changed = repository.saveVisibility(item.id(), item.visibility());
                if (changed == 0) invalid();
            }
        }
        return get();
    }

    @Transactional(readOnly = true)
    public FinancialPortal financial(LocalDate requestedStart, LocalDate requestedEnd) {
        var configuration = get();
        var end = requestedEnd == null ? LocalDate.now() : requestedEnd;
        var start = requestedStart == null ? end.withDayOfYear(1) : requestedStart;
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) > 366) invalid();
        if (configuration.mode() == MemberPortalTransparencyMode.DISABLED) {
            return new FinancialPortal(false, MemberPortalTransparencyMode.DISABLED,
                    start, end, 0, 0, List.of(), List.of());
        }

        Map<UUID, PlanAccountVisibility> configured = new LinkedHashMap<>();
        configuration.planAccounts().forEach(account -> configured.put(account.id(), account));
        var rows = repository.findFinancialRows(start, end);

        Map<UUID, MutableSummary> summaries = new LinkedHashMap<>();
        List<ExpenseDetail> details = new ArrayList<>();
        double revenue = 0, expense = 0;
        for (var row : rows) {
            var account = row.planId() == null ? null : configured.get(row.planId());
            var visibility = configuration.mode() == MemberPortalTransparencyMode.FULL
                    ? MemberPortalPlanVisibility.DETAILED
                    : account == null ? MemberPortalPlanVisibility.HIDDEN : account.visibility();
            if (visibility == MemberPortalPlanVisibility.HIDDEN) continue;
            var nature = row.type() == 0 ? "REVENUE" : "EXPENSE";
            if (row.type() == 0) revenue += row.value(); else expense += row.value();
            if (row.planId() != null) {
                summaries.computeIfAbsent(row.planId(), id -> new MutableSummary(row.planId(), row.codeTree(),
                        row.planDescription(), nature, visibility)).total += row.value();
            }
            if (row.type() == 1 && visibility == MemberPortalPlanVisibility.DETAILED) {
                details.add(new ExpenseDetail(row.id(), row.date(), row.description(), row.planId(),
                        row.planDescription() == null ? "Sem classificação" : row.planDescription(), row.value()));
            }
        }
        var accounts = summaries.values().stream().map(item -> new AccountSummary(item.id, item.codeTree,
                item.description, item.nature, item.visibility, item.total)).toList();
        return new FinancialPortal(true, configuration.mode(), start, end, revenue, expense, accounts, details);
    }

    private static final class MutableSummary {
        private final UUID id; private final String codeTree; private final String description;
        private final String nature; private final MemberPortalPlanVisibility visibility; private double total;
        private MutableSummary(UUID id, String codeTree, String description, String nature,
                               MemberPortalPlanVisibility visibility) {
            this.id = id; this.codeTree = codeTree; this.description = description;
            this.nature = nature; this.visibility = visibility;
        }
    }

    private void invalid() {
        throw new ServiceException(HttpStatus.BAD_REQUEST, "member_transparency_invalid_configuration");
    }
}

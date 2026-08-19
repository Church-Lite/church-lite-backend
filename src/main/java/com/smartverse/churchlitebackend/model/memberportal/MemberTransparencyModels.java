package com.smartverse.churchlitebackend.model.memberportal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.smartverse.churchlitebackend_gen.enums.MemberPortalPlanVisibility;
import com.smartverse.churchlitebackend_gen.enums.MemberPortalTransparencyMode;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class MemberTransparencyModels {
    private MemberTransparencyModels() {
    }

    public record Configuration(MemberPortalTransparencyMode mode, boolean memberApprovalEnabled,
                                List<PlanAccountVisibility> planAccounts) {
    }

    public record PlanAccountVisibility(UUID id, String codeTree, String description,
                                        String financialNature, MemberPortalPlanVisibility visibility) {
    }

    public record UpdateRequest(@NotNull MemberPortalTransparencyMode mode, boolean memberApprovalEnabled,
                                List<@Valid VisibilityUpdate> planAccounts) {
    }

    public record VisibilityUpdate(@NotNull UUID id, @NotNull MemberPortalPlanVisibility visibility) {
    }

    public record FinancialPortal(boolean enabled, MemberPortalTransparencyMode mode,
                                  LocalDate startDate, LocalDate endDate,
                                  double revenueTotal, double expenseTotal,
                                  List<AccountSummary> accounts, List<ExpenseDetail> expenses) {
    }

    public record AccountSummary(UUID planAccountId, String codeTree, String description,
                                 String financialNature, MemberPortalPlanVisibility visibility, double total) {
    }

    public record ExpenseDetail(UUID id, LocalDate date, String description,
                                UUID planAccountId, String planAccount, double value) {
    }
}

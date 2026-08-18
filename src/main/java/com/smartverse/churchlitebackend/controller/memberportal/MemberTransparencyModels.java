package com.smartverse.churchlitebackend.controller.memberportal;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

public final class MemberTransparencyModels {
    private MemberTransparencyModels() {}

    public enum Mode { DISABLED, FULL, PARTIAL }
    public enum Visibility { HIDDEN, TOTAL_ONLY, DETAILED }

    public record Configuration(Mode mode, boolean memberApprovalEnabled, List<PlanAccountVisibility> planAccounts) {}
    public record PlanAccountVisibility(UUID id, String codeTree, String description,
                                        String financialNature, Visibility visibility) {}
    public record UpdateRequest(Mode mode, boolean memberApprovalEnabled, List<VisibilityUpdate> planAccounts) {}
    public record VisibilityUpdate(UUID id, Visibility visibility) {}

    public record FinancialPortal(boolean enabled, Mode mode, LocalDate startDate, LocalDate endDate,
                                  double revenueTotal, double expenseTotal,
                                  List<AccountSummary> accounts, List<ExpenseDetail> expenses) {}
    public record AccountSummary(UUID planAccountId, String codeTree, String description,
                                 String financialNature, Visibility visibility, double total) {}
    public record ExpenseDetail(UUID id, LocalDate date, String description,
                                UUID planAccountId, String planAccount, double value) {}
}

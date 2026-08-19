package com.smartverse.churchlitebackend.model.memberportal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.smartverse.churchlitebackend_gen.enums.MemberFinancialStatementStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class MemberFinancialApprovalModels {
    private MemberFinancialApprovalModels() {
    }

    public record CashClosing(UUID id, String cash, LocalDate startDate, LocalDate endDate, double finalBalance) {
    }

    public record SaveRequest(UUID id,
                              @NotBlank @Size(max = 180) String title,
                              String description,
                              @NotEmpty List<@NotNull UUID> cashClosingIds) {
    }

    public record Statement(UUID id, String title, String description, MemberFinancialStatementStatus status,
                            LocalDateTime publishedAt,
                            LocalDateTime closedAt, List<CashClosing> cashClosings, double revenue, double expense,
                            long approvals, long rejections, long votes, double approvalPercentage,
                            boolean alreadyVoted) {
    }

    public record VoteRequest(boolean approved) {
    }
}

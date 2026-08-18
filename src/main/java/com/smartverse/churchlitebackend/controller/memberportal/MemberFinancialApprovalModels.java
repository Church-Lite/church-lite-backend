package com.smartverse.churchlitebackend.controller.memberportal;
import java.time.LocalDate;import java.time.LocalDateTime;import java.util.List;import java.util.UUID;
public final class MemberFinancialApprovalModels {private MemberFinancialApprovalModels(){}
 public record CashClosing(UUID id,String cash,LocalDate startDate,LocalDate endDate,double finalBalance){}
 public record SaveRequest(UUID id,String title,String description,List<UUID> cashClosingIds){}
 public record Statement(UUID id,String title,String description,String status,LocalDateTime publishedAt,LocalDateTime closedAt,List<CashClosing> cashClosings,double revenue,double expense,long approvals,long rejections,long votes,double approvalPercentage,boolean alreadyVoted){}
 public record VoteRequest(boolean approved){}
}

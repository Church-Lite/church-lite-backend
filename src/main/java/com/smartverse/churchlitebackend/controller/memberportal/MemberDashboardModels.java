package com.smartverse.churchlitebackend.controller.memberportal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class MemberDashboardModels {
    private MemberDashboardModels() {}

    public record Dashboard(String memberName, double monthTotal, double yearTotal,
                            List<Event> upcomingEvents, List<Contribution> recentContributions) {}

    public record Event(UUID id, String name, String color, LocalDateTime initialDate,
                        LocalDateTime finalDate, String local) {}

    public record Contribution(UUID id, String description, String planAccount,
                               LocalDate date, double value) {}
}

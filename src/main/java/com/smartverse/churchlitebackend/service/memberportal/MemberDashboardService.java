package com.smartverse.churchlitebackend.service.memberportal;

import com.smartverse.churchlitebackend.config.context.RequestUserContext;
import com.smartverse.churchlitebackend.controller.memberportal.MemberDashboardModels.*;
import com.smartverse.churchlitebackend.repository.memberportal.MemberDashboardRepository;
import com.smartverse.churchlitebackend.repository.memberportal.MemberContributionRepository;
import com.smartverse.churchlitebackend.repository.memberportal.MemberEventRepository;
import com.smartverse.churchlitebackend_gen.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.enums.AppointmentStatus;
import com.smartverse.churchlitebackend_gen.enums.TypeFinancial;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class MemberDashboardService {
    private final MemberDashboardRepository repository;
    private final MemberEventRepository eventRepository;
    private final MemberContributionRepository contributionRepository;

    public MemberDashboardService(MemberDashboardRepository repository, MemberEventRepository eventRepository,
                                  MemberContributionRepository contributionRepository) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.contributionRepository = contributionRepository;
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard() {
        var member = repository.findByAccessUserHash(RequestUserContext.getRequired())
                .orElseThrow(() -> new ServiceException(HttpStatus.FORBIDDEN, "member_portal_access_not_linked"));
        var person = member.getPerson();
        var today = LocalDate.now();
        var events = eventRepository.upcomingEvents(AppointmentStatus.SCHEDULED, LocalDateTime.now(), PageRequest.of(0, 6))
                .stream().map(item -> new Event(item.getId(), item.getEventsType().getName(),
                        item.getEventsType().getColor(), item.getInitialDate(), item.getFinalDate(), item.getLocal())).toList();
        var contributions = contributionRepository.recentContributions(person.getId(), TypeFinancial.REVENUE, PageRequest.of(0, 6))
                .stream().map(item -> new Contribution(item.getId(), item.getDescription(),
                        item.getPlanAccount() == null ? null : item.getPlanAccount().getDescription(),
                        item.getPaymentReceiptDate(), item.getValue())).toList();
        var monthTotal = contributionRepository.contributionTotal(person.getId(), TypeFinancial.REVENUE,
                today.withDayOfMonth(1), today);
        var yearTotal = contributionRepository.contributionTotal(person.getId(), TypeFinancial.REVENUE,
                today.withDayOfYear(1), today);
        return new Dashboard(person.getName(), value(monthTotal), value(yearTotal), events, contributions);
    }

    private double value(Double value) {
        return value == null ? 0 : value;
    }
}

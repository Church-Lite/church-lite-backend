package com.smartverse.churchlitebackend.service.memberportal;

import com.smartverse.churchlitebackend.controller.memberportal.MemberTransparencyModels.*;
import com.smartverse.churchlitebackend_gen.authorization.exception.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbc;

    public MemberTransparencyService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public Configuration get() {
        var settings = jdbc.query("select mode, member_approval_enabled from member_portal_transparency_configuration where id = true",
                (rs, row) -> new Object[]{Mode.valueOf(rs.getString("mode")), rs.getBoolean("member_approval_enabled")});
        var mode = settings.isEmpty() ? Mode.DISABLED : (Mode) settings.getFirst()[0];
        var approvalEnabled = !settings.isEmpty() && (boolean) settings.getFirst()[1];
        var accounts = jdbc.query("""
                select p.id, p.code_tree, p.description, p.financial_nature,
                       coalesce(v.visibility, 'HIDDEN') visibility
                  from plan_account p
                  left join member_portal_plan_account_visibility v on v.plan_account_id = p.id
                 order by p.code_tree, p.description
                """, (rs, row) -> new PlanAccountVisibility(rs.getObject("id", java.util.UUID.class),
                rs.getString("code_tree"), rs.getString("description"),
                rs.getInt("financial_nature") == 0 ? "REVENUE" : "EXPENSE",
                Visibility.valueOf(rs.getString("visibility"))));
        return new Configuration(mode, approvalEnabled, accounts);
    }

    @Transactional
    public Configuration update(UpdateRequest request) {
        if (request == null || request.mode() == null) invalid();
        jdbc.update("""
                insert into member_portal_transparency_configuration (id, mode, member_approval_enabled) values (true, ?, ?)
                on conflict (id) do update set mode = excluded.mode, member_approval_enabled = excluded.member_approval_enabled
                """, request.mode().name(), request.memberApprovalEnabled());

        if (request.mode() == Mode.PARTIAL) {
            var values = Objects.requireNonNullElse(request.planAccounts(), List.<VisibilityUpdate>of());
            for (var item : values) {
                if (item == null || item.id() == null || item.visibility() == null) invalid();
                var changed = jdbc.update("""
                        insert into member_portal_plan_account_visibility (plan_account_id, visibility)
                        select id, ? from plan_account where id = ?
                        on conflict (plan_account_id) do update set visibility = excluded.visibility
                        """, item.visibility().name(), item.id());
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
        if (configuration.mode() == Mode.DISABLED) {
            return new FinancialPortal(false, Mode.DISABLED, start, end, 0, 0, List.of(), List.of());
        }

        Map<UUID, PlanAccountVisibility> configured = new LinkedHashMap<>();
        configuration.planAccounts().forEach(account -> configured.put(account.id(), account));
        var rows = jdbc.query("""
                select t.id, t.date_transaction, t.description, t.value,
                       f.type_financial, p.id plan_id, p.code_tree, p.description plan_description
                  from transactions t
                  join financial f on f.id = t.financial
                  left join plan_account p on p.id = f.plan_account
                 where t.date_transaction between ? and ?
                 order by t.date_transaction desc, t.id desc
                """, (rs, row) -> new FinancialRow(rs.getObject("id", UUID.class),
                rs.getObject("date_transaction", LocalDate.class), rs.getString("description"),
                rs.getDouble("value"), rs.getInt("type_financial"),
                rs.getObject("plan_id", UUID.class), rs.getString("code_tree"), rs.getString("plan_description")),
                start, end);

        Map<UUID, MutableSummary> summaries = new LinkedHashMap<>();
        List<ExpenseDetail> details = new ArrayList<>();
        double revenue = 0, expense = 0;
        for (var row : rows) {
            var account = row.planId() == null ? null : configured.get(row.planId());
            var visibility = configuration.mode() == Mode.FULL ? Visibility.DETAILED
                    : account == null ? Visibility.HIDDEN : account.visibility();
            if (visibility == Visibility.HIDDEN) continue;
            var nature = row.type() == 0 ? "REVENUE" : "EXPENSE";
            if (row.type() == 0) revenue += row.value(); else expense += row.value();
            if (row.planId() != null) {
                summaries.computeIfAbsent(row.planId(), id -> new MutableSummary(row.planId(), row.codeTree(),
                        row.planDescription(), nature, visibility)).total += row.value();
            }
            if (row.type() == 1 && visibility == Visibility.DETAILED) {
                details.add(new ExpenseDetail(row.id(), row.date(), row.description(), row.planId(),
                        row.planDescription() == null ? "Sem classificação" : row.planDescription(), row.value()));
            }
        }
        var accounts = summaries.values().stream().map(item -> new AccountSummary(item.id, item.codeTree,
                item.description, item.nature, item.visibility, item.total)).toList();
        return new FinancialPortal(true, configuration.mode(), start, end, revenue, expense, accounts, details);
    }

    private record FinancialRow(UUID id, LocalDate date, String description, double value, int type,
                                UUID planId, String codeTree, String planDescription) {}
    private static final class MutableSummary {
        private final UUID id; private final String codeTree; private final String description;
        private final String nature; private final Visibility visibility; private double total;
        private MutableSummary(UUID id, String codeTree, String description, String nature, Visibility visibility) {
            this.id = id; this.codeTree = codeTree; this.description = description;
            this.nature = nature; this.visibility = visibility;
        }
    }

    private void invalid() {
        throw new ServiceException(HttpStatus.BAD_REQUEST, "member_transparency_invalid_configuration");
    }
}

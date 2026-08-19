package com.smartverse.churchlitebackend.repository.memberportal;

import com.smartverse.churchlitebackend.model.memberportal.MemberTransparencyModels.PlanAccountVisibility;
import com.smartverse.churchlitebackend_gen.enums.MemberPortalPlanVisibility;
import com.smartverse.churchlitebackend_gen.enums.MemberPortalTransparencyMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class MemberTransparencyRepository {
    private final JdbcTemplate jdbc;

    public MemberTransparencyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Settings findSettings() {
        return jdbc.query("""
                select mode, member_approval_enabled
                  from member_portal_transparency_configuration where id = true
                """, (row, number) -> new Settings(MemberPortalTransparencyMode.valueOf(row.getString("mode")),
                row.getBoolean("member_approval_enabled"))).stream().findFirst()
                .orElse(new Settings(MemberPortalTransparencyMode.DISABLED, false));
    }

    public boolean isMemberApprovalEnabled() {
        return findSettings().memberApprovalEnabled();
    }

    public List<PlanAccountVisibility> findPlanAccountVisibilities() {
        return jdbc.query("""
                select p.id, p.code_tree, p.description, p.financial_nature,
                       coalesce(v.visibility, 'HIDDEN') visibility
                  from plan_account p
                  left join member_portal_plan_account_visibility v on v.plan_account_id = p.id
                 order by p.code_tree, p.description
                """, (row, number) -> new PlanAccountVisibility(row.getObject("id", UUID.class),
                row.getString("code_tree"), row.getString("description"),
                row.getInt("financial_nature") == 0 ? "REVENUE" : "EXPENSE",
                MemberPortalPlanVisibility.valueOf(row.getString("visibility"))));
    }

    public void saveSettings(MemberPortalTransparencyMode mode, boolean approvalEnabled) {
        jdbc.update("""
                insert into member_portal_transparency_configuration (id, mode, member_approval_enabled)
                values (true, ?, ?)
                on conflict (id) do update
                    set mode = excluded.mode, member_approval_enabled = excluded.member_approval_enabled
                """, mode.name(), approvalEnabled);
    }

    public int saveVisibility(UUID planAccountId, MemberPortalPlanVisibility visibility) {
        return jdbc.update("""
                insert into member_portal_plan_account_visibility (plan_account_id, visibility)
                select id, ? from plan_account where id = ?
                on conflict (plan_account_id) do update set visibility = excluded.visibility
                """, visibility.name(), planAccountId);
    }

    public List<FinancialRow> findFinancialRows(LocalDate start, LocalDate end) {
        return jdbc.query("""
                select t.id, t.date_transaction, t.description, t.value,
                       f.type_financial, p.id plan_id, p.code_tree, p.description plan_description
                  from transactions t
                  join financial f on f.id = t.financial
                  left join plan_account p on p.id = f.plan_account
                 where t.date_transaction between ? and ?
                 order by t.date_transaction desc, t.id desc
                """, (row, number) -> new FinancialRow(row.getObject("id", UUID.class),
                row.getObject("date_transaction", LocalDate.class), row.getString("description"),
                row.getDouble("value"), row.getInt("type_financial"), row.getObject("plan_id", UUID.class),
                row.getString("code_tree"), row.getString("plan_description")), start, end);
    }

    public record Settings(MemberPortalTransparencyMode mode, boolean memberApprovalEnabled) {
    }

    public record FinancialRow(UUID id, LocalDate date, String description, double value, int type,
                               UUID planId, String codeTree, String planDescription) {
    }
}

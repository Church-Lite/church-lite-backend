package com.smartverse.churchlitebackend.repository.memberportal;

import com.smartverse.churchlitebackend.model.memberportal.MemberFinancialApprovalModels.CashClosing;
import com.smartverse.churchlitebackend_gen.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.enums.MemberFinancialStatementStatus;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MemberFinancialApprovalRepository {
    private final JdbcTemplate jdbc;

    public MemberFinancialApprovalRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CashClosing> findAvailableClosings() {
        return jdbc.query("""
                select ct.id, c.description, ct.start_date, ct.end_date, ct.final_balance
                  from cash_transactions ct
                  join cash c on c.id = ct.cash
                 where ct.end_date is not null
                 order by ct.end_date desc
                """, (row, number) -> cashClosing(row));
    }

    public void insertStatement(UUID id, String title, String description, UUID anonymousSalt) {
        jdbc.update("""
                insert into member_financial_statement(id, title, description, status, anonymous_salt)
                values (?, ?, ?, 'DRAFT', ?)
                """, id, title, description, anonymousSalt);
    }

    public int updateDraft(UUID id, String title, String description) {
        return jdbc.update("""
                update member_financial_statement set title = ?, description = ?
                 where id = ? and status = 'DRAFT'
                """, title, description, id);
    }

    public void deleteCashClosings(UUID statementId) {
        jdbc.update("delete from member_financial_statement_cash where statement_id = ?", statementId);
    }

    public int addCashClosing(UUID statementId, UUID cashClosingId) {
        return jdbc.update("""
                insert into member_financial_statement_cash(statement_id, cash_transaction_id)
                select ?, id from cash_transactions where id = ? and end_date is not null
                """, statementId, cashClosingId);
    }

    public int transition(UUID id, MemberFinancialStatementStatus currentStatus,
                          MemberFinancialStatementStatus nextStatus, String timestampColumn,
                          LocalDateTime timestamp) {
        if (!timestampColumn.equals("published_at") && !timestampColumn.equals("closed_at")) {
            throw new IllegalArgumentException("Unsupported statement timestamp column");
        }
        var sql = "update member_financial_statement set status = ?, " + timestampColumn
                + " = ? where id = ? and status = ?";
        return jdbc.update(sql, nextStatus.name(), timestamp, id, currentStatus.name());
    }

    public List<UUID> findAdminStatementIds() {
        return jdbc.query("""
                select id from member_financial_statement
                 order by coalesce(published_at, closed_at) desc nulls last, title
                """, (row, number) -> row.getObject("id", UUID.class));
    }

    public List<UUID> findPublishedStatementIds() {
        return jdbc.query("""
                select id from member_financial_statement
                 where status in ('OPEN', 'CLOSED')
                 order by published_at desc
                """, (row, number) -> row.getObject("id", UUID.class));
    }

    public Optional<StatementData> findStatement(UUID id) {
        return jdbc.query("""
                select id, title, description, status, published_at, closed_at, anonymous_salt
                  from member_financial_statement where id = ?
                """, (row, number) -> new StatementData(row.getObject("id", UUID.class), row.getString("title"),
                row.getString("description"), MemberFinancialStatementStatus.valueOf(row.getString("status")),
                row.getObject("published_at", LocalDateTime.class),
                row.getObject("closed_at", LocalDateTime.class),
                row.getObject("anonymous_salt", UUID.class)), id).stream().findFirst();
    }

    public UUID findAnonymousSalt(UUID id) {
        return findStatement(id).map(StatementData::anonymousSalt).orElseThrow(() ->
                new ServiceException(HttpStatus.NOT_FOUND, "member_financial_statement_not_found"));
    }

    public List<CashClosing> findCashClosings(UUID statementId) {
        return jdbc.query("""
                select ct.id, c.description, ct.start_date, ct.end_date, ct.final_balance
                  from member_financial_statement_cash statement_cash
                  join cash_transactions ct on ct.id = statement_cash.cash_transaction_id
                  join cash c on c.id = ct.cash
                 where statement_cash.statement_id = ?
                 order by ct.end_date
                """, (row, number) -> cashClosing(row), statementId);
    }

    public FinancialTotals findFinancialTotals(UUID statementId) {
        Map<String, Object> values = jdbc.queryForMap("""
                select coalesce(sum(case when f.type_financial = 0 then t.value else 0 end), 0) revenue,
                       coalesce(sum(case when f.type_financial = 1 then t.value else 0 end), 0) expense
                  from member_financial_statement_cash statement_cash
                  join transactions t on t.cash_transaction = statement_cash.cash_transaction_id
                  join financial f on f.id = t.financial
                 where statement_cash.statement_id = ?
                """, statementId);
        return new FinancialTotals(number(values, "revenue").doubleValue(), number(values, "expense").doubleValue());
    }

    public VoteTotals findVoteTotals(UUID statementId) {
        Map<String, Object> values = jdbc.queryForMap("""
                select count(*) filter (where approved) approvals,
                       count(*) filter (where not approved) rejections,
                       count(*) votes
                  from member_financial_statement_vote where statement_id = ?
                """, statementId);
        return new VoteTotals(number(values, "approvals").longValue(),
                number(values, "rejections").longValue(), number(values, "votes").longValue());
    }

    public int insertVote(UUID statementId, String fingerprint, boolean approved) {
        return jdbc.update("""
                insert into member_financial_statement_vote(id, statement_id, voter_fingerprint, approved)
                select ?, id, ?, ? from member_financial_statement where id = ? and status = 'OPEN'
                """, UUID.randomUUID(), fingerprint, approved, statementId);
    }

    public boolean hasVote(UUID statementId, String fingerprint) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from member_financial_statement_vote
                               where statement_id = ? and voter_fingerprint = ?)
                """, Boolean.class, statementId, fingerprint));
    }

    private CashClosing cashClosing(java.sql.ResultSet row) throws java.sql.SQLException {
        return new CashClosing(row.getObject("id", UUID.class), row.getString("description"),
                row.getObject("start_date", java.time.LocalDate.class),
                row.getObject("end_date", java.time.LocalDate.class), row.getDouble("final_balance"));
    }

    private Number number(Map<String, Object> values, String key) {
        return (Number) values.get(key);
    }

    public record StatementData(UUID id, String title, String description, MemberFinancialStatementStatus status,
                                LocalDateTime publishedAt, LocalDateTime closedAt, UUID anonymousSalt) {
    }

    public record FinancialTotals(double revenue, double expense) {
    }

    public record VoteTotals(long approvals, long rejections, long votes) {
    }
}

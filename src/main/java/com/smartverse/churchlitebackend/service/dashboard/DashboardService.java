package com.smartverse.churchlitebackend.service.dashboard;


import com.smartverse.churchlitebackend.model.dashboard.DashboardModels;
import com.smartverse.churchlitebackend.model.dashboard.DashboardModels.*;
import com.smartverse.churchlitebackend.repository.dashboard.DashboardRepository;
import com.smartverse.churchlitebackend_gen.entities.*;
import com.smartverse.churchlitebackend_gen.enums.AppointmentStatus;
import com.smartverse.churchlitebackend_gen.enums.TypeCash;
import com.smartverse.churchlitebackend_gen.enums.TypeFinancial;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final DashboardRepository repository;

    public DashboardService(DashboardRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DashboardModels.FinancialSnapshot financial(DashboardModels.FinancialFilter requested) {
        LocalDate end = requested.dataFinal() == null ? LocalDate.now() : requested.dataFinal();
        LocalDate start = requested.dataInicial() == null ? end.withDayOfMonth(1) : requested.dataInicial();
        if (start.isAfter(end)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }
        DashboardModels.FinancialFilter filter = new DashboardModels.FinancialFilter(start, end, requested.bancoId(), requested.contaBancariaId(),
                requested.caixaId(), requested.somenteCaixasAbertos(), requested.centroCustoId(), requested.planoContaId());

        List<TransactionsEntity> allTransactions = repository.transactions();
        List<CashTransactionsEntity> sessions = repository.cashTransactions();
        Set<UUID> openCashIds = sessions.stream().filter(s -> s.getEndDate() == null && s.getCash() != null)
                .map(s -> s.getCash().getId()).collect(Collectors.toSet());
        List<TransactionsEntity> current = allTransactions.stream().filter(t -> matches(t, filter, openCashIds, true)).toList();

        long days = Duration.between(start.atStartOfDay(), end.plusDays(1).atStartOfDay()).toDays();
        LocalDate previousEnd = start.minusDays(1);
        FinancialFilter previousFilter = new FinancialFilter(start.minusDays(days), previousEnd, filter.bancoId(),
                filter.contaBancariaId(), filter.caixaId(), filter.somenteCaixasAbertos(),
                filter.centroCustoId(), filter.planoContaId());
        List<TransactionsEntity> previous = allTransactions.stream().filter(t -> matches(t, previousFilter, openCashIds, true)).toList();

        double revenue = total(current, TypeFinancial.REVENUE);
        double expense = total(current, TypeFinancial.EXPENSE);
        double previousRevenue = total(previous, TypeFinancial.REVENUE);
        double previousExpense = total(previous, TypeFinancial.EXPENSE);
        Balances balances = balances(allTransactions, sessions, filter, openCashIds);
        FinancialSummary summary = new FinancialSummary(
                new Indicator(revenue, previousRevenue, variation(revenue, previousRevenue)),
                new Indicator(expense, previousExpense, variation(expense, previousExpense)),
                new Result(revenue - expense, situation(revenue - expense)),
                new AvailableBalance(balances.saldoTotal(), balances.saldoContasBancarias(), balances.saldoCaixas(),
                        balances.contasBancarias().size(), balances.caixas().size()));

        return new FinancialSnapshot(summary, evolution(current, start, end), costCenters(current, expense),
                planAccounts(current, expense), balances, recent(current), alerts(current, sessions), filters());
    }

    @Transactional(readOnly = true)
    public AgendaSnapshot agenda() {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        List<AppointmentsEntity> active = repository.appointments().stream()
                .filter(a -> a.getInitialDate() != null && a.getStatus() != AppointmentStatus.CANCELLED)
                .sorted(Comparator.comparing(AppointmentsEntity::getInitialDate)).toList();
        List<AgendaItem> todayItems = active.stream().filter(a -> a.getInitialDate().toLocalDate().equals(today))
                .map(this::agendaItem).toList();
        List<AgendaItem> next = active.stream().filter(a -> !a.getInitialDate().toLocalDate().isBefore(today))
                .limit(8).map(this::agendaItem).toList();
        long week = active.stream().filter(a -> between(a.getInitialDate().toLocalDate(), today, weekEnd)).count();
        long month = active.stream().filter(a -> between(a.getInitialDate().toLocalDate(), today.withDayOfMonth(1), monthEnd)).count();
        return new AgendaSnapshot(todayItems, next, new AgendaSummary(todayItems.size(), week, month));
    }

    private boolean matches(TransactionsEntity t, FinancialFilter f, Set<UUID> openCashIds, boolean dates) {
        FinancialEntity financial = t.getFinancial();
        CashEntity cash = financial == null ? null : financial.getCash();
        if (financial == null || cash == null || t.getDateTransaction() == null) return false;
        if (dates && !between(t.getDateTransaction(), f.dataInicial(), f.dataFinal())) return false;
        if (f.bancoId() != null && (cash.getBank() == null || !f.bancoId().equals(cash.getBank().getId())))
            return false;
        if (f.contaBancariaId() != null && !f.contaBancariaId().equals(cash.getId())) return false;
        if (f.caixaId() != null && !f.caixaId().equals(cash.getId())) return false;
        if (f.somenteCaixasAbertos() && cash.getTypeCash() == TypeCash.CASH && !openCashIds.contains(cash.getId()))
            return false;
        if (f.centroCustoId() != null && (financial.getCostCenter() == null || !f.centroCustoId().equals(financial.getCostCenter().getId())))
            return false;
        return f.planoContaId() == null || (financial.getPlanAccount() != null && f.planoContaId().equals(financial.getPlanAccount().getId()));
    }

    private Balances balances(List<TransactionsEntity> all, List<CashTransactionsEntity> sessions,
                              FinancialFilter filter, Set<UUID> openCashIds) {
        Map<UUID, List<TransactionsEntity>> byCash = all.stream().filter(t -> t.getFinancial() != null && t.getFinancial().getCash() != null)
                .collect(Collectors.groupingBy(t -> t.getFinancial().getCash().getId()));
        List<CashEntity> cashes = repository.cashes().stream().filter(c -> balanceCashMatches(c, filter, openCashIds)).toList();
        List<BankBalance> banks = cashes.stream().filter(c -> c.getTypeCash() == TypeCash.BANK).map(c -> {
            double value = signedTotal(byCash.getOrDefault(c.getId(), List.of()));
            return new BankBalance(c.getId(), c.getBank() == null ? null : c.getBank().getId(),
                    c.getBank() == null ? "Banco não informado" : c.getBank().getName(), c.getDescription(), null,
                    maskAccount(c.getNumberAccount(), c.getDigit()), value);
        }).toList();
        List<CashBalance> physical = cashes.stream().filter(c -> c.getTypeCash() == TypeCash.CASH).map(c -> {
            CashTransactionsEntity latest = sessions.stream().filter(s -> s.getCash() != null && c.getId().equals(s.getCash().getId()))
                    .max(Comparator.comparing(CashTransactionsEntity::getStartDate)).orElse(null);
            boolean open = latest != null && latest.getEndDate() == null;
            double value = latest == null ? 0 : (open
                                                 ? number(latest.getInitialBalance()) + signedTotal(byCash.getOrDefault(c.getId(), List.of()).stream()
                    .filter(t -> Objects.equals(t.getCashTransaction(), latest.getId())).toList())
                                                 : number(latest.getFinalBalance()));
            return new CashBalance(c.getId(), c.getDescription(), open ? "ABERTO" : "FECHADO", value,
                    latest == null ? null : latest.getStartDate(), null);
        }).toList();
        double bankTotal = banks.stream().mapToDouble(BankBalance::saldoAtual).sum();
        double cashTotal = physical.stream().mapToDouble(CashBalance::saldoAtual).sum();
        return new Balances(bankTotal + cashTotal, bankTotal, cashTotal, banks, physical);
    }

    private boolean balanceCashMatches(CashEntity c, FinancialFilter f, Set<UUID> open) {
        if (f.bancoId() != null && (c.getBank() == null || !f.bancoId().equals(c.getBank().getId()))) return false;
        if (f.contaBancariaId() != null && !f.contaBancariaId().equals(c.getId())) return false;
        if (f.caixaId() != null && !f.caixaId().equals(c.getId())) return false;
        return !f.somenteCaixasAbertos() || c.getTypeCash() != TypeCash.CASH || open.contains(c.getId());
    }

    private List<EvolutionPoint> evolution(List<TransactionsEntity> values, LocalDate start, LocalDate end) {
        long days = Duration.between(start.atStartOfDay(), end.plusDays(1).atStartOfDay()).toDays();
        Function<LocalDate, String> key = days <= 31 ? LocalDate::toString
                : days <= 120 ? d -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
                  : d -> String.format("%04d-%02d", d.getYear(), d.getMonthValue());
        Map<String, List<TransactionsEntity>> grouped = values.stream().collect(Collectors.groupingBy(t -> key.apply(t.getDateTransaction()), TreeMap::new, Collectors.toList()));
        return grouped.entrySet().stream().map(e -> {
            double r = total(e.getValue(), TypeFinancial.REVENUE), x = total(e.getValue(), TypeFinancial.EXPENSE);
            return new EvolutionPoint(e.getKey(), r, x, r - x);
        }).toList();
    }

    private List<CostCenterExpense> costCenters(List<TransactionsEntity> values, double totalExpense) {
        List<TransactionsEntity> expenses = values.stream().filter(t -> type(t) == TypeFinancial.EXPENSE).toList();
        List<CostCenterExpense> result = new ArrayList<>(expenses.stream()
                .filter(t -> t.getFinancial().getCostCenter() != null)
                .collect(Collectors.groupingBy(t -> t.getFinancial().getCostCenter(), Collectors.summingDouble(t -> number(t.getValue()))))
                .entrySet().stream().map(e -> new CostCenterExpense(e.getKey().getId(),
                        e.getKey().getDescription(), e.getValue(), percent(e.getValue(), totalExpense))).toList());
        double unclassified = expenses.stream().filter(t -> t.getFinancial().getCostCenter() == null).mapToDouble(t -> number(t.getValue())).sum();
        if (unclassified > 0)
            result.add(new CostCenterExpense(null, "Sem centro de custo", unclassified, percent(unclassified, totalExpense)));
        return result.stream().sorted(Comparator.comparingDouble(CostCenterExpense::valorTotal).reversed()).toList();
    }

    private List<PlanAccountExpense> planAccounts(List<TransactionsEntity> values, double totalExpense) {
        List<TransactionsEntity> expenses = values.stream().filter(t -> type(t) == TypeFinancial.EXPENSE).toList();
        List<PlanAccountExpense> result = new ArrayList<>(expenses.stream()
                .filter(t -> t.getFinancial().getPlanAccount() != null)
                .collect(Collectors.groupingBy(t -> t.getFinancial().getPlanAccount(), Collectors.summingDouble(t -> number(t.getValue()))))
                .entrySet().stream().map(e -> {
                    PlanAccountEntity p = e.getKey();
                    String code = p.getCodeTree();
                    int level = code == null || code.isBlank() ? 0 : code.split("\\.").length;
                    return new PlanAccountExpense(p.getId(), code, p.getDescription(), level,
                            p.getParentCode() == null ? null : p.getParentCode().getId(), e.getValue(), percent(e.getValue(), totalExpense));
                }).toList());
        double unclassified = expenses.stream().filter(t -> t.getFinancial().getPlanAccount() == null).mapToDouble(t -> number(t.getValue())).sum();
        if (unclassified > 0)
            result.add(new PlanAccountExpense(null, "", "Sem classificação", 0, null, unclassified, percent(unclassified, totalExpense)));
        return result.stream().sorted(Comparator.comparingDouble(PlanAccountExpense::valorTotal).reversed()).toList();
    }

    private List<RecentTransaction> recent(List<TransactionsEntity> values) {
        return values.stream().sorted(Comparator.comparing(TransactionsEntity::getDateTransaction).reversed()).limit(8).map(t -> {
            FinancialEntity f = t.getFinancial();
            CashEntity c = f.getCash();
            return new RecentTransaction(t.getId(), t.getDateTransaction(), t.getDescription(),
                    type(t) == TypeFinancial.REVENUE ? "RECEITA" : "DESPESA",
                    f.getPlanAccount() == null ? "Sem classificação" : f.getPlanAccount().getDescription(),
                    f.getCostCenter() == null ? "Sem centro de custo" : f.getCostCenter().getDescription(),
                    c.getDescription(), c.getTypeCash() == TypeCash.BANK ? "CONTA_BANCARIA" : "CAIXA", number(t.getValue()), "CONFIRMADA");
        }).toList();
    }

    private List<Alert> alerts(List<TransactionsEntity> values, List<CashTransactionsEntity> sessions) {
        List<Alert> alerts = new ArrayList<>();
        List<CashTransactionsEntity> open = sessions.stream().filter(s -> s.getEndDate() == null).toList();
        if (!open.isEmpty())
            alerts.add(new Alert("CAIXA_ABERTO", "ATENCAO", "Existem caixas aguardando fechamento.", open.size(), open.getFirst().getId()));
        long withoutCost = values.stream().filter(t -> t.getFinancial().getCostCenter() == null).count();
        if (withoutCost > 0)
            alerts.add(new Alert("SEM_CENTRO_CUSTO", "INFORMACAO", "Movimentações sem centro de custo.", (int) withoutCost, null));
        long withoutPlan = values.stream().filter(t -> t.getFinancial().getPlanAccount() == null).count();
        if (withoutPlan > 0)
            alerts.add(new Alert("SEM_PLANO_CONTA", "INFORMACAO", "Movimentações sem plano de contas.", (int) withoutPlan, null));
        return alerts;
    }

    private DashboardFilters filters() {
        return new DashboardFilters(
                repository.banks().stream().map(b -> new FilterOption(b.getId(), b.getName())).sorted(Comparator.comparing(FilterOption::descricao)).toList(),
                repository.cashes().stream().filter(c -> c.getTypeCash() == TypeCash.BANK).map(c -> new FilterOption(c.getId(), c.getDescription())).toList(),
                repository.cashes().stream().filter(c -> c.getTypeCash() == TypeCash.CASH).map(c -> new FilterOption(c.getId(), c.getDescription())).toList(),
                repository.costCenters().stream().map(c -> new FilterOption(c.getId(), c.getDescription())).toList(),
                repository.planAccounts().stream().map(p -> new FilterOption(p.getId(), p.getCodeTree() + " - " + p.getDescription())).toList());
    }

    private AgendaItem agendaItem(AppointmentsEntity a) {
        return new AgendaItem(a.getId(), a.getEventsType() == null ? a.getDescription() : a.getEventsType().getName(),
                a.getEventsType() == null ? "Compromisso" : a.getEventsType().getName(), a.getInitialDate(), a.getFinalDate(),
                a.getLocal(), a.getUserConfiguration() == null ? null : a.getUserConfiguration().getName(), a.getStatus() == null ? "SCHEDULED" : a.getStatus().name());
    }

    private double total(List<TransactionsEntity> values, TypeFinancial type) {
        return values.stream().filter(t -> type(t) == type).mapToDouble(t -> number(t.getValue())).sum();
    }

    private double signedTotal(List<TransactionsEntity> values) {
        return values.stream().mapToDouble(t -> type(t) == TypeFinancial.EXPENSE ? -number(t.getValue()) : number(t.getValue())).sum();
    }

    private TypeFinancial type(TransactionsEntity t) {
        return t.getFinancial().getTypeFinancial();
    }

    private double number(Double value) {
        return value == null ? 0 : value;
    }

    private boolean between(LocalDate value, LocalDate start, LocalDate end) {
        return !value.isBefore(start) && !value.isAfter(end);
    }

    private Double variation(double current, double previous) {
        return previous == 0 ? null : ((current - previous) / Math.abs(previous)) * 100;
    }

    private double percent(double value, double total) {
        return total == 0 ? 0 : value * 100 / total;
    }

    private String situation(double value) {
        return value > 0 ? "POSITIVO" : value < 0 ? "NEGATIVO" : "ZERADO";
    }

    private String maskAccount(String number, String digit) {
        if (number == null || number.isBlank()) return null;
        String visible = number.length() <= 4 ? number : number.substring(number.length() - 4);
        return "•••• " + visible + (digit == null ? "" : "-" + digit);
    }
}

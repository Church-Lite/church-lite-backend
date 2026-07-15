package com.smartverse.churchlitebackend.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class DashboardModels {
    private DashboardModels() {}

    public record FinancialFilter(LocalDate dataInicial, LocalDate dataFinal, UUID bancoId,
            UUID contaBancariaId, UUID caixaId, boolean somenteCaixasAbertos,
            UUID centroCustoId, UUID planoContaId) {}
    public record Indicator(double valorTotal, double valorPeriodoAnterior, Double percentualVariacao) {}
    public record Result(double valorResultado, String situacao) {}
    public record AvailableBalance(double saldoTotal, double saldoContasBancarias, double saldoCaixas,
            int quantidadeContasBancarias, int quantidadeCaixas) {}
    public record FinancialSummary(Indicator receitasPeriodo, Indicator despesasPeriodo,
            Result resultadoPeriodo, AvailableBalance saldoDisponivel) {}
    public record EvolutionPoint(String periodo, double receitas, double despesas, double resultado) {}
    public record CostCenterExpense(UUID centroCustoId, String descricao, double valorTotal, double percentual) {}
    public record PlanAccountExpense(UUID planoContaId, String codigo, String descricao, int nivel,
            UUID paiId, double valorTotal, double percentual) {}
    public record BankBalance(UUID contaBancariaId, UUID bancoId, String banco, String descricao,
            String agencia, String numeroConta, double saldoAtual) {}
    public record CashBalance(UUID caixaId, String descricao, String situacao, double saldoAtual,
            LocalDate dataAbertura, String responsavel) {}
    public record Balances(double saldoTotal, double saldoContasBancarias, double saldoCaixas,
            List<BankBalance> contasBancarias, List<CashBalance> caixas) {}
    public record RecentTransaction(UUID id, LocalDate data, String descricao, String tipo,
            String planoConta, String centroCusto, String origem, String origemTipo,
            double valor, String situacao) {}
    public record Alert(String tipo, String severidade, String descricao, int quantidade, UUID referenciaId) {}
    public record FilterOption(UUID id, String descricao) {}
    public record DashboardFilters(List<FilterOption> bancos, List<FilterOption> contasBancarias,
            List<FilterOption> caixas, List<FilterOption> centrosCusto, List<FilterOption> planosConta) {}
    public record FinancialSnapshot(FinancialSummary resumo, List<EvolutionPoint> evolucao,
            List<CostCenterExpense> despesasPorCentroCusto, List<PlanAccountExpense> despesasPorPlanoConta,
            Balances saldos, List<RecentTransaction> movimentacoesRecentes, List<Alert> alertas,
            DashboardFilters filtros) {}
    public record AgendaItem(UUID id, String titulo, String tipoEvento, LocalDateTime dataInicio,
            LocalDateTime dataFim, String local, String responsavel, String situacao) {}
    public record AgendaSummary(long quantidadeHoje, long quantidadeSemana, long quantidadeMes) {}
    public record AgendaSnapshot(List<AgendaItem> agendaHoje, List<AgendaItem> proximosEventos,
            AgendaSummary resumo) {}
}

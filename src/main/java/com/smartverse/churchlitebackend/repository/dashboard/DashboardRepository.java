package com.smartverse.churchlitebackend.repository.dashboard;

import com.smartverse.churchlitebackend_gen.entities.*;
import com.smartverse.churchlitebackend_gen.repositories.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DashboardRepository {
    private final TransactionsRepository transactions;
    private final CashRepository cash;
    private final CashTransactionsRepository cashTransactions;
    private final BankRepository banks;
    private final CostCenterRepository costCenters;
    private final PlanAccountRepository planAccounts;
    private final AppointmentsRepository appointments;

    public DashboardRepository(TransactionsRepository transactions, CashRepository cash,
            CashTransactionsRepository cashTransactions, BankRepository banks,
            CostCenterRepository costCenters, PlanAccountRepository planAccounts,
            AppointmentsRepository appointments) {
        this.transactions = transactions;
        this.cash = cash;
        this.cashTransactions = cashTransactions;
        this.banks = banks;
        this.costCenters = costCenters;
        this.planAccounts = planAccounts;
        this.appointments = appointments;
    }

    public List<TransactionsEntity> transactions() { return transactions.findAll(); }
    public List<CashEntity> cashes() { return cash.findAll(); }
    public List<CashTransactionsEntity> cashTransactions() { return cashTransactions.findAll(); }
    public List<BankEntity> banks() { return banks.findAll(); }
    public List<CostCenterEntity> costCenters() { return costCenters.findAll(); }
    public List<PlanAccountEntity> planAccounts() { return planAccounts.findAll(); }
    public List<AppointmentsEntity> appointments() { return appointments.findAll(); }
}

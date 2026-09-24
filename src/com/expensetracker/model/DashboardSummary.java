package com.expensetracker.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the primary dashboard financial KPI metrics:
 *  - Current Balance (Total Income - Total Expenses)
 *  - Total Income
 *  - Total Expenses
 *  - Monthly Budget & Utilization
 *  - Recent Transactions
 */
public class DashboardSummary {

    private double balance;
    private double totalIncome;
    private double totalExpense;
    private double monthlyBudget;
    private double budgetRemaining;
    private double budgetPercentage;
    private int transactionCount;
    private List<Transaction> recentTransactions = new ArrayList<>();

    public DashboardSummary() {
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = Math.round(balance * 100.0) / 100.0;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(double totalIncome) {
        this.totalIncome = Math.round(totalIncome * 100.0) / 100.0;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(double totalExpense) {
        this.totalExpense = Math.round(totalExpense * 100.0) / 100.0;
    }

    public double getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(double monthlyBudget) {
        this.monthlyBudget = Math.round(monthlyBudget * 100.0) / 100.0;
    }

    public double getBudgetRemaining() {
        return budgetRemaining;
    }

    public void setBudgetRemaining(double budgetRemaining) {
        this.budgetRemaining = Math.round(budgetRemaining * 100.0) / 100.0;
    }

    public double getBudgetPercentage() {
        return budgetPercentage;
    }

    public void setBudgetPercentage(double budgetPercentage) {
        this.budgetPercentage = Math.round(budgetPercentage * 10.0) / 10.0;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public List<Transaction> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<Transaction> recentTransactions) {
        this.recentTransactions = (recentTransactions != null) ? recentTransactions : new ArrayList<>();
    }
}

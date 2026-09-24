package com.expensetracker.model;

/**
 * Encapsulates monthly financial reporting metrics:
 *  - Income, Expenses, Savings (Income - Expenses)
 *  - Transaction Count
 *  - Highest Expense, Highest Spending Category
 *  - Remaining Budget, Budget Percentage Used
 */
public class MonthlySummary {

    private int month;
    private int year;
    private String monthName;
    private double income;
    private double expenses;
    private double savings;
    private int transactionCount;
    private Transaction highestExpense;
    private String highestSpendingCategory;
    private double highestSpendingCategoryAmount;
    private double budgetAmount;
    private double remainingBudget;
    private double budgetPercentageUsed;

    public MonthlySummary() {
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }

    public double getIncome() {
        return income;
    }

    public void setIncome(double income) {
        this.income = Math.round(income * 100.0) / 100.0;
        recalculateSavings();
    }

    public double getExpenses() {
        return expenses;
    }

    public void setExpenses(double expenses) {
        this.expenses = Math.round(expenses * 100.0) / 100.0;
        recalculateSavings();
        recalculateBudgetMetrics();
    }

    public double getSavings() {
        return savings;
    }

    public void setSavings(double savings) {
        this.savings = Math.round(savings * 100.0) / 100.0;
    }

    private void recalculateSavings() {
        this.savings = Math.round((this.income - this.expenses) * 100.0) / 100.0;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Transaction getHighestExpense() {
        return highestExpense;
    }

    public void setHighestExpense(Transaction highestExpense) {
        this.highestExpense = highestExpense;
    }

    public String getHighestSpendingCategory() {
        return highestSpendingCategory;
    }

    public void setHighestSpendingCategory(String highestSpendingCategory) {
        this.highestSpendingCategory = highestSpendingCategory;
    }

    public double getHighestSpendingCategoryAmount() {
        return highestSpendingCategoryAmount;
    }

    public void setHighestSpendingCategoryAmount(double highestSpendingCategoryAmount) {
        this.highestSpendingCategoryAmount = Math.round(highestSpendingCategoryAmount * 100.0) / 100.0;
    }

    public double getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(double budgetAmount) {
        this.budgetAmount = Math.round(budgetAmount * 100.0) / 100.0;
        recalculateBudgetMetrics();
    }

    public double getRemainingBudget() {
        return remainingBudget;
    }

    public void setRemainingBudget(double remainingBudget) {
        this.remainingBudget = Math.round(remainingBudget * 100.0) / 100.0;
    }

    public double getBudgetPercentageUsed() {
        return budgetPercentageUsed;
    }

    public void setBudgetPercentageUsed(double budgetPercentageUsed) {
        this.budgetPercentageUsed = Math.round(budgetPercentageUsed * 10.0) / 10.0;
    }

    private void recalculateBudgetMetrics() {
        this.remainingBudget = Math.round((this.budgetAmount - this.expenses) * 100.0) / 100.0;
        if (this.budgetAmount > 0) {
            this.budgetPercentageUsed = Math.round((this.expenses / this.budgetAmount) * 10000.0) / 100.0;
        } else {
            this.budgetPercentageUsed = 0.0;
        }
    }
}

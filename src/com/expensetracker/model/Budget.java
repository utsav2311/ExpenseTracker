package com.expensetracker.model;

import java.time.LocalDateTime;

/**
 * Budget model for tracking user monthly overall spending limit.
 * Applies strictly to monthly expenses.
 */
public class Budget {

    private int budgetId;
    private int userId = 1;
    private int month;
    private int year;
    private double budgetAmount;
    private LocalDateTime createdAt;

    // Dynamically computed metrics for reports and dashboard
    private double spent;
    private double remaining;
    private double percentageUsed;

    public Budget() {
        this.createdAt = LocalDateTime.now();
    }

    public Budget(int budgetId, int userId, int month, int year, double budgetAmount) {
        this();
        this.budgetId = budgetId;
        this.userId = userId;
        this.month = month;
        this.year = year;
        setBudgetAmount(budgetAmount);
    }

    public Budget(int userId, int month, int year, double budgetAmount) {
        this(0, userId, month, year, budgetAmount);
    }

    public int getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(int budgetId) {
        this.budgetId = budgetId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    public double getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(double budgetAmount) {
        if (budgetAmount < 0) {
            throw new IllegalArgumentException("Budget amount cannot be negative.");
        }
        this.budgetAmount = Math.round(budgetAmount * 100.0) / 100.0;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public double getSpent() {
        return spent;
    }

    public void setSpent(double spent) {
        this.spent = Math.round(spent * 100.0) / 100.0;
        this.remaining = Math.round((budgetAmount - spent) * 100.0) / 100.0;
        if (budgetAmount > 0) {
            this.percentageUsed = Math.round((spent / budgetAmount) * 10000.0) / 100.0;
        } else {
            this.percentageUsed = 0.0;
        }
    }

    public double getRemaining() {
        return remaining;
    }

    public void setRemaining(double remaining) {
        this.remaining = Math.round(remaining * 100.0) / 100.0;
    }

    public double getPercentageUsed() {
        return percentageUsed;
    }

    public void setPercentageUsed(double percentageUsed) {
        this.percentageUsed = Math.round(percentageUsed * 10.0) / 10.0;
    }

    public boolean isExceeded() {
        return spent > budgetAmount && budgetAmount > 0;
    }

    public double getExceededAmount() {
        return isExceeded() ? Math.round((spent - budgetAmount) * 100.0) / 100.0 : 0.0;
    }
}

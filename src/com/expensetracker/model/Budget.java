package com.expensetracker.model;

/**
 * Budget model for tracking category monthly spending targets.
 */
public class Budget {

    private int id;
    private Category category;
    private double monthlyLimit;
    private String monthYear; // Format: "YYYY-MM"
    private double spent;     // Dynamically computed from expenses

    public Budget() {
    }

    public Budget(int id, Category category, double monthlyLimit, String monthYear) {
        this.id = id;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.monthYear = monthYear;
    }

    public Budget(Category category, double monthlyLimit, String monthYear) {
        this(0, category, monthlyLimit, monthYear);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(double monthlyLimit) {
        if (monthlyLimit < 0) {
            throw new IllegalArgumentException("Budget limit cannot be negative.");
        }
        this.monthlyLimit = Math.round(monthlyLimit * 100.0) / 100.0;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }

    public double getSpent() {
        return spent;
    }

    public void setSpent(double spent) {
        this.spent = Math.round(spent * 100.0) / 100.0;
    }

    public double getRemaining() {
        return Math.max(0, monthlyLimit - spent);
    }

    public double getPercentageUsed() {
        if (monthlyLimit <= 0) return 0.0;
        return Math.min(100.0, Math.round((spent / monthlyLimit) * 10000.0) / 100.0);
    }

    public boolean isExceeded() {
        return spent > monthlyLimit && monthlyLimit > 0;
    }

    @Override
    public String toString() {
        return String.format("Budget [%s %s: Limit=$%.2f, Spent=$%.2f (%.1f%%)]",
                category.getIcon(), category.getDisplayName(), monthlyLimit, spent, getPercentageUsed());
    }
}

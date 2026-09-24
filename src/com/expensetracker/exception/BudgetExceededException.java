package com.expensetracker.exception;

import com.expensetracker.model.Category;

/**
 * Thrown or signaled when an expense transaction causes a category budget to be exceeded.
 */
public class BudgetExceededException extends ExpenseTrackerException {

    private final Category category;
    private final double budgetLimit;
    private final double projectedTotal;

    public BudgetExceededException(Category category, double budgetLimit, double projectedTotal) {
        super(String.format("Budget exceeded for category '%s'! Limit: $%.2f, Projected Spend: $%.2f",
                category.getDisplayName(), budgetLimit, projectedTotal));
        this.category = category;
        this.budgetLimit = budgetLimit;
        this.projectedTotal = projectedTotal;
    }

    public Category getCategory() {
        return category;
    }

    public double getBudgetLimit() {
        return budgetLimit;
    }

    public double getProjectedTotal() {
        return projectedTotal;
    }
}

package com.expensetracker.exception;

/**
 * Thrown when an expense record cannot be found by its identifier.
 */
public class ExpenseNotFoundException extends ExpenseTrackerException {

    private final int expenseId;

    public ExpenseNotFoundException(int expenseId) {
        super("Expense with ID " + expenseId + " was not found.");
        this.expenseId = expenseId;
    }

    public int getExpenseId() {
        return expenseId;
    }
}

package com.expensetracker.exception;

/**
 * Base checked exception for domain-level errors in Personal Expense Tracker.
 * Demonstrates Java Exception Handling hierarchy.
 */
public class ExpenseTrackerException extends Exception {

    public ExpenseTrackerException(String message) {
        super(message);
    }

    public ExpenseTrackerException(String message, Throwable cause) {
        super(message, cause);
    }
}

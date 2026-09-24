package com.expensetracker.exception;

/**
 * Thrown when an invalid transaction or budget monetary amount is provided.
 */
public class InvalidAmountException extends ExpenseTrackerException {

    private final double amount;

    public InvalidAmountException(double amount, String reason) {
        super("Invalid amount (₹" + amount + "): " + reason);
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }
}

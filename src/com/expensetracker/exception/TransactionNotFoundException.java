package com.expensetracker.exception;

/**
 * Thrown when a transaction cannot be located by its identifier.
 */
public class TransactionNotFoundException extends ExpenseTrackerException {

    private final int transactionId;

    public TransactionNotFoundException(int transactionId) {
        super("Transaction with ID #" + transactionId + " was not found.");
        this.transactionId = transactionId;
    }

    public int getTransactionId() {
        return transactionId;
    }
}

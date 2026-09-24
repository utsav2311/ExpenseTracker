package com.expensetracker.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when transaction validation constraints fail.
 */
public class InvalidTransactionException extends ExpenseTrackerException {

    private final List<String> errors;

    public InvalidTransactionException(String message) {
        super(message);
        this.errors = Collections.singletonList(message);
    }

    public InvalidTransactionException(List<String> errors) {
        super("Validation failed: " + String.join("; ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}

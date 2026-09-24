package com.expensetracker.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when expense validation fails (e.g., negative amount, blank title).
 */
public class InvalidExpenseException extends ExpenseTrackerException {

    private final List<String> validationErrors;

    public InvalidExpenseException(String message) {
        super(message);
        this.validationErrors = Collections.singletonList(message);
    }

    public InvalidExpenseException(List<String> errors) {
        super("Validation failed: " + String.join("; ", errors));
        this.validationErrors = errors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}

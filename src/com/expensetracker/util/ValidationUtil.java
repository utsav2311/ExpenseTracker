package com.expensetracker.util;

import com.expensetracker.exception.InvalidExpenseException;
import com.expensetracker.model.Expense;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates expense entities against domain business rules.
 */
public final class ValidationUtil {

    private ValidationUtil() {
    }

    /**
     * Validates an expense and throws {@link InvalidExpenseException} if invalid.
     */
    public static void validateExpense(Expense expense) throws InvalidExpenseException {
        if (expense == null) {
            throw new InvalidExpenseException("Expense object cannot be null.");
        }

        List<String> errors = new ArrayList<>();

        if (expense.getTitle() == null || expense.getTitle().trim().isEmpty()) {
            errors.add("Title is required and cannot be empty.");
        } else if (expense.getTitle().trim().length() > 150) {
            errors.add("Title cannot exceed 150 characters.");
        }

        if (expense.getAmount() <= 0.0) {
            errors.add("Amount must be greater than zero.");
        } else if (expense.getAmount() > 10_000_000.0) {
            errors.add("Amount cannot exceed $10,000,000.00.");
        }

        if (expense.getCategory() == null) {
            errors.add("Expense category is required.");
        }

        if (expense.getPaymentMethod() == null) {
            errors.add("Payment method is required.");
        }

        if (expense.getDate() == null) {
            errors.add("Expense date is required.");
        } else if (expense.getDate().isAfter(LocalDate.now().plusDays(30))) {
            errors.add("Expense date cannot be more than 30 days in the future.");
        }

        if (expense.getNotes() != null && expense.getNotes().length() > 500) {
            errors.add("Notes cannot exceed 500 characters.");
        }

        if (!errors.isEmpty()) {
            throw new InvalidExpenseException(errors);
        }
    }
}

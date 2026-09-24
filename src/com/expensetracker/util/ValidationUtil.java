package com.expensetracker.util;

import com.expensetracker.exception.InvalidAmountException;
import com.expensetracker.exception.InvalidTransactionException;
import com.expensetracker.model.Category;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates financial transactions against strict business domain rules.
 */
public final class ValidationUtil {

    private ValidationUtil() {
    }

    /**
     * Validates a transaction object. Throws InvalidAmountException or InvalidTransactionException.
     */
    public static void validateTransaction(Transaction tx, Category category)
            throws InvalidAmountException, InvalidTransactionException {
        if (tx == null) {
            throw new InvalidTransactionException("Transaction data cannot be null.");
        }

        List<String> errors = new ArrayList<>();

        // Validate Amount
        if (tx.getAmount() <= 0.0) {
            throw new InvalidAmountException(tx.getAmount(), "Amount must be strictly greater than zero.");
        }
        if (tx.getAmount() > 100_000_000.0) {
            errors.add("Amount exceeds maximum allowed limit of ₹10,00,00,000.00.");
        }

        // Validate Description
        if (tx.getDescription() == null || tx.getDescription().trim().isEmpty()) {
            errors.add("Description is required and cannot be empty.");
        } else if (tx.getDescription().trim().length() > 150) {
            errors.add("Description cannot exceed 150 characters.");
        }

        // Validate Transaction Type
        if (tx.getTransactionType() == null) {
            errors.add("Transaction type (INCOME or EXPENSE) is required.");
        }

        // Validate Category
        if (tx.getCategoryId() <= 0 && category == null) {
            errors.add("Valid category is required.");
        }

        // Validate Category Type Matches Transaction Type
        if (category != null && tx.getTransactionType() != null) {
            if (category.getCategoryType() != tx.getTransactionType()) {
                errors.add(String.format("Category '%s' is an %s category, but transaction type is %s. " +
                                "An %s transaction cannot use category '%s'.",
                        category.getCategoryName(), category.getCategoryType(), tx.getTransactionType(),
                        tx.getTransactionType(), category.getCategoryName()));
            }
        }

        // Validate Date
        if (tx.getTransactionDate() == null) {
            errors.add("Transaction date is required.");
        } else if (tx.getTransactionDate().isAfter(LocalDate.now().plusDays(365))) {
            errors.add("Transaction date cannot be more than 1 year in the future.");
        }

        // Validate Notes length
        if (tx.getNotes() != null && tx.getNotes().length() > 500) {
            errors.add("Notes cannot exceed 500 characters.");
        }

        if (!errors.isEmpty()) {
            throw new InvalidTransactionException(errors);
        }
    }
}

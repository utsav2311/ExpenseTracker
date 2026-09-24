package com.expensetracker.dsa;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Structures & Algorithms: Multi-criteria filtering pipeline for Expenses.
 * Supports filtering by category, payment method, date range, and amount boundaries.
 */
public final class ExpenseFilter {

    private ExpenseFilter() {
    }

    public static List<Expense> filter(List<Expense> expenses,
                                       Category category,
                                       PaymentMethod paymentMethod,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       Double minAmount,
                                       Double maxAmount) {
        if (expenses == null) {
            return new ArrayList<>();
        }

        List<Expense> filtered = new ArrayList<>();
        for (Expense e : expenses) {
            if (category != null && e.getCategory() != category) {
                continue;
            }
            if (paymentMethod != null && e.getPaymentMethod() != paymentMethod) {
                continue;
            }
            if (startDate != null && e.getDate().isBefore(startDate)) {
                continue;
            }
            if (endDate != null && e.getDate().isAfter(endDate)) {
                continue;
            }
            if (minAmount != null && e.getAmount() < minAmount) {
                continue;
            }
            if (maxAmount != null && e.getAmount() > maxAmount) {
                continue;
            }
            filtered.add(e);
        }
        return filtered;
    }
}

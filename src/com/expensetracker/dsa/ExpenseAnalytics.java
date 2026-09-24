package com.expensetracker.dsa;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseSummary;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.util.DateTimeUtil;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data Structures & Algorithms: Aggregations, statistical metrics, and distribution analytics.
 * Demonstrates:
 *  - Java Collections: Maps (LinkedHashMap, TreeMap), iteration, and aggregation
 *  - Statistical computations: sums, averages, min/max finding
 *  - Percentage distribution calculations
 */
public final class ExpenseAnalytics {

    private ExpenseAnalytics() {
    }

    public static ExpenseSummary computeSummary(List<Expense> expenses) {
        ExpenseSummary summary = new ExpenseSummary();
        if (expenses == null || expenses.isEmpty()) {
            return summary;
        }

        double total = 0.0;
        Expense highest = expenses.get(0);
        Expense lowest = expenses.get(0);

        Map<Category, Double> categoryMap = new LinkedHashMap<>();
        Map<PaymentMethod, Double> paymentMap = new LinkedHashMap<>();
        Map<String, Double> monthMap = new TreeMap<>(); // sorted chronologically

        // Single pass O(N) aggregation
        for (Expense e : expenses) {
            double amt = e.getAmount();
            total += amt;

            // Highest & Lowest
            if (amt > highest.getAmount()) {
                highest = e;
            }
            if (amt < lowest.getAmount()) {
                lowest = e;
            }

            // Category breakdown
            Category cat = e.getCategory();
            categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + amt);

            // Payment method breakdown
            PaymentMethod pm = e.getPaymentMethod();
            paymentMap.put(pm, paymentMap.getOrDefault(pm, 0.0) + amt);

            // Monthly breakdown (YYYY-MM)
            String ym = DateTimeUtil.getMonthYear(e.getDate());
            monthMap.put(ym, monthMap.getOrDefault(ym, 0.0) + amt);
        }

        summary.setTotalExpenses(total);
        summary.setTotalCount(expenses.size());
        summary.setAverageExpense(total / expenses.size());
        summary.setHighestExpense(highest);
        summary.setLowestExpense(lowest);

        // Sort categories by spending descending
        Map<Category, Double> sortedCategories = new LinkedHashMap<>();
        categoryMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> sortedCategories.put(entry.getKey(), entry.getValue()));
        summary.setCategoryBreakdown(sortedCategories);

        // Calculate Category Percentages
        Map<Category, Double> percentages = new LinkedHashMap<>();
        if (total > 0) {
            for (Map.Entry<Category, Double> entry : sortedCategories.entrySet()) {
                double pct = (entry.getValue() / total) * 100.0;
                percentages.put(entry.getKey(), Math.round(pct * 10.0) / 10.0);
            }
        }
        summary.setCategoryPercentages(percentages);

        // Identify Top Category
        if (!sortedCategories.isEmpty()) {
            Map.Entry<Category, Double> first = sortedCategories.entrySet().iterator().next();
            summary.setTopCategory(first.getKey());
            summary.setTopCategoryAmount(first.getValue());
        }

        // Sort Payment Methods by spending descending
        Map<PaymentMethod, Double> sortedPayments = new LinkedHashMap<>();
        paymentMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> sortedPayments.put(entry.getKey(), entry.getValue()));
        summary.setPaymentMethodBreakdown(sortedPayments);

        summary.setMonthlyBreakdown(monthMap);

        return summary;
    }

    /**
     * Computes the total spent for a specific category within a given month.
     */
    public static double computeCategoryMonthlySpend(List<Expense> expenses, Category category, String monthYear) {
        if (expenses == null || category == null || monthYear == null) {
            return 0.0;
        }
        double sum = 0.0;
        for (Expense e : expenses) {
            if (e.getCategory() == category && monthYear.equals(DateTimeUtil.getMonthYear(e.getDate()))) {
                sum += e.getAmount();
            }
        }
        return Math.round(sum * 100.0) / 100.0;
    }
}

package com.expensetracker.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encapsulates analytical summary metrics of expenses.
 * Demonstrates use of Java Collections: Maps and aggregation structures.
 */
public class ExpenseSummary {

    private double totalExpenses;
    private int totalCount;
    private double averageExpense;
    private Expense highestExpense;
    private Expense lowestExpense;
    private Category topCategory;
    private double topCategoryAmount;
    private Map<Category, Double> categoryBreakdown = new LinkedHashMap<>();
    private Map<Category, Double> categoryPercentages = new LinkedHashMap<>();
    private Map<PaymentMethod, Double> paymentMethodBreakdown = new LinkedHashMap<>();
    private Map<String, Double> monthlyBreakdown = new LinkedHashMap<>();

    public ExpenseSummary() {
    }

    public double getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(double totalExpenses) {
        this.totalExpenses = Math.round(totalExpenses * 100.0) / 100.0;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public double getAverageExpense() {
        return averageExpense;
    }

    public void setAverageExpense(double averageExpense) {
        this.averageExpense = Math.round(averageExpense * 100.0) / 100.0;
    }

    public Expense getHighestExpense() {
        return highestExpense;
    }

    public void setHighestExpense(Expense highestExpense) {
        this.highestExpense = highestExpense;
    }

    public Expense getLowestExpense() {
        return lowestExpense;
    }

    public void setLowestExpense(Expense lowestExpense) {
        this.lowestExpense = lowestExpense;
    }

    public Category getTopCategory() {
        return topCategory;
    }

    public void setTopCategory(Category topCategory) {
        this.topCategory = topCategory;
    }

    public double getTopCategoryAmount() {
        return topCategoryAmount;
    }

    public void setTopCategoryAmount(double topCategoryAmount) {
        this.topCategoryAmount = Math.round(topCategoryAmount * 100.0) / 100.0;
    }

    public Map<Category, Double> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(Map<Category, Double> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public Map<Category, Double> getCategoryPercentages() {
        return categoryPercentages;
    }

    public void setCategoryPercentages(Map<Category, Double> categoryPercentages) {
        this.categoryPercentages = categoryPercentages;
    }

    public Map<PaymentMethod, Double> getPaymentMethodBreakdown() {
        return paymentMethodBreakdown;
    }

    public void setPaymentMethodBreakdown(Map<PaymentMethod, Double> paymentMethodBreakdown) {
        this.paymentMethodBreakdown = paymentMethodBreakdown;
    }

    public Map<String, Double> getMonthlyBreakdown() {
        return monthlyBreakdown;
    }

    public void setMonthlyBreakdown(Map<String, Double> monthlyBreakdown) {
        this.monthlyBreakdown = monthlyBreakdown;
    }
}

package com.expensetracker.model;

/**
 * Encapsulates spending/earnings aggregate for a specific category.
 */
public class CategorySummary {

    private int categoryId;
    private String categoryName;
    private TransactionType categoryType;
    private double totalAmount;
    private int transactionCount;
    private double percentage;
    private String icon;

    public CategorySummary() {
    }

    public CategorySummary(int categoryId, String categoryName, TransactionType categoryType,
                           double totalAmount, int transactionCount) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryType = categoryType;
        this.totalAmount = Math.round(totalAmount * 100.0) / 100.0;
        this.transactionCount = transactionCount;
        this.icon = Category.resolveIcon(categoryName);
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
        this.icon = Category.resolveIcon(categoryName);
    }

    public TransactionType getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(TransactionType categoryType) {
        this.categoryType = categoryType;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = Math.round(totalAmount * 100.0) / 100.0;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = Math.round(percentage * 10.0) / 10.0;
    }

    public String getIcon() {
        if (icon == null) {
            icon = Category.resolveIcon(categoryName);
        }
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}

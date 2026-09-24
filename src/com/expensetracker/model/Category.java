package com.expensetracker.model;

import java.util.Objects;

/**
 * Domain entity representing a financial category.
 * Each category is classified as either INCOME or EXPENSE.
 */
public class Category {

    private int categoryId;
    private String categoryName;
    private TransactionType categoryType;
    private String icon;

    public Category() {
    }

    public Category(int categoryId, String categoryName, TransactionType categoryType) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryType = categoryType;
        this.icon = resolveIcon(categoryName);
    }

    public Category(String categoryName, TransactionType categoryType) {
        this(0, categoryName, categoryType);
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
        this.categoryName = (categoryName != null) ? categoryName.trim() : "";
        this.icon = resolveIcon(this.categoryName);
    }

    public TransactionType getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(TransactionType categoryType) {
        this.categoryType = categoryType;
    }

    public String getIcon() {
        if (icon == null || icon.isEmpty()) {
            icon = resolveIcon(categoryName);
        }
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public static String resolveIcon(String name) {
        if (name == null) return "🏷️";
        String n = name.trim().toLowerCase();
        switch (n) {
            case "salary": return "💼";
            case "freelance": return "💻";
            case "bonus": return "🎁";
            case "investment": return "📈";
            case "other income": return "💰";
            case "food": return "🍔";
            case "travel": return "🚗";
            case "shopping": return "🛍️";
            case "bills": return "💡";
            case "entertainment": return "🎬";
            case "education": return "📚";
            case "health": return "💊";
            case "rent": return "🏠";
            case "other expense": return "📦";
            default: return "🏷️";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category category = (Category) o;
        return categoryId == category.categoryId ||
                (Objects.equals(categoryName, category.categoryName) && categoryType == category.categoryType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId, categoryName, categoryType);
    }

    @Override
    public String toString() {
        return String.format("%s %s (%s)", getIcon(), categoryName, categoryType);
    }
}

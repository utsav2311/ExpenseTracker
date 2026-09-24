package com.expensetracker.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Common domain entity representing a financial transaction (INCOME or EXPENSE).
 * Demonstrates:
 *  - Encapsulation: Private fields with validated getters and setters
 *  - Collections support: Implements Comparable and overrides equals/hashCode
 */
public class Transaction implements Comparable<Transaction> {

    private int transactionId;
    private int userId = 1;
    private int categoryId;
    private String categoryName;
    private String categoryIcon;
    private double amount;
    private TransactionType transactionType;
    private String description;
    private String notes;
    private String paymentMethod;
    private LocalDate transactionDate;
    private LocalDateTime createdAt;

    public Transaction() {
        this.transactionType = TransactionType.EXPENSE;
        this.paymentMethod = "Cash";
        this.transactionDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }

    public Transaction(int transactionId, int userId, int categoryId, double amount,
                       TransactionType transactionType, String description, String notes,
                       String paymentMethod, LocalDate transactionDate) {
        this();
        this.transactionId = transactionId;
        this.userId = userId;
        this.categoryId = categoryId;
        setAmount(amount);
        this.transactionType = (transactionType != null) ? transactionType : TransactionType.EXPENSE;
        setDescription(description);
        this.notes = (notes != null) ? notes.trim() : "";
        this.paymentMethod = (paymentMethod != null && !paymentMethod.trim().isEmpty()) ? paymentMethod.trim() : "Cash";
        this.transactionDate = (transactionDate != null) ? transactionDate : LocalDate.now();
    }

    public Transaction(int userId, int categoryId, double amount,
                       TransactionType transactionType, String description, String notes,
                       String paymentMethod, LocalDate transactionDate) {
        this(0, userId, categoryId, amount, transactionType, description, notes, paymentMethod, transactionDate);
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    // Helper for legacy id access if needed
    public int getId() {
        return transactionId;
    }

    public void setId(int id) {
        this.transactionId = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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
        if (categoryName != null) {
            this.categoryIcon = Category.resolveIcon(categoryName);
        }
    }

    public String getCategoryIcon() {
        if (categoryIcon == null && categoryName != null) {
            categoryIcon = Category.resolveIcon(categoryName);
        }
        return (categoryIcon != null) ? categoryIcon : "🏷️";
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Transaction amount must be strictly greater than zero.");
        }
        this.amount = Math.round(amount * 100.0) / 100.0;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = (transactionType != null) ? transactionType : TransactionType.EXPENSE;
    }

    public boolean isIncome() {
        return transactionType == TransactionType.INCOME;
    }

    public boolean isExpense() {
        return transactionType == TransactionType.EXPENSE;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction description cannot be empty.");
        }
        this.description = description.trim();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = (notes != null) ? notes.trim() : "";
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = (paymentMethod != null && !paymentMethod.trim().isEmpty()) ? paymentMethod.trim() : "Cash";
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = (transactionDate != null) ? transactionDate : LocalDate.now();
    }

    // Helper for date access
    public LocalDate getDate() {
        return transactionDate;
    }

    public void setDate(LocalDate date) {
        this.transactionDate = (date != null) ? date : LocalDate.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Default natural ordering: sorts descending by transaction date, then descending by ID.
     */
    @Override
    public int compareTo(Transaction other) {
        if (other == null) return 1;
        int dateCmp = other.transactionDate.compareTo(this.transactionDate);
        if (dateCmp != 0) return dateCmp;
        return Integer.compare(other.transactionId, this.transactionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return transactionId != 0 && transactionId == that.transactionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return String.format("%s #%d: %s | ₹%.2f | %s | %s | %s",
                transactionType, transactionId, description, amount,
                (categoryName != null ? categoryName : ("Cat#" + categoryId)),
                paymentMethod, transactionDate);
    }
}

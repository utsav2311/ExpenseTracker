package com.expensetracker.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Concrete Expense domain model extending {@link Transaction}.
 * Demonstrates:
 *  - Inheritance: inherits ID, amount, date, notes, createdAt
 *  - Encapsulation: validated setters and private fields
 *  - Polymorphism: implements getTransactionType()
 *  - Collections contract: properly overrides equals() and hashCode()
 */
public class Expense extends Transaction implements Comparable<Expense> {

    private String title;
    private Category category;
    private PaymentMethod paymentMethod;

    public Expense() {
        super();
        this.category = Category.OTHER;
        this.paymentMethod = PaymentMethod.CASH;
    }

    public Expense(int id, String title, double amount, Category category,
                   PaymentMethod paymentMethod, LocalDate date, String notes) {
        super(id, amount, date, notes);
        setTitle(title);
        setCategory(category);
        setPaymentMethod(paymentMethod);
    }

    public Expense(String title, double amount, Category category,
                   PaymentMethod paymentMethod, LocalDate date, String notes) {
        this(0, title, amount, category, paymentMethod, date, notes);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Expense title cannot be empty.");
        }
        this.title = title.trim();
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = (category != null) ? category : Category.OTHER;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = (paymentMethod != null) ? paymentMethod : PaymentMethod.CASH;
    }

    @Override
    public String getTransactionType() {
        return "EXPENSE";
    }

    /**
     * Default natural ordering: sorts descending by date, then descending by ID.
     */
    @Override
    public int compareTo(Expense other) {
        if (other == null) return 1;
        int dateCompare = other.getDate().compareTo(this.getDate());
        if (dateCompare != 0) {
            return dateCompare;
        }
        return Integer.compare(other.getId(), this.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Expense)) return false;
        Expense expense = (Expense) o;
        return id != 0 && id == expense.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Expense #%d: %s | $%.2f | %s %s | %s | %s",
                id, title, amount, category.getIcon(), category.getDisplayName(),
                paymentMethod.getDisplayName(), date);
    }
}

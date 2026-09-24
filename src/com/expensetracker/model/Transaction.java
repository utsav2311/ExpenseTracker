package com.expensetracker.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Abstract base class representing a general financial transaction.
 * Demonstrates Object-Oriented Principles:
 *  - Abstraction: Defines common attributes & contracts for financial records
 *  - Encapsulation: Private members exposed via getters and setters with validation
 *  - Inheritance: Base class for concrete classes like Expense
 */
public abstract class Transaction {

    protected int id;
    protected double amount;
    protected LocalDate date;
    protected String notes;
    protected LocalDateTime createdAt;

    public Transaction() {
        this.createdAt = LocalDateTime.now();
        this.date = LocalDate.now();
    }

    public Transaction(int id, double amount, LocalDate date, String notes) {
        this();
        setId(id);
        setAmount(amount);
        setDate(date);
        setNotes(notes);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Transaction amount cannot be negative.");
        }
        this.amount = Math.round(amount * 100.0) / 100.0;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = (date != null) ? date : LocalDate.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = (notes != null) ? notes.trim() : "";
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    /**
     * Abstract method to be implemented by concrete subclasses.
     * Demonstrates Polymorphism.
     */
    public abstract String getTransactionType();

    @Override
    public String toString() {
        return String.format("%s [ID=%d, Amount=%.2f, Date=%s]",
                getTransactionType(), id, amount, date);
    }
}

package com.expensetracker.dao;

import com.expensetracker.exception.DatabaseOperationException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object (DAO) interface defining persistence operations for Expenses and Budgets.
 * Demonstrates:
 *  - Interface Segregation and Abstraction: Decouples business logic from persistence implementation
 *  - Polymorphism: Multiple implementations (JDBC MySQL and In-Memory Collections)
 */
public interface ExpenseDAO {

    /**
     * Inserts a new Expense into the data store and assigns its generated ID.
     */
    void add(Expense expense) throws DatabaseOperationException;

    /**
     * Finds an expense by its unique identifier.
     */
    Expense findById(int id) throws DatabaseOperationException;

    /**
     * Retrieves all expenses from the data store.
     */
    List<Expense> findAll() throws DatabaseOperationException;

    /**
     * Updates an existing expense. Returns true if updated, false if not found.
     */
    boolean update(Expense expense) throws DatabaseOperationException;

    /**
     * Deletes an expense by ID. Returns true if deleted, false if not found.
     */
    boolean delete(int id) throws DatabaseOperationException;

    /**
     * Retrieves expenses belonging to a specific category.
     */
    List<Expense> findByCategory(Category category) throws DatabaseOperationException;

    /**
     * Retrieves expenses within a specific date range (inclusive).
     */
    List<Expense> findByDateRange(LocalDate startDate, LocalDate endDate) throws DatabaseOperationException;

    /**
     * Retrieves all saved budgets.
     */
    List<Budget> findAllBudgets() throws DatabaseOperationException;

    /**
     * Sets or updates a category budget.
     */
    void saveBudget(Budget budget) throws DatabaseOperationException;

    /**
     * Deletes a budget by ID.
     */
    boolean deleteBudget(int id) throws DatabaseOperationException;

    /**
     * Checks if the underlying persistence mechanism is actively reachable.
     */
    boolean isAvailable();

    /**
     * Returns a human-readable name of the persistence layer ("MySQL via JDBC" or "In-Memory Java Collections").
     */
    String getStorageType();
}

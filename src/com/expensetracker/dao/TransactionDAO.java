package com.expensetracker.dao;

import com.expensetracker.exception.DatabaseOperationException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategorySummary;
import com.expensetracker.model.DashboardSummary;
import com.expensetracker.model.MonthlySummary;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object (DAO) interface defining persistence operations for
 * Transactions, Categories, Budgets, and SQL Analytical Reports.
 * Demonstrates the DAO design pattern and abstraction.
 */
public interface TransactionDAO {

    // =========================================================================
    //  TRANSACTION CRUD OPERATIONS
    // =========================================================================

    void addTransaction(Transaction tx) throws DatabaseOperationException;

    Transaction getTransactionById(int id) throws DatabaseOperationException;

    List<Transaction> getAllTransactions() throws DatabaseOperationException;

    boolean updateTransaction(Transaction tx) throws DatabaseOperationException;

    boolean deleteTransaction(int id) throws DatabaseOperationException;

    List<Transaction> findByType(TransactionType type) throws DatabaseOperationException;

    List<Transaction> findByCategory(int categoryId) throws DatabaseOperationException;

    List<Transaction> findByDateRange(LocalDate startDate, LocalDate endDate) throws DatabaseOperationException;

    List<Transaction> getRecentTransactions(int limit) throws DatabaseOperationException;

    List<Transaction> getTopExpenses(int limit) throws DatabaseOperationException;

    // =========================================================================
    //  CATEGORY OPERATIONS
    // =========================================================================

    List<Category> getAllCategories() throws DatabaseOperationException;

    List<Category> getCategoriesByType(TransactionType type) throws DatabaseOperationException;

    Category getCategoryById(int categoryId) throws DatabaseOperationException;

    // =========================================================================
    //  BUDGET OPERATIONS
    // =========================================================================

    Budget getBudget(int userId, int month, int year) throws DatabaseOperationException;

    void saveBudget(Budget budget) throws DatabaseOperationException;

    // =========================================================================
    //  ANALYTICAL & REPORTING OPERATIONS (SQL Aggregations)
    // =========================================================================

    DashboardSummary getDashboardSummary(int month, int year) throws DatabaseOperationException;

    MonthlySummary getMonthlySummary(int month, int year) throws DatabaseOperationException;

    List<CategorySummary> getCategoryExpenseReport(int month, int year) throws DatabaseOperationException;

    // =========================================================================
    //  HEALTH & METADATA
    // =========================================================================

    boolean isAvailable();

    String getStorageType();
}

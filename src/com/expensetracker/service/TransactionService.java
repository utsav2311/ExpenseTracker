package com.expensetracker.service;

import com.expensetracker.dao.InMemoryTransactionDAO;
import com.expensetracker.dao.TransactionDAO;
import com.expensetracker.dao.TransactionDAOImpl;
import com.expensetracker.dsa.TransactionSearcher;
import com.expensetracker.dsa.TransactionSorter;
import com.expensetracker.exception.DatabaseOperationException;
import com.expensetracker.exception.InvalidAmountException;
import com.expensetracker.exception.InvalidTransactionException;
import com.expensetracker.exception.TransactionNotFoundException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategorySummary;
import com.expensetracker.model.DashboardSummary;
import com.expensetracker.model.MonthlySummary;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.util.DatabaseConfig;
import com.expensetracker.util.DateTimeUtil;
import com.expensetracker.util.ValidationUtil;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Service Layer coordinating business logic, strict validations,
 * persistence strategy, and reporting.
 */
public class TransactionService {

    private final TransactionDAO activeDao;
    private final boolean databaseAvailable;

    public TransactionService() {
        if (DatabaseConfig.isMemoryModeExplicitlyRequested()) {
            System.out.println("[TransactionService] Explicitly configured for In-Memory storage (storage.mode=memory).");
            this.activeDao = new InMemoryTransactionDAO();
            this.databaseAvailable = false;
        } else {
            TransactionDAO jdbcDao = null;
            boolean connected = false;
            try {
                jdbcDao = new TransactionDAOImpl();
                connected = jdbcDao.isAvailable();
            } catch (Throwable t) {
                connected = false;
            }

            if (connected) {
                System.out.println("[TransactionService] Connected to MySQL database via JDBC.");
                this.activeDao = jdbcDao;
                this.databaseAvailable = true;
            } else {
                System.err.println("[TransactionService] Unable to connect to MySQL database at " + DatabaseConfig.getUrl());
                System.err.println("Please check that MySQL is running and database credentials are configured in db.properties.");
                // We keep the jdbcDao reference so operations will throw explicit informative DatabaseOperationException
                this.activeDao = (jdbcDao != null) ? jdbcDao : new TransactionDAOImpl();
                this.databaseAvailable = false;
            }
        }
    }

    public TransactionService(TransactionDAO dao) {
        this.activeDao = dao;
        this.databaseAvailable = dao.isAvailable();
    }

    public boolean isDatabaseAvailable() {
        return databaseAvailable;
    }

    public String getStorageType() {
        return activeDao.getStorageType();
    }

    private void ensureStorageAvailable() throws DatabaseOperationException {
        if (!DatabaseConfig.isMemoryModeExplicitlyRequested() && !activeDao.isAvailable()) {
            throw new DatabaseOperationException(
                    "Unable to connect to database. Please check that MySQL is running and database credentials are configured."
            );
        }
    }

    // =========================================================================
    //  TRANSACTION CRUD
    // =========================================================================

    public Transaction addTransaction(Transaction tx)
            throws InvalidAmountException, InvalidTransactionException, DatabaseOperationException {
        ensureStorageAvailable();

        if (tx == null) {
            throw new InvalidTransactionException("Transaction data cannot be null.");
        }

        // Fetch category to validate that category type matches transaction type
        Category category = activeDao.getCategoryById(tx.getCategoryId());
        if (category == null) {
            throw new InvalidTransactionException("Category with ID " + tx.getCategoryId() + " does not exist.");
        }

        // Run thorough validation
        ValidationUtil.validateTransaction(tx, category);

        tx.setCategoryName(category.getCategoryName());
        activeDao.addTransaction(tx);
        return tx;
    }

    public Transaction getTransactionById(int id)
            throws TransactionNotFoundException, DatabaseOperationException {
        ensureStorageAvailable();
        Transaction tx = activeDao.getTransactionById(id);
        if (tx == null) {
            throw new TransactionNotFoundException(id);
        }
        return tx;
    }

    public Transaction updateTransaction(int id, Transaction updated)
            throws TransactionNotFoundException, InvalidAmountException, InvalidTransactionException, DatabaseOperationException {
        ensureStorageAvailable();

        Transaction existing = activeDao.getTransactionById(id);
        if (existing == null) {
            throw new TransactionNotFoundException(id);
        }

        Category category = activeDao.getCategoryById(updated.getCategoryId());
        if (category == null) {
            throw new InvalidTransactionException("Category with ID " + updated.getCategoryId() + " does not exist.");
        }

        updated.setTransactionId(id);
        ValidationUtil.validateTransaction(updated, category);

        updated.setCategoryName(category.getCategoryName());
        boolean success = activeDao.updateTransaction(updated);
        if (!success) {
            throw new TransactionNotFoundException(id);
        }
        return updated;
    }

    public boolean deleteTransaction(int id)
            throws TransactionNotFoundException, DatabaseOperationException {
        ensureStorageAvailable();
        boolean deleted = activeDao.deleteTransaction(id);
        if (!deleted) {
            throw new TransactionNotFoundException(id);
        }
        return true;
    }

    // =========================================================================
    //  SEARCH, FILTER, AND SORT PIPELINE
    // =========================================================================

    public List<Transaction> getFilteredTransactions(TransactionType type,
                                                     Integer categoryId,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     String query,
                                                     String sortField) throws DatabaseOperationException {
        ensureStorageAvailable();

        // 1. Fetch base list
        List<Transaction> list;
        if (type != null && (categoryId == null || categoryId <= 0) && startDate == null && endDate == null) {
            list = activeDao.findByType(type);
        } else if (categoryId != null && categoryId > 0 && type == null && startDate == null && endDate == null) {
            list = activeDao.findByCategory(categoryId);
        } else if (startDate != null && endDate != null && type == null && (categoryId == null || categoryId <= 0)) {
            list = activeDao.findByDateRange(startDate, endDate);
        } else {
            list = activeDao.getAllTransactions();
        }

        // 2. In-memory filter pipeline (demonstrates Java Collections & Filtering)
        List<Transaction> filtered = TransactionSearcher.filter(list, type, categoryId, startDate, endDate);

        // 3. Search matching
        List<Transaction> searched = (query != null && !query.trim().isEmpty())
                ? TransactionSearcher.linearSearch(filtered, query)
                : filtered;

        // 4. Sort using Comparator & Collections.sort()
        Comparator<Transaction> cmp = TransactionSorter.getComparator(sortField);
        return TransactionSorter.sort(searched, cmp);
    }

    public List<Transaction> getAllTransactions() throws DatabaseOperationException {
        ensureStorageAvailable();
        return activeDao.getAllTransactions();
    }

    public List<Transaction> getRecentTransactions(int limit) throws DatabaseOperationException {
        ensureStorageAvailable();
        return activeDao.getRecentTransactions(limit);
    }

    public List<Transaction> getTopExpenses(int limit) throws DatabaseOperationException {
        ensureStorageAvailable();
        return activeDao.getTopExpenses(limit);
    }

    // =========================================================================
    //  CATEGORIES
    // =========================================================================

    public List<Category> getAllCategories() throws DatabaseOperationException {
        ensureStorageAvailable();
        return activeDao.getAllCategories();
    }

    public List<Category> getCategoriesByType(TransactionType type) throws DatabaseOperationException {
        ensureStorageAvailable();
        if (type == null) {
            return activeDao.getAllCategories();
        }
        return activeDao.getCategoriesByType(type);
    }

    // =========================================================================
    //  BUDGET MANAGEMENT
    // =========================================================================

    public Budget getBudget(int month, int year) throws DatabaseOperationException {
        ensureStorageAvailable();
        DashboardSummary ds = activeDao.getDashboardSummary(month, year);
        Budget b = activeDao.getBudget(1, month, year);
        b.setSpent(ds.getTotalExpense());
        return b;
    }

    public void saveBudget(Budget budget) throws InvalidAmountException, DatabaseOperationException {
        ensureStorageAvailable();
        if (budget.getBudgetAmount() < 0) {
            throw new InvalidAmountException(budget.getBudgetAmount(), "Budget amount cannot be negative.");
        }
        if (budget.getMonth() < 1 || budget.getMonth() > 12) {
            budget.setMonth(LocalDate.now().getMonthValue());
        }
        if (budget.getYear() < 2000) {
            budget.setYear(LocalDate.now().getYear());
        }
        activeDao.saveBudget(budget);
    }

    // =========================================================================
    //  DASHBOARD & REPORTS
    // =========================================================================

    public DashboardSummary getDashboardSummary(int month, int year) throws DatabaseOperationException {
        ensureStorageAvailable();
        return activeDao.getDashboardSummary(month, year);
    }

    public MonthlySummary getMonthlySummary(int month, int year) throws DatabaseOperationException {
        ensureStorageAvailable();
        return activeDao.getMonthlySummary(month, year);
    }

    public List<CategorySummary> getCategoryExpenseReport(int month, int year) throws DatabaseOperationException {
        ensureStorageAvailable();
        return activeDao.getCategoryExpenseReport(month, year);
    }

    // =========================================================================
    //  CSV EXPORT
    // =========================================================================

    public String generateCsvExport() throws DatabaseOperationException {
        ensureStorageAvailable();
        List<Transaction> transactions = activeDao.getAllTransactions();
        StringBuilder sb = new StringBuilder();
        sb.append("Transaction ID,Type,Description,Amount (INR),Category,Payment Method,Date,Notes\n");

        for (Transaction t : transactions) {
            sb.append(t.getTransactionId()).append(",");
            sb.append(t.getTransactionType().name()).append(",");
            sb.append("\"").append(t.getDescription().replace("\"", "\"\"")).append("\",");
            sb.append(String.format(java.util.Locale.US, "%.2f", t.getAmount())).append(",");
            sb.append("\"").append(t.getCategoryName() != null ? t.getCategoryName().replace("\"", "\"\"") : "").append("\",");
            sb.append("\"").append(t.getPaymentMethod().replace("\"", "\"\"")).append("\",");
            sb.append(DateTimeUtil.formatDate(t.getTransactionDate())).append(",");
            sb.append("\"").append(t.getNotes() != null ? t.getNotes().replace("\"", "\"\"") : "").append("\"\n");
        }
        return sb.toString();
    }
}

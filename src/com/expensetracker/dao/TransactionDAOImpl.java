package com.expensetracker.dao;

import com.expensetracker.exception.DatabaseOperationException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategorySummary;
import com.expensetracker.model.DashboardSummary;
import com.expensetracker.model.MonthlySummary;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JDBC implementation of {@link TransactionDAO} using MySQL.
 * Demonstrates:
 *  - Database connection lifecycle management using DriverManager
 *  - 100% Parameterized PreparedStatements to eliminate SQL Injection
 *  - Generated Keys retrieval with Statement.RETURN_GENERATED_KEYS
 *  - Explicit ACID transaction management (conn.setAutoCommit(false), commit(), rollback())
 *  - SQL Aggregations and JOIN queries for high-performance server-side reporting
 *  - Safe resource cleanup via try-with-resources
 */
public class TransactionDAOImpl implements TransactionDAO {

    public TransactionDAOImpl() {
        initSchemaQuietly();
    }

    private void initSchemaQuietly() {
        if (!isAvailable()) return;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create users table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(150) NOT NULL UNIQUE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // Seed default user
            stmt.executeUpdate("INSERT IGNORE INTO users (user_id, name, email) VALUES (1, 'Utsav Kumar', 'utsav@example.com')");

            // Create categories table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS categories (" +
                    "category_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "category_name VARCHAR(100) NOT NULL, " +
                    "category_type ENUM('INCOME', 'EXPENSE') NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY uk_cat_name_type (category_name, category_type)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // Seed default categories
            stmt.executeUpdate("INSERT IGNORE INTO categories (category_name, category_type) VALUES " +
                    "('Salary', 'INCOME'), ('Freelance', 'INCOME'), ('Bonus', 'INCOME'), ('Investment', 'INCOME'), ('Other Income', 'INCOME'), " +
                    "('Food', 'EXPENSE'), ('Travel', 'EXPENSE'), ('Shopping', 'EXPENSE'), ('Bills', 'EXPENSE'), ('Entertainment', 'EXPENSE'), " +
                    "('Education', 'EXPENSE'), ('Health', 'EXPENSE'), ('Rent', 'EXPENSE'), ('Other Expense', 'EXPENSE')");

            // Create transactions table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transactions (" +
                    "transaction_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "category_id INT NOT NULL, " +
                    "amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0), " +
                    "transaction_type ENUM('INCOME', 'EXPENSE') NOT NULL, " +
                    "description VARCHAR(150) NOT NULL, " +
                    "notes TEXT, " +
                    "payment_method VARCHAR(50) NOT NULL, " +
                    "transaction_date DATE NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE RESTRICT, " +
                    "INDEX idx_user_tx (user_id), " +
                    "INDEX idx_category_tx (category_id), " +
                    "INDEX idx_tx_date (transaction_date), " +
                    "INDEX idx_tx_type (transaction_type)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // Create budgets table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS budgets (" +
                    "budget_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "month INT NOT NULL CHECK (month BETWEEN 1 AND 12), " +
                    "year INT NOT NULL CHECK (year >= 2000), " +
                    "budget_amount DECIMAL(10, 2) NOT NULL CHECK (budget_amount >= 0), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY uk_user_month_year (user_id, month, year), " +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        } catch (Exception e) {
            System.err.println("[TransactionDAOImpl] Schema check notice: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return DatabaseConnection.testConnection();
    }

    @Override
    public String getStorageType() {
        return "MySQL via JDBC";
    }

    // =========================================================================
    //  TRANSACTION CRUD
    // =========================================================================

    @Override
    public void addTransaction(Transaction tx) throws DatabaseOperationException {
        String sql = "INSERT INTO transactions (user_id, category_id, amount, transaction_type, " +
                     "description, notes, payment_method, transaction_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, tx.getUserId() > 0 ? tx.getUserId() : 1);
                ps.setInt(2, tx.getCategoryId());
                ps.setDouble(3, tx.getAmount());
                ps.setString(4, tx.getTransactionType().name());
                ps.setString(5, tx.getDescription());
                ps.setString(6, tx.getNotes());
                ps.setString(7, tx.getPaymentMethod());
                ps.setDate(8, Date.valueOf(tx.getTransactionDate()));

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected == 0) {
                    throw new DatabaseOperationException("Inserting transaction failed, 0 rows affected.");
                }

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        tx.setTransactionId(keys.getInt(1));
                    }
                }

                conn.commit(); // Commit transaction
            }
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new DatabaseOperationException("Failed to insert transaction: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(conn);
        }
    }

    @Override
    public Transaction getTransactionById(int id) throws DatabaseOperationException {
        String sql = "SELECT t.transaction_id, t.user_id, t.category_id, c.category_name, t.amount, " +
                     "t.transaction_type, t.description, t.notes, t.payment_method, t.transaction_date, t.created_at " +
                     "FROM transactions t " +
                     "JOIN categories c ON t.category_id = c.category_id " +
                     "WHERE t.transaction_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTransaction(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find transaction #" + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> getAllTransactions() throws DatabaseOperationException {
        String sql = "SELECT t.transaction_id, t.user_id, t.category_id, c.category_name, t.amount, " +
                     "t.transaction_type, t.description, t.notes, t.payment_method, t.transaction_date, t.created_at " +
                     "FROM transactions t " +
                     "JOIN categories c ON t.category_id = c.category_id " +
                     "ORDER BY t.transaction_date DESC, t.transaction_id DESC";

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToTransaction(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to retrieve transactions: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateTransaction(Transaction tx) throws DatabaseOperationException {
        String sql = "UPDATE transactions SET category_id = ?, amount = ?, transaction_type = ?, " +
                     "description = ?, notes = ?, payment_method = ?, transaction_date = ? " +
                     "WHERE transaction_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, tx.getCategoryId());
                ps.setDouble(2, tx.getAmount());
                ps.setString(3, tx.getTransactionType().name());
                ps.setString(4, tx.getDescription());
                ps.setString(5, tx.getNotes());
                ps.setString(6, tx.getPaymentMethod());
                ps.setDate(7, Date.valueOf(tx.getTransactionDate()));
                ps.setInt(8, tx.getTransactionId());

                int rows = ps.executeUpdate();
                conn.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new DatabaseOperationException("Failed to update transaction #" + tx.getTransactionId() + ": " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(conn);
        }
    }

    @Override
    public boolean deleteTransaction(int id) throws DatabaseOperationException {
        String sql = "DELETE FROM transactions WHERE transaction_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                conn.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new DatabaseOperationException("Failed to delete transaction #" + id + ": " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(conn);
        }
    }

    @Override
    public List<Transaction> findByType(TransactionType type) throws DatabaseOperationException {
        String sql = "SELECT t.transaction_id, t.user_id, t.category_id, c.category_name, t.amount, " +
                     "t.transaction_type, t.description, t.notes, t.payment_method, t.transaction_date, t.created_at " +
                     "FROM transactions t " +
                     "JOIN categories c ON t.category_id = c.category_id " +
                     "WHERE t.transaction_type = ? " +
                     "ORDER BY t.transaction_date DESC, t.transaction_id DESC";

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to filter transactions by type: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> findByCategory(int categoryId) throws DatabaseOperationException {
        String sql = "SELECT t.transaction_id, t.user_id, t.category_id, c.category_name, t.amount, " +
                     "t.transaction_type, t.description, t.notes, t.payment_method, t.transaction_date, t.created_at " +
                     "FROM transactions t " +
                     "JOIN categories c ON t.category_id = c.category_id " +
                     "WHERE t.category_id = ? " +
                     "ORDER BY t.transaction_date DESC, t.transaction_id DESC";

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to filter transactions by category: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> findByDateRange(LocalDate startDate, LocalDate endDate) throws DatabaseOperationException {
        String sql = "SELECT t.transaction_id, t.user_id, t.category_id, c.category_name, t.amount, " +
                     "t.transaction_type, t.description, t.notes, t.payment_method, t.transaction_date, t.created_at " +
                     "FROM transactions t " +
                     "JOIN categories c ON t.category_id = c.category_id " +
                     "WHERE t.transaction_date BETWEEN ? AND ? " +
                     "ORDER BY t.transaction_date DESC, t.transaction_id DESC";

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to filter transactions by date range: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> getRecentTransactions(int limit) throws DatabaseOperationException {
        String sql = "SELECT t.transaction_id, t.user_id, t.category_id, c.category_name, t.amount, " +
                     "t.transaction_type, t.description, t.notes, t.payment_method, t.transaction_date, t.created_at " +
                     "FROM transactions t " +
                     "JOIN categories c ON t.category_id = c.category_id " +
                     "ORDER BY t.transaction_date DESC, t.transaction_id DESC LIMIT ?";

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch recent transactions: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> getTopExpenses(int limit) throws DatabaseOperationException {
        String sql = "SELECT t.transaction_id, t.user_id, t.category_id, c.category_name, t.amount, " +
                     "t.transaction_type, t.description, t.notes, t.payment_method, t.transaction_date, t.created_at " +
                     "FROM transactions t " +
                     "JOIN categories c ON t.category_id = c.category_id " +
                     "WHERE t.transaction_type = 'EXPENSE' " +
                     "ORDER BY t.amount DESC LIMIT ?";

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch top expenses: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    //  CATEGORY OPERATIONS
    // =========================================================================

    @Override
    public List<Category> getAllCategories() throws DatabaseOperationException {
        String sql = "SELECT category_id, category_name, category_type FROM categories ORDER BY category_type ASC, category_name ASC";
        List<Category> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Category(
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        TransactionType.fromString(rs.getString("category_type"))
                ));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to load categories: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Category> getCategoriesByType(TransactionType type) throws DatabaseOperationException {
        String sql = "SELECT category_id, category_name, category_type FROM categories WHERE category_type = ? ORDER BY category_name ASC";
        List<Category> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Category(
                            rs.getInt("category_id"),
                            rs.getString("category_name"),
                            TransactionType.fromString(rs.getString("category_type"))
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to load categories by type: " + e.getMessage(), e);
        }
    }

    @Override
    public Category getCategoryById(int categoryId) throws DatabaseOperationException {
        String sql = "SELECT category_id, category_name, category_type FROM categories WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Category(
                            rs.getInt("category_id"),
                            rs.getString("category_name"),
                            TransactionType.fromString(rs.getString("category_type"))
                    );
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find category #" + categoryId + ": " + e.getMessage(), e);
        }
    }

    // =========================================================================
    //  BUDGET OPERATIONS
    // =========================================================================

    @Override
    public Budget getBudget(int userId, int month, int year) throws DatabaseOperationException {
        String sql = "SELECT budget_id, user_id, month, year, budget_amount, created_at " +
                     "FROM budgets WHERE user_id = ? AND month = ? AND year = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId > 0 ? userId : 1);
            ps.setInt(2, month);
            ps.setInt(3, year);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Budget b = new Budget(
                            rs.getInt("budget_id"),
                            rs.getInt("user_id"),
                            rs.getInt("month"),
                            rs.getInt("year"),
                            rs.getDouble("budget_amount")
                    );
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) b.setCreatedAt(ts.toLocalDateTime());
                    return b;
                }
            }
            // Return zero budget object if not found
            return new Budget(0, userId > 0 ? userId : 1, month, year, 0.0);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch budget: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveBudget(Budget budget) throws DatabaseOperationException {
        String sql = "INSERT INTO budgets (user_id, month, year, budget_amount) VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE budget_amount = VALUES(budget_amount)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, budget.getUserId() > 0 ? budget.getUserId() : 1);
            ps.setInt(2, budget.getMonth());
            ps.setInt(3, budget.getYear());
            ps.setDouble(4, budget.getBudgetAmount());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    budget.setBudgetId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to save budget: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    //  ANALYTICAL SQL REPORTS
    // =========================================================================

    @Override
    public DashboardSummary getDashboardSummary(int month, int year) throws DatabaseOperationException {
        DashboardSummary dashboard = new DashboardSummary();

        // Query 1: Total Income & Total Expense across all transactions for User 1
        String sqlTotals = "SELECT " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0.00) AS total_income, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0.00) AS total_expense, " +
                "COUNT(*) AS total_count " +
                "FROM transactions WHERE user_id = 1";

        // Query 2: Monthly expenses strictly in this month (for budget progress)
        String sqlMonthlyExpense = "SELECT COALESCE(SUM(amount), 0.00) AS monthly_expense " +
                "FROM transactions WHERE user_id = 1 AND transaction_type = 'EXPENSE' " +
                "AND MONTH(transaction_date) = ? AND YEAR(transaction_date) = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Totals
            try (PreparedStatement ps = conn.prepareStatement(sqlTotals);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double income = rs.getDouble("total_income");
                    double expense = rs.getDouble("total_expense");
                    dashboard.setTotalIncome(income);
                    dashboard.setTotalExpense(expense);
                    dashboard.setBalance(income - expense);
                    dashboard.setTransactionCount(rs.getInt("total_count"));
                }
            }

            // Monthly Expense for Budget calculation
            double monthlyExpense = 0.0;
            try (PreparedStatement ps = conn.prepareStatement(sqlMonthlyExpense)) {
                ps.setInt(1, month);
                ps.setInt(2, year);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        monthlyExpense = rs.getDouble("monthly_expense");
                    }
                }
            }

            // Budget for target month
            Budget budget = getBudget(1, month, year);
            budget.setSpent(monthlyExpense);
            dashboard.setMonthlyBudget(budget.getBudgetAmount());
            dashboard.setBudgetRemaining(budget.getRemaining());
            dashboard.setBudgetPercentage(budget.getPercentageUsed());

            // Recent 5 Transactions
            dashboard.setRecentTransactions(getRecentTransactions(5));

            return dashboard;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to generate dashboard summary: " + e.getMessage(), e);
        }
    }

    @Override
    public MonthlySummary getMonthlySummary(int month, int year) throws DatabaseOperationException {
        MonthlySummary summary = new MonthlySummary();
        summary.setMonth(month);
        summary.setYear(year);
        summary.setMonthName(Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year);

        String sqlAgg = "SELECT " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0.00) AS month_income, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0.00) AS month_expense, " +
                "COUNT(*) AS tx_count " +
                "FROM transactions WHERE user_id = 1 AND MONTH(transaction_date) = ? AND YEAR(transaction_date) = ?";

        String sqlHighestExp = "SELECT t.transaction_id, t.user_id, t.category_id, c.category_name, t.amount, " +
                "t.transaction_type, t.description, t.notes, t.payment_method, t.transaction_date, t.created_at " +
                "FROM transactions t JOIN categories c ON t.category_id = c.category_id " +
                "WHERE t.user_id = 1 AND t.transaction_type = 'EXPENSE' " +
                "AND MONTH(t.transaction_date) = ? AND YEAR(t.transaction_date) = ? " +
                "ORDER BY t.amount DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlAgg)) {
                ps.setInt(1, month);
                ps.setInt(2, year);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        summary.setIncome(rs.getDouble("month_income"));
                        summary.setExpenses(rs.getDouble("month_expense"));
                        summary.setTransactionCount(rs.getInt("tx_count"));
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlHighestExp)) {
                ps.setInt(1, month);
                ps.setInt(2, year);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        summary.setHighestExpense(mapRowToTransaction(rs));
                    }
                }
            }

            // Top spending category for this month
            List<CategorySummary> catReports = getCategoryExpenseReport(month, year);
            if (!catReports.isEmpty()) {
                CategorySummary top = catReports.get(0);
                summary.setHighestSpendingCategory(top.getCategoryName());
                summary.setHighestSpendingCategoryAmount(top.getTotalAmount());
            }

            // Budget calculation
            Budget budget = getBudget(1, month, year);
            summary.setBudgetAmount(budget.getBudgetAmount());

            return summary;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to generate monthly summary: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CategorySummary> getCategoryExpenseReport(int month, int year) throws DatabaseOperationException {
        String sql = "SELECT c.category_id, c.category_name, c.category_type, " +
                     "COALESCE(SUM(t.amount), 0.00) AS total_amount, COUNT(t.transaction_id) AS tx_count " +
                     "FROM transactions t " +
                     "JOIN categories c ON t.category_id = c.category_id " +
                     "WHERE t.user_id = 1 AND t.transaction_type = 'EXPENSE' " +
                     "AND MONTH(t.transaction_date) = ? AND YEAR(t.transaction_date) = ? " +
                     "GROUP BY c.category_id, c.category_name, c.category_type " +
                     "ORDER BY total_amount DESC";

        List<CategorySummary> list = new ArrayList<>();
        double grandTotal = 0.0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, month);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double amt = rs.getDouble("total_amount");
                    grandTotal += amt;
                    list.add(new CategorySummary(
                            rs.getInt("category_id"),
                            rs.getString("category_name"),
                            TransactionType.fromString(rs.getString("category_type")),
                            amt,
                            rs.getInt("tx_count")
                    ));
                }
            }

            // Compute percentage for each category
            if (grandTotal > 0) {
                for (CategorySummary cs : list) {
                    double pct = (cs.getTotalAmount() / grandTotal) * 100.0;
                    cs.setPercentage(Math.round(pct * 10.0) / 10.0);
                }
            }

            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch category report: " + e.getMessage(), e);
        }
    }

    private Transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        Transaction tx = new Transaction();
        tx.setTransactionId(rs.getInt("transaction_id"));
        tx.setUserId(rs.getInt("user_id"));
        tx.setCategoryId(rs.getInt("category_id"));
        tx.setCategoryName(rs.getString("category_name"));
        tx.setAmount(rs.getDouble("amount"));
        tx.setTransactionType(TransactionType.fromString(rs.getString("transaction_type")));
        tx.setDescription(rs.getString("description"));
        tx.setNotes(rs.getString("notes"));
        tx.setPaymentMethod(rs.getString("payment_method"));
        tx.setTransactionDate(rs.getDate("transaction_date").toLocalDate());

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            tx.setCreatedAt(ts.toLocalDateTime());
        }
        return tx;
    }
}

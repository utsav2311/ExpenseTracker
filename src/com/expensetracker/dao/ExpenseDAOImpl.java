package com.expensetracker.dao;

import com.expensetracker.exception.DatabaseOperationException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of {@link ExpenseDAO} using MySQL.
 * Demonstrates:
 *  - JDBC best practices: Connection pooling/lifecycle, PreparedStatement for SQL injection defense
 *  - Generated Keys retrieval with Statement.RETURN_GENERATED_KEYS
 *  - Explicit transaction management (setAutoCommit(false), commit(), rollback())
 *  - Resource management with try-with-resources
 */
public class ExpenseDAOImpl implements ExpenseDAO {

    public ExpenseDAOImpl() {
        initSchemaQuietly();
    }

    private void initSchemaQuietly() {
        if (!isAvailable()) return;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create expenses table if missing
            String createExpenses = "CREATE TABLE IF NOT EXISTS expenses (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "title VARCHAR(150) NOT NULL, " +
                    "amount DECIMAL(10, 2) NOT NULL, " +
                    "category VARCHAR(50) NOT NULL, " +
                    "payment_method VARCHAR(50) NOT NULL, " +
                    "expense_date DATE NOT NULL, " +
                    "notes TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "INDEX idx_category (category), " +
                    "INDEX idx_date (expense_date)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            stmt.executeUpdate(createExpenses);

            // Create budgets table if missing
            String createBudgets = "CREATE TABLE IF NOT EXISTS budgets (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "category VARCHAR(50) NOT NULL, " +
                    "monthly_limit DECIMAL(10, 2) NOT NULL, " +
                    "month_year VARCHAR(7) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY uk_cat_month (category, month_year)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            stmt.executeUpdate(createBudgets);

        } catch (Exception e) {
            System.err.println("[ExpenseDAOImpl] Schema initialization notice: " + e.getMessage());
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

    @Override
    public void add(Expense expense) throws DatabaseOperationException {
        String sql = "INSERT INTO expenses (title, amount, category, payment_method, expense_date, notes, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, expense.getTitle());
                ps.setDouble(2, expense.getAmount());
                ps.setString(3, expense.getCategory().name());
                ps.setString(4, expense.getPaymentMethod().name());
                ps.setDate(5, Date.valueOf(expense.getDate()));
                ps.setString(6, expense.getNotes());
                ps.setTimestamp(7, Timestamp.valueOf(expense.getCreatedAt() != null ? expense.getCreatedAt() : LocalDateTime.now()));

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected == 0) {
                    throw new DatabaseOperationException("Inserting expense failed, no rows affected.");
                }

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        expense.setId(generatedKeys.getInt(1));
                    }
                }

                conn.commit(); // Commit transaction
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException ex) {
                    System.err.println("[ExpenseDAOImpl] Rollback failed: " + ex.getMessage());
                }
            }
            throw new DatabaseOperationException("Failed to add expense: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(conn);
        }
    }

    @Override
    public Expense findById(int id) throws DatabaseOperationException {
        String sql = "SELECT id, title, amount, category, payment_method, expense_date, notes, created_at " +
                     "FROM expenses WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToExpense(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching expense with ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Expense> findAll() throws DatabaseOperationException {
        String sql = "SELECT id, title, amount, category, payment_method, expense_date, notes, created_at " +
                     "FROM expenses ORDER BY expense_date DESC, id DESC";

        List<Expense> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToExpense(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all expenses: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Expense expense) throws DatabaseOperationException {
        String sql = "UPDATE expenses SET title = ?, amount = ?, category = ?, payment_method = ?, " +
                     "expense_date = ?, notes = ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, expense.getTitle());
                ps.setDouble(2, expense.getAmount());
                ps.setString(3, expense.getCategory().name());
                ps.setString(4, expense.getPaymentMethod().name());
                ps.setDate(5, Date.valueOf(expense.getDate()));
                ps.setString(6, expense.getNotes());
                ps.setInt(7, expense.getId());

                int rows = ps.executeUpdate();
                conn.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new DatabaseOperationException("Failed to update expense #" + expense.getId() + ": " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(conn);
        }
    }

    @Override
    public boolean delete(int id) throws DatabaseOperationException {
        String sql = "DELETE FROM expenses WHERE id = ?";

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
            throw new DatabaseOperationException("Failed to delete expense #" + id + ": " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(conn);
        }
    }

    @Override
    public List<Expense> findByCategory(Category category) throws DatabaseOperationException {
        String sql = "SELECT id, title, amount, category, payment_method, expense_date, notes, created_at " +
                     "FROM expenses WHERE category = ? ORDER BY expense_date DESC";

        List<Expense> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, category.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToExpense(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving expenses by category: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Expense> findByDateRange(LocalDate startDate, LocalDate endDate) throws DatabaseOperationException {
        String sql = "SELECT id, title, amount, category, payment_method, expense_date, notes, created_at " +
                     "FROM expenses WHERE expense_date BETWEEN ? AND ? ORDER BY expense_date DESC";

        List<Expense> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToExpense(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving expenses by date range: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Budget> findAllBudgets() throws DatabaseOperationException {
        String sql = "SELECT id, category, monthly_limit, month_year FROM budgets ORDER BY category ASC";
        List<Budget> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Budget b = new Budget(
                        rs.getInt("id"),
                        Category.fromString(rs.getString("category")),
                        rs.getDouble("monthly_limit"),
                        rs.getString("month_year")
                );
                list.add(b);
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching budgets: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveBudget(Budget budget) throws DatabaseOperationException {
        String sql = "INSERT INTO budgets (category, monthly_limit, month_year) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE monthly_limit = VALUES(monthly_limit)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, budget.getCategory().name());
            ps.setDouble(2, budget.getMonthlyLimit());
            ps.setString(3, budget.getMonthYear());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    budget.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to save budget: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteBudget(int id) throws DatabaseOperationException {
        String sql = "DELETE FROM budgets WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to delete budget #" + id + ": " + e.getMessage(), e);
        }
    }

    private Expense mapRowToExpense(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        double amount = rs.getDouble("amount");
        Category category = Category.fromString(rs.getString("category"));
        PaymentMethod pm = PaymentMethod.fromString(rs.getString("payment_method"));
        LocalDate date = rs.getDate("expense_date").toLocalDate();
        String notes = rs.getString("notes");

        Expense expense = new Expense(id, title, amount, category, pm, date, notes);

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            expense.setCreatedAt(ts.toLocalDateTime());
        }
        return expense;
    }
}

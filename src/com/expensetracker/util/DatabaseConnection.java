package com.expensetracker.util;

import com.expensetracker.exception.DatabaseOperationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages JDBC connections to MySQL database.
 * Demonstrates JDBC driver loading, connection lifecycle, and exception wrapping.
 */
public final class DatabaseConnection {

    private static boolean driverLoaded = false;

    static {
        try {
            Class.forName(DatabaseConfig.getDriver());
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            // Driver may not be present on classpath; application will fallback gracefully to In-Memory mode.
            System.err.println("[DatabaseConnection] Notice: MySQL JDBC Driver (" + DatabaseConfig.getDriver() + ") is not found on classpath.");
            driverLoaded = false;
        }
    }

    private DatabaseConnection() {
    }

    public static boolean isDriverAvailable() {
        return driverLoaded;
    }

    /**
     * Obtains a fresh JDBC Connection from DriverManager.
     *
     * @return active java.sql.Connection
     * @throws DatabaseOperationException if connection cannot be established
     */
    public static Connection getConnection() throws DatabaseOperationException {
        if (!driverLoaded) {
            throw new DatabaseOperationException("MySQL JDBC Driver is not loaded on the classpath.");
        }
        try {
            return DriverManager.getConnection(
                    DatabaseConfig.getUrl(),
                    DatabaseConfig.getUser(),
                    DatabaseConfig.getPassword()
            );
        } catch (SQLException e) {
            throw new DatabaseOperationException(
                    "Unable to connect to MySQL database at " + DatabaseConfig.getUrl() + ". " +
                    "Reason: " + e.getMessage(), e
            );
        }
    }

    /**
     * Tests if the database connection can be established successfully.
     *
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        if (!driverLoaded) {
            return false;
        }
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Safely closes one or more AutoCloseable resources (Connection, Statement, ResultSet).
     */
    public static void closeQuietly(AutoCloseable... closeables) {
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}

package com.expensetracker.util;

import com.expensetracker.exception.DatabaseOperationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages database connection lifecycle using standard JDBC DriverManager.
 * Note: This project intentionally demonstrates fundamental JDBC without third-party
 * connection pooling libraries (such as HikariCP or Apache DBCP) to showcase core Java mechanics.
 */
public final class DatabaseConnection {

    private static boolean driverLoaded = false;

    static {
        loadDriver();
    }

    private static void loadDriver() {
        try {
            Class.forName(DatabaseConfig.getDriver());
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            driverLoaded = false;
        }
    }

    private DatabaseConnection() {
    }

    public static boolean isDriverAvailable() {
        if (!driverLoaded) {
            loadDriver();
        }
        return driverLoaded;
    }

    /**
     * Obtains a fresh JDBC Connection from DriverManager.
     *
     * @return active java.sql.Connection
     * @throws DatabaseOperationException if connection cannot be established
     */
    public static Connection getConnection() throws DatabaseOperationException {
        if (!isDriverAvailable()) {
            throw new DatabaseOperationException(
                    "MySQL JDBC Driver (" + DatabaseConfig.getDriver() + ") is not found on the classpath. " +
                    "Please ensure mysql-connector-j-*.jar is placed in the lib/ directory."
            );
        }
        try {
            return DriverManager.getConnection(
                    DatabaseConfig.getUrl(),
                    DatabaseConfig.getUser(),
                    DatabaseConfig.getPassword()
            );
        } catch (SQLException e) {
            throw new DatabaseOperationException(
                    "Unable to connect to database at " + DatabaseConfig.getUrl() + ". " +
                    "Please check that MySQL is running and database credentials are configured in db.properties. " +
                    "Error: " + e.getMessage(), e
            );
        }
    }

    /**
     * Tests if the database connection can be established successfully.
     *
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        if (!isDriverAvailable()) {
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

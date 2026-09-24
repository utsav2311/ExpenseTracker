package com.expensetracker.exception;

/**
 * Exception indicating an issue while interacting with the database via JDBC.
 * Wraps low-level java.sql.SQLException.
 */
public class DatabaseOperationException extends ExpenseTrackerException {

    public DatabaseOperationException(String message) {
        super(message);
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.expensetracker.model;

/**
 * Enumeration representing transaction type: INCOME or EXPENSE.
 */
public enum TransactionType {
    INCOME("Income", "+"),
    EXPENSE("Expense", "-");

    private final String displayName;
    private final String sign;

    TransactionType(String displayName, String sign) {
        this.displayName = displayName;
        this.sign = sign;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSign() {
        return sign;
    }

    public static TransactionType fromString(String val) {
        if (val == null || val.trim().isEmpty()) {
            return EXPENSE;
        }
        String clean = val.trim().toUpperCase();
        for (TransactionType t : values()) {
            if (t.name().equals(clean) || t.displayName.equalsIgnoreCase(val.trim())) {
                return t;
            }
        }
        return EXPENSE;
    }
}

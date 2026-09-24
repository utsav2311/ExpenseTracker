package com.expensetracker.model;

/**
 * Enumeration representing supported payment methods.
 */
public enum PaymentMethod {
    CASH("Cash", "💵"),
    CREDIT_CARD("Credit Card", "💳"),
    DEBIT_CARD("Debit Card", "🏧"),
    UPI("UPI / Instant Transfer", "📱"),
    NET_BANKING("Net Banking", "🏦");

    private final String displayName;
    private final String icon;

    PaymentMethod(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public static PaymentMethod fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return CASH;
        }
        String clean = text.trim().toUpperCase().replace(" ", "_").replace("/", "_");
        for (PaymentMethod pm : values()) {
            if (pm.name().equalsIgnoreCase(clean) || pm.displayName.equalsIgnoreCase(text.trim())) {
                return pm;
            }
        }
        return CASH;
    }
}

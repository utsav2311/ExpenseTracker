package com.expensetracker.model;

/**
 * Enumeration representing predefined expense categories.
 * Demonstrates Java Enum features, custom fields, and static helper lookups.
 */
public enum Category {
    FOOD("Food & Dining", "🍔"),
    TRANSPORTATION("Transportation", "🚗"),
    HOUSING("Housing & Rent", "🏠"),
    UTILITIES("Utilities & Bills", "💡"),
    ENTERTAINMENT("Entertainment", "🎬"),
    HEALTHCARE("Healthcare & Medical", "💊"),
    SHOPPING("Shopping", "🛍️"),
    EDUCATION("Education", "📚"),
    PERSONAL("Personal Care", "✂️"),
    OTHER("Other / Miscellaneous", "📦");

    private final String displayName;
    private final String icon;

    Category(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    /**
     * Safely parse a Category from a case-insensitive string.
     * Fallback to OTHER if unrecognized.
     */
    public static Category fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return OTHER;
        }
        String clean = text.trim().toUpperCase().replace(" ", "_").replace("&", "").replace("/", "_");
        for (Category c : values()) {
            if (c.name().equalsIgnoreCase(clean) || c.displayName.equalsIgnoreCase(text.trim())) {
                return c;
            }
        }
        return OTHER;
    }
}

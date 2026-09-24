package com.expensetracker.util;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategorySummary;
import com.expensetracker.model.DashboardSummary;
import com.expensetracker.model.MonthlySummary;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure Core Java JSON parser and serializer with zero external dependencies.
 * Implements recursive-descent tokenization and serialization for domain entities.
 */
public final class SimpleJson {

    private SimpleJson() {
    }

    // =========================================================================
    //  SERIALIZATION (Java Object -> JSON String)
    // =========================================================================

    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + escape((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof LocalDate) {
            return "\"" + DateTimeUtil.formatDate((LocalDate) obj) + "\"";
        }
        if (obj instanceof LocalDateTime) {
            return "\"" + DateTimeUtil.formatTimestamp((LocalDateTime) obj) + "\"";
        }
        if (obj instanceof Enum<?>) {
            return "\"" + ((Enum<?>) obj).name() + "\"";
        }
        if (obj instanceof Transaction) {
            return transactionToJson((Transaction) obj);
        }
        if (obj instanceof Category) {
            return categoryToJson((Category) obj);
        }
        if (obj instanceof Budget) {
            return budgetToJson((Budget) obj);
        }
        if (obj instanceof DashboardSummary) {
            return dashboardSummaryToJson((DashboardSummary) obj);
        }
        if (obj instanceof MonthlySummary) {
            return monthlySummaryToJson((MonthlySummary) obj);
        }
        if (obj instanceof CategorySummary) {
            return categorySummaryToJson((CategorySummary) obj);
        }
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (i++ > 0) sb.append(",");
                sb.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Iterable<?>) {
            StringBuilder sb = new StringBuilder("[");
            int i = 0;
            for (Object item : (Iterable<?>) obj) {
                if (i++ > 0) sb.append(",");
                sb.append(toJson(item));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escape(obj.toString()) + "\"";
    }

    public static String transactionToJson(Transaction t) {
        if (t == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"transactionId\":").append(t.getTransactionId()).append(",");
        sb.append("\"id\":").append(t.getTransactionId()).append(",");
        sb.append("\"userId\":").append(t.getUserId()).append(",");
        sb.append("\"categoryId\":").append(t.getCategoryId()).append(",");
        sb.append("\"categoryName\":").append(toJson(t.getCategoryName())).append(",");
        sb.append("\"categoryIcon\":").append(toJson(t.getCategoryIcon())).append(",");
        sb.append("\"amount\":").append(String.format(java.util.Locale.US, "%.2f", t.getAmount())).append(",");
        sb.append("\"transactionType\":").append(toJson(t.getTransactionType().name())).append(",");
        sb.append("\"type\":").append(toJson(t.getTransactionType().name())).append(",");
        sb.append("\"typeSign\":").append(toJson(t.getTransactionType().getSign())).append(",");
        sb.append("\"description\":").append(toJson(t.getDescription())).append(",");
        sb.append("\"notes\":").append(toJson(t.getNotes())).append(",");
        sb.append("\"paymentMethod\":").append(toJson(t.getPaymentMethod())).append(",");
        sb.append("\"transactionDate\":").append(toJson(t.getTransactionDate())).append(",");
        sb.append("\"date\":").append(toJson(t.getTransactionDate())).append(",");
        sb.append("\"displayDate\":").append(toJson(DateTimeUtil.formatDisplayDate(t.getTransactionDate()))).append(",");
        sb.append("\"createdAt\":").append(toJson(t.getCreatedAt()));
        sb.append("}");
        return sb.toString();
    }

    public static String categoryToJson(Category c) {
        if (c == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"categoryId\":").append(c.getCategoryId()).append(",");
        sb.append("\"id\":").append(c.getCategoryId()).append(",");
        sb.append("\"categoryName\":").append(toJson(c.getCategoryName())).append(",");
        sb.append("\"name\":").append(toJson(c.getCategoryName())).append(",");
        sb.append("\"categoryType\":").append(toJson(c.getCategoryType().name())).append(",");
        sb.append("\"type\":").append(toJson(c.getCategoryType().name())).append(",");
        sb.append("\"icon\":").append(toJson(c.getIcon()));
        sb.append("}");
        return sb.toString();
    }

    public static String budgetToJson(Budget b) {
        if (b == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"budgetId\":").append(b.getBudgetId()).append(",");
        sb.append("\"userId\":").append(b.getUserId()).append(",");
        sb.append("\"month\":").append(b.getMonth()).append(",");
        sb.append("\"year\":").append(b.getYear()).append(",");
        sb.append("\"budgetAmount\":").append(String.format(java.util.Locale.US, "%.2f", b.getBudgetAmount())).append(",");
        sb.append("\"spent\":").append(String.format(java.util.Locale.US, "%.2f", b.getSpent())).append(",");
        sb.append("\"remaining\":").append(String.format(java.util.Locale.US, "%.2f", b.getRemaining())).append(",");
        sb.append("\"percentageUsed\":").append(String.format(java.util.Locale.US, "%.1f", b.getPercentageUsed())).append(",");
        sb.append("\"isExceeded\":").append(b.isExceeded()).append(",");
        sb.append("\"exceededAmount\":").append(String.format(java.util.Locale.US, "%.2f", b.getExceededAmount()));
        sb.append("}");
        return sb.toString();
    }

    public static String dashboardSummaryToJson(DashboardSummary d) {
        if (d == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"balance\":").append(String.format(java.util.Locale.US, "%.2f", d.getBalance())).append(",");
        sb.append("\"totalIncome\":").append(String.format(java.util.Locale.US, "%.2f", d.getTotalIncome())).append(",");
        sb.append("\"totalExpense\":").append(String.format(java.util.Locale.US, "%.2f", d.getTotalExpense())).append(",");
        sb.append("\"monthlyBudget\":").append(String.format(java.util.Locale.US, "%.2f", d.getMonthlyBudget())).append(",");
        sb.append("\"budgetRemaining\":").append(String.format(java.util.Locale.US, "%.2f", d.getBudgetRemaining())).append(",");
        sb.append("\"budgetPercentage\":").append(String.format(java.util.Locale.US, "%.1f", d.getBudgetPercentage())).append(",");
        sb.append("\"transactionCount\":").append(d.getTransactionCount()).append(",");
        sb.append("\"recentTransactions\":").append(toJson(d.getRecentTransactions()));
        sb.append("}");
        return sb.toString();
    }

    public static String monthlySummaryToJson(MonthlySummary m) {
        if (m == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"month\":").append(m.getMonth()).append(",");
        sb.append("\"year\":").append(m.getYear()).append(",");
        sb.append("\"monthName\":").append(toJson(m.getMonthName())).append(",");
        sb.append("\"income\":").append(String.format(java.util.Locale.US, "%.2f", m.getIncome())).append(",");
        sb.append("\"expenses\":").append(String.format(java.util.Locale.US, "%.2f", m.getExpenses())).append(",");
        sb.append("\"savings\":").append(String.format(java.util.Locale.US, "%.2f", m.getSavings())).append(",");
        sb.append("\"transactionCount\":").append(m.getTransactionCount()).append(",");
        sb.append("\"highestExpense\":").append(transactionToJson(m.getHighestExpense())).append(",");
        sb.append("\"highestSpendingCategory\":").append(toJson(m.getHighestSpendingCategory())).append(",");
        sb.append("\"highestSpendingCategoryAmount\":").append(String.format(java.util.Locale.US, "%.2f", m.getHighestSpendingCategoryAmount())).append(",");
        sb.append("\"budgetAmount\":").append(String.format(java.util.Locale.US, "%.2f", m.getBudgetAmount())).append(",");
        sb.append("\"remainingBudget\":").append(String.format(java.util.Locale.US, "%.2f", m.getRemainingBudget())).append(",");
        sb.append("\"budgetPercentageUsed\":").append(String.format(java.util.Locale.US, "%.1f", m.getBudgetPercentageUsed()));
        sb.append("}");
        return sb.toString();
    }

    public static String categorySummaryToJson(CategorySummary cs) {
        if (cs == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"categoryId\":").append(cs.getCategoryId()).append(",");
        sb.append("\"categoryName\":").append(toJson(cs.getCategoryName())).append(",");
        sb.append("\"categoryType\":").append(toJson(cs.getCategoryType().name())).append(",");
        sb.append("\"totalAmount\":").append(String.format(java.util.Locale.US, "%.2f", cs.getTotalAmount())).append(",");
        sb.append("\"transactionCount\":").append(cs.getTransactionCount()).append(",");
        sb.append("\"percentage\":").append(String.format(java.util.Locale.US, "%.1f", cs.getPercentage())).append(",");
        sb.append("\"icon\":").append(toJson(cs.getIcon()));
        sb.append("}");
        return sb.toString();
    }

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // =========================================================================
    //  PARSING (JSON String -> Java Objects)
    // =========================================================================

    public static Object parse(String json) {
        if (json == null) return null;
        String trimmed = json.trim();
        if (trimmed.isEmpty()) return null;
        Parser parser = new Parser(trimmed);
        return parser.parseValue();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object result = parse(json);
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }
        return new LinkedHashMap<>();
    }

    /**
     * Parses incoming JSON payload into Transaction domain entity.
     */
    public static Transaction parseTransaction(String json) {
        Map<String, Object> map = parseObject(json);
        Transaction tx = new Transaction();

        if (map.containsKey("transactionId") && map.get("transactionId") instanceof Number) {
            tx.setTransactionId(((Number) map.get("transactionId")).intValue());
        } else if (map.containsKey("id") && map.get("id") instanceof Number) {
            tx.setTransactionId(((Number) map.get("id")).intValue());
        }

        if (map.containsKey("userId") && map.get("userId") instanceof Number) {
            tx.setUserId(((Number) map.get("userId")).intValue());
        } else {
            tx.setUserId(1);
        }

        if (map.containsKey("categoryId") && map.get("categoryId") instanceof Number) {
            tx.setCategoryId(((Number) map.get("categoryId")).intValue());
        }

        if (map.containsKey("description") && map.get("description") != null) {
            tx.setDescription(map.get("description").toString());
        } else if (map.containsKey("title") && map.get("title") != null) {
            tx.setDescription(map.get("title").toString());
        }

        if (map.containsKey("amount")) {
            Object amtObj = map.get("amount");
            if (amtObj instanceof Number) {
                tx.setAmount(((Number) amtObj).doubleValue());
            } else if (amtObj != null) {
                try {
                    tx.setAmount(Double.parseDouble(amtObj.toString().trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        if (map.containsKey("transactionType") && map.get("transactionType") != null) {
            tx.setTransactionType(TransactionType.fromString(map.get("transactionType").toString()));
        } else if (map.containsKey("type") && map.get("type") != null) {
            tx.setTransactionType(TransactionType.fromString(map.get("type").toString()));
        }

        if (map.containsKey("paymentMethod") && map.get("paymentMethod") != null) {
            tx.setPaymentMethod(map.get("paymentMethod").toString());
        }

        if (map.containsKey("transactionDate") && map.get("transactionDate") != null) {
            tx.setTransactionDate(DateTimeUtil.parseDate(map.get("transactionDate").toString()));
        } else if (map.containsKey("date") && map.get("date") != null) {
            tx.setTransactionDate(DateTimeUtil.parseDate(map.get("date").toString()));
        }

        if (map.containsKey("notes") && map.get("notes") != null) {
            tx.setNotes(map.get("notes").toString());
        }

        return tx;
    }

    /**
     * Parses incoming JSON payload into Budget domain entity.
     */
    public static Budget parseBudget(String json) {
        Map<String, Object> map = parseObject(json);
        Budget b = new Budget();

        if (map.containsKey("budgetId") && map.get("budgetId") instanceof Number) {
            b.setBudgetId(((Number) map.get("budgetId")).intValue());
        }
        if (map.containsKey("userId") && map.get("userId") instanceof Number) {
            b.setUserId(((Number) map.get("userId")).intValue());
        } else {
            b.setUserId(1);
        }

        LocalDate now = LocalDate.now();
        if (map.containsKey("month") && map.get("month") instanceof Number) {
            b.setMonth(((Number) map.get("month")).intValue());
        } else {
            b.setMonth(now.getMonthValue());
        }

        if (map.containsKey("year") && map.get("year") instanceof Number) {
            b.setYear(((Number) map.get("year")).intValue());
        } else {
            b.setYear(now.getYear());
        }

        if (map.containsKey("budgetAmount")) {
            Object amtObj = map.get("budgetAmount");
            if (amtObj instanceof Number) {
                b.setBudgetAmount(((Number) amtObj).doubleValue());
            } else if (amtObj != null) {
                try {
                    b.setBudgetAmount(Double.parseDouble(amtObj.toString().trim()));
                } catch (NumberFormatException ignored) {}
            }
        } else if (map.containsKey("amount")) {
            Object amtObj = map.get("amount");
            if (amtObj instanceof Number) {
                b.setBudgetAmount(((Number) amtObj).doubleValue());
            } else if (amtObj != null) {
                try {
                    b.setBudgetAmount(Double.parseDouble(amtObj.toString().trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        return b;
    }

    // =========================================================================
    //  RECURSIVE DESCENT PARSER IMPLEMENTATION
    // =========================================================================

    private static class Parser {
        private final String src;
        private int pos = 0;

        Parser(String src) {
            this.src = src;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) return null;
            char c = src.charAt(pos);

            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (c == '-' || Character.isDigit(c)) return parseNumber();

            throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + pos);
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            consume('{');
            skipWhitespace();

            if (pos < src.length() && src.charAt(pos) == '}') {
                pos++;
                return map;
            }

            while (pos < src.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                consume(':');
                Object val = parseValue();
                map.put(key, val);

                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == '}') {
                    pos++;
                    break;
                }
                consume(',');
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            consume('[');
            skipWhitespace();

            if (pos < src.length() && src.charAt(pos) == ']') {
                pos++;
                return list;
            }

            while (pos < src.length()) {
                Object val = parseValue();
                list.add(val);

                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ']') {
                    pos++;
                    break;
                }
                consume(',');
            }
            return list;
        }

        private String parseString() {
            consume('"');
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (pos >= src.length()) break;
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 <= src.length()) {
                                String hex = src.substring(pos, pos + 4);
                                sb.append((char) Integer.parseInt(hex, 16));
                                pos += 4;
                            }
                            break;
                        default:
                            sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Number parseNumber() {
            int start = pos;
            if (src.charAt(pos) == '-') pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;

            boolean isFloating = false;
            if (pos < src.length() && src.charAt(pos) == '.') {
                isFloating = true;
                pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }
            if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
                isFloating = true;
                pos++;
                if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }

            String numStr = src.substring(start, pos);
            try {
                if (isFloating) {
                    return Double.parseDouble(numStr);
                }
                long l = Long.parseLong(numStr);
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                    return (int) l;
                }
                return l;
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Expected boolean at pos " + pos);
        }

        private Object parseNull() {
            if (src.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Expected null at pos " + pos);
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        private void consume(char expected) {
            skipWhitespace();
            if (pos >= src.length() || src.charAt(pos) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + pos +
                        (pos < src.length() ? " but found '" + src.charAt(pos) + "'" : " (EOF)"));
            }
            pos++;
        }
    }
}

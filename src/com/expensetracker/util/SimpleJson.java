package com.expensetracker.util;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseSummary;
import com.expensetracker.model.PaymentMethod;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight JSON parser and serializer in Core Java (Zero external dependencies).
 * Demonstrates:
 *  - String manipulation and tokenization
 *  - Recursive descent parsing for JSON Objects and Arrays
 *  - Dynamic type mapping to Maps, Lists, Strings, and Numbers
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
        if (obj instanceof Category) {
            return "\"" + ((Category) obj).name() + "\"";
        }
        if (obj instanceof PaymentMethod) {
            return "\"" + ((PaymentMethod) obj).name() + "\"";
        }
        if (obj instanceof Expense) {
            return expenseToJson((Expense) obj);
        }
        if (obj instanceof Budget) {
            return budgetToJson((Budget) obj);
        }
        if (obj instanceof ExpenseSummary) {
            return summaryToJson((ExpenseSummary) obj);
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

    public static String expenseToJson(Expense e) {
        if (e == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":").append(e.getId()).append(",");
        sb.append("\"title\":").append(toJson(e.getTitle())).append(",");
        sb.append("\"amount\":").append(String.format(java.util.Locale.US, "%.2f", e.getAmount())).append(",");
        sb.append("\"category\":").append(toJson(e.getCategory().name())).append(",");
        sb.append("\"categoryName\":").append(toJson(e.getCategory().getDisplayName())).append(",");
        sb.append("\"categoryIcon\":").append(toJson(e.getCategory().getIcon())).append(",");
        sb.append("\"paymentMethod\":").append(toJson(e.getPaymentMethod().name())).append(",");
        sb.append("\"paymentMethodName\":").append(toJson(e.getPaymentMethod().getDisplayName())).append(",");
        sb.append("\"paymentMethodIcon\":").append(toJson(e.getPaymentMethod().getIcon())).append(",");
        sb.append("\"date\":").append(toJson(e.getDate())).append(",");
        sb.append("\"displayDate\":").append(toJson(DateTimeUtil.formatDisplayDate(e.getDate()))).append(",");
        sb.append("\"notes\":").append(toJson(e.getNotes())).append(",");
        sb.append("\"createdAt\":").append(toJson(e.getCreatedAt()));
        sb.append("}");
        return sb.toString();
    }

    public static String budgetToJson(Budget b) {
        if (b == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":").append(b.getId()).append(",");
        sb.append("\"category\":").append(toJson(b.getCategory().name())).append(",");
        sb.append("\"categoryName\":").append(toJson(b.getCategory().getDisplayName())).append(",");
        sb.append("\"categoryIcon\":").append(toJson(b.getCategory().getIcon())).append(",");
        sb.append("\"monthlyLimit\":").append(String.format(java.util.Locale.US, "%.2f", b.getMonthlyLimit())).append(",");
        sb.append("\"monthYear\":").append(toJson(b.getMonthYear())).append(",");
        sb.append("\"spent\":").append(String.format(java.util.Locale.US, "%.2f", b.getSpent())).append(",");
        sb.append("\"remaining\":").append(String.format(java.util.Locale.US, "%.2f", b.getRemaining())).append(",");
        sb.append("\"percentageUsed\":").append(String.format(java.util.Locale.US, "%.1f", b.getPercentageUsed())).append(",");
        sb.append("\"isExceeded\":").append(b.isExceeded());
        sb.append("}");
        return sb.toString();
    }

    public static String summaryToJson(ExpenseSummary s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"totalExpenses\":").append(String.format(java.util.Locale.US, "%.2f", s.getTotalExpenses())).append(",");
        sb.append("\"totalCount\":").append(s.getTotalCount()).append(",");
        sb.append("\"averageExpense\":").append(String.format(java.util.Locale.US, "%.2f", s.getAverageExpense())).append(",");

        sb.append("\"highestExpense\":").append(expenseToJson(s.getHighestExpense())).append(",");
        sb.append("\"lowestExpense\":").append(expenseToJson(s.getLowestExpense())).append(",");

        if (s.getTopCategory() != null) {
            sb.append("\"topCategory\":").append(toJson(s.getTopCategory().name())).append(",");
            sb.append("\"topCategoryName\":").append(toJson(s.getTopCategory().getDisplayName())).append(",");
            sb.append("\"topCategoryIcon\":").append(toJson(s.getTopCategory().getIcon())).append(",");
            sb.append("\"topCategoryAmount\":").append(String.format(java.util.Locale.US, "%.2f", s.getTopCategoryAmount())).append(",");
        } else {
            sb.append("\"topCategory\":null,");
            sb.append("\"topCategoryName\":null,");
            sb.append("\"topCategoryIcon\":null,");
            sb.append("\"topCategoryAmount\":0.0,");
        }

        // Category breakdown
        sb.append("\"categoryBreakdown\":{");
        int i = 0;
        for (Map.Entry<Category, Double> entry : s.getCategoryBreakdown().entrySet()) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(entry.getKey().name()).append("\":").append(String.format(java.util.Locale.US, "%.2f", entry.getValue()));
        }
        sb.append("},");

        // Category percentages
        sb.append("\"categoryPercentages\":{");
        i = 0;
        for (Map.Entry<Category, Double> entry : s.getCategoryPercentages().entrySet()) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(entry.getKey().name()).append("\":").append(String.format(java.util.Locale.US, "%.1f", entry.getValue()));
        }
        sb.append("},");

        // Payment method breakdown
        sb.append("\"paymentMethodBreakdown\":{");
        i = 0;
        for (Map.Entry<PaymentMethod, Double> entry : s.getPaymentMethodBreakdown().entrySet()) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(entry.getKey().name()).append("\":").append(String.format(java.util.Locale.US, "%.2f", entry.getValue()));
        }
        sb.append("},");

        // Monthly breakdown
        sb.append("\"monthlyBreakdown\":{");
        i = 0;
        for (Map.Entry<String, Double> entry : s.getMonthlyBreakdown().entrySet()) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(escape(entry.getKey())).append("\":").append(String.format(java.util.Locale.US, "%.2f", entry.getValue()));
        }
        sb.append("}");

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
    //  PARSING (JSON String -> Map / List / Object)
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
     * Helper to deserialize JSON object into Expense entity.
     */
    public static Expense parseExpense(String json) {
        Map<String, Object> map = parseObject(json);
        Expense expense = new Expense();

        if (map.containsKey("id") && map.get("id") instanceof Number) {
            expense.setId(((Number) map.get("id")).intValue());
        }
        if (map.containsKey("title") && map.get("title") != null) {
            expense.setTitle(map.get("title").toString());
        }
        if (map.containsKey("amount")) {
            Object amtObj = map.get("amount");
            if (amtObj instanceof Number) {
                expense.setAmount(((Number) amtObj).doubleValue());
            } else if (amtObj != null) {
                try {
                    expense.setAmount(Double.parseDouble(amtObj.toString().trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (map.containsKey("category") && map.get("category") != null) {
            expense.setCategory(Category.fromString(map.get("category").toString()));
        }
        if (map.containsKey("paymentMethod") && map.get("paymentMethod") != null) {
            expense.setPaymentMethod(PaymentMethod.fromString(map.get("paymentMethod").toString()));
        }
        if (map.containsKey("date") && map.get("date") != null) {
            expense.setDate(DateTimeUtil.parseDate(map.get("date").toString()));
        }
        if (map.containsKey("notes") && map.get("notes") != null) {
            expense.setNotes(map.get("notes").toString());
        }

        return expense;
    }

    /**
     * Helper to deserialize JSON object into Budget entity.
     */
    public static Budget parseBudget(String json) {
        Map<String, Object> map = parseObject(json);
        Budget budget = new Budget();

        if (map.containsKey("id") && map.get("id") instanceof Number) {
            budget.setId(((Number) map.get("id")).intValue());
        }
        if (map.containsKey("category") && map.get("category") != null) {
            budget.setCategory(Category.fromString(map.get("category").toString()));
        }
        if (map.containsKey("monthlyLimit")) {
            Object limitObj = map.get("monthlyLimit");
            if (limitObj instanceof Number) {
                budget.setMonthlyLimit(((Number) limitObj).doubleValue());
            } else if (limitObj != null) {
                try {
                    budget.setMonthlyLimit(Double.parseDouble(limitObj.toString().trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (map.containsKey("monthYear") && map.get("monthYear") != null) {
            budget.setMonthYear(map.get("monthYear").toString());
        } else {
            budget.setMonthYear(DateTimeUtil.getCurrentMonthYear());
        }

        return budget;
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

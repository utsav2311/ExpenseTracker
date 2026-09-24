package com.expensetracker.web;

import com.expensetracker.dsa.ExpenseSorter;
import com.expensetracker.exception.ExpenseNotFoundException;
import com.expensetracker.exception.InvalidExpenseException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseSummary;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.util.DateTimeUtil;
import com.expensetracker.util.SimpleJson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Built-in Core Java HTTP Server and RESTful API controller.
 * Demonstrates:
 *  - com.sun.net.httpserver.HttpServer (Zero external framework requirement)
 *  - REST API conventions: HTTP Methods (GET, POST, PUT, DELETE), Resource URIs, Status Codes
 *  - Query parameter extraction and body parsing
 *  - Static web file serving (HTML, CSS, JS) with appropriate MIME types
 *  - CORS support and centralized JSON error responses
 */
public class ExpenseHttpServer {

    private int port;
    private final ExpenseService expenseService;
    private HttpServer server;

    public ExpenseHttpServer(int port, ExpenseService expenseService) {
        this.port = port;
        this.expenseService = expenseService;
    }

    public int getPort() {
        return port;
    }

    public void start() throws IOException {
        IOException lastEx = null;
        boolean bound = false;
        int maxAttempts = 5;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                bound = true;
                break;
            } catch (java.net.BindException be) {
                System.out.println("[Notice] Port " + port + " is already in use. Trying port " + (port + 1) + "...");
                port++;
                lastEx = be;
            }
        }

        if (!bound) {
            throw (lastEx != null) ? lastEx : new IOException("Could not bind HTTP server to any available port.");
        }

        server.setExecutor(Executors.newFixedThreadPool(12));

        // REST API Contexts
        server.createContext("/api/expenses", new ExpensesHandler());
        server.createContext("/api/summary", new SummaryHandler());
        server.createContext("/api/budgets", new BudgetsHandler());
        server.createContext("/api/export", new ExportHandler());
        server.createContext("/api/demo", new DemoHandler());
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/meta", new MetaHandler());

        // Static Web UI Assets Context
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("==========================================================");
        System.out.println("  Personal Expense Tracker HTTP Server is RUNNING!        ");
        System.out.println("  Access URL:  http://localhost:" + port + "/");
        System.out.println("  Storage:     " + expenseService.getStorageType());
        System.out.println("==========================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // =========================================================================
    //  REST HANDLER: /api/expenses and /api/expenses/{id}
    // =========================================================================

    private class ExpensesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            // Path could be "/api/expenses" or "/api/expenses/123"
            String subPath = path.substring("/api/expenses".length());
            if (subPath.startsWith("/")) {
                subPath = subPath.substring(1);
            }

            try {
                if (subPath.isEmpty()) {
                    // Route: /api/expenses
                    if ("GET".equals(method)) {
                        handleListExpenses(exchange);
                    } else if ("POST".equals(method)) {
                        handleCreateExpense(exchange);
                    } else {
                        sendError(exchange, 405, "Method Not Allowed: " + method);
                    }
                } else {
                    // Route: /api/expenses/{id}
                    int id;
                    try {
                        id = Integer.parseInt(subPath);
                    } catch (NumberFormatException e) {
                        sendError(exchange, 400, "Invalid expense ID: " + subPath);
                        return;
                    }

                    if ("GET".equals(method)) {
                        handleGetExpense(exchange, id);
                    } else if ("PUT".equals(method)) {
                        handleUpdateExpense(exchange, id);
                    } else if ("DELETE".equals(method)) {
                        handleDeleteExpense(exchange, id);
                    } else {
                        sendError(exchange, 405, "Method Not Allowed: " + method);
                    }
                }
            } catch (ExpenseNotFoundException e) {
                sendError(exchange, 404, e.getMessage());
            } catch (InvalidExpenseException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }

        private void handleListExpenses(HttpExchange exchange) throws Exception {
            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());

            String search = queryParams.get("search");
            String catStr = queryParams.get("category");
            String pmStr = queryParams.get("paymentMethod");
            String startStr = queryParams.get("startDate");
            String endStr = queryParams.get("endDate");
            String minStr = queryParams.get("minAmount");
            String maxStr = queryParams.get("maxAmount");
            String sortBy = queryParams.getOrDefault("sortBy", "date");
            String sortOrder = queryParams.getOrDefault("sortOrder", "desc");
            String algoStr = queryParams.getOrDefault("algo", "TIM_SORT");

            Category category = (catStr != null && !catStr.isEmpty() && !"ALL".equalsIgnoreCase(catStr))
                    ? Category.fromString(catStr) : null;
            PaymentMethod pm = (pmStr != null && !pmStr.isEmpty() && !"ALL".equalsIgnoreCase(pmStr))
                    ? PaymentMethod.fromString(pmStr) : null;
            LocalDate start = (startStr != null && !startStr.isEmpty()) ? DateTimeUtil.parseDate(startStr) : null;
            LocalDate end = (endStr != null && !endStr.isEmpty()) ? DateTimeUtil.parseDate(endStr) : null;
            Double min = (minStr != null && !minStr.isEmpty()) ? Double.parseDouble(minStr) : null;
            Double max = (maxStr != null && !maxStr.isEmpty()) ? Double.parseDouble(maxStr) : null;

            ExpenseSorter.Algorithm algo;
            try {
                algo = ExpenseSorter.Algorithm.valueOf(algoStr.toUpperCase());
            } catch (Exception e) {
                algo = ExpenseSorter.Algorithm.TIM_SORT;
            }

            List<Expense> expenses = expenseService.getFilteredAndSortedExpenses(
                    search, category, pm, start, end, min, max, sortBy, sortOrder, algo
            );

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("count", expenses.size());
            resp.put("algorithmUsed", algo.name());
            resp.put("expenses", expenses);

            sendJson(exchange, 200, SimpleJson.toJson(resp));
        }

        private void handleGetExpense(HttpExchange exchange, int id) throws Exception {
            Expense expense = expenseService.getExpenseById(id);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("expense", expense);
            sendJson(exchange, 200, SimpleJson.toJson(resp));
        }

        private void handleCreateExpense(HttpExchange exchange) throws Exception {
            String body = readRequestBody(exchange);
            Expense expense = SimpleJson.parseExpense(body);
            Expense created = expenseService.createExpense(expense);

            Budget alertBudget = expenseService.checkBudgetAlert(created);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", "Expense created successfully!");
            resp.put("expense", created);
            if (alertBudget != null) {
                resp.put("budgetExceeded", true);
                resp.put("budgetWarning", String.format("Warning: Budget for %s is exceeded by $%.2f!",
                        alertBudget.getCategory().getDisplayName(), alertBudget.getSpent() - alertBudget.getMonthlyLimit()));
            } else {
                resp.put("budgetExceeded", false);
            }

            sendJson(exchange, 201, SimpleJson.toJson(resp));
        }

        private void handleUpdateExpense(HttpExchange exchange, int id) throws Exception {
            String body = readRequestBody(exchange);
            Expense updatedData = SimpleJson.parseExpense(body);
            Expense updated = expenseService.updateExpense(id, updatedData);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", "Expense #" + id + " updated successfully!");
            resp.put("expense", updated);

            sendJson(exchange, 200, SimpleJson.toJson(resp));
        }

        private void handleDeleteExpense(HttpExchange exchange, int id) throws Exception {
            expenseService.deleteExpense(id);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", "Expense #" + id + " deleted successfully!");
            sendJson(exchange, 200, SimpleJson.toJson(resp));
        }
    }

    // =========================================================================
    //  REST HANDLER: /api/summary
    // =========================================================================

    private class SummaryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                ExpenseSummary summary = expenseService.getSummary();
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("summary", summary);
                sendJson(exchange, 200, SimpleJson.toJson(resp));
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    // =========================================================================
    //  REST HANDLER: /api/budgets
    // =========================================================================

    private class BudgetsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String subPath = path.substring("/api/budgets".length());
            if (subPath.startsWith("/")) {
                subPath = subPath.substring(1);
            }

            try {
                if (subPath.isEmpty()) {
                    if ("GET".equals(method)) {
                        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());
                        String monthYear = params.getOrDefault("monthYear", DateTimeUtil.getCurrentMonthYear());
                        List<Budget> budgets = expenseService.getBudgetsWithSpending(monthYear);

                        Map<String, Object> resp = new LinkedHashMap<>();
                        resp.put("success", true);
                        resp.put("monthYear", monthYear);
                        resp.put("budgets", budgets);
                        sendJson(exchange, 200, SimpleJson.toJson(resp));
                    } else if ("POST".equals(method)) {
                        String body = readRequestBody(exchange);
                        Budget budget = SimpleJson.parseBudget(body);
                        expenseService.saveBudget(budget);

                        Map<String, Object> resp = new LinkedHashMap<>();
                        resp.put("success", true);
                        resp.put("message", "Budget saved successfully!");
                        sendJson(exchange, 200, SimpleJson.toJson(resp));
                    } else {
                        sendError(exchange, 405, "Method Not Allowed");
                    }
                } else {
                    int id = Integer.parseInt(subPath);
                    if ("DELETE".equals(method)) {
                        expenseService.deleteBudget(id);
                        Map<String, Object> resp = new LinkedHashMap<>();
                        resp.put("success", true);
                        resp.put("message", "Budget deleted successfully!");
                        sendJson(exchange, 200, SimpleJson.toJson(resp));
                    } else {
                        sendError(exchange, 405, "Method Not Allowed");
                    }
                }
            } catch (InvalidExpenseException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    // =========================================================================
    //  REST HANDLER: /api/export
    // =========================================================================

    private class ExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                String csv = expenseService.generateCsvExport();
                byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"expenses.csv\"");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    // =========================================================================
    //  REST HANDLER: /api/demo
    // =========================================================================

    private class DemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                expenseService.reloadDemoData();
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("message", "Sample expense records loaded successfully!");
                sendJson(exchange, 200, SimpleJson.toJson(resp));
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    // =========================================================================
    //  REST HANDLER: /api/status
    // =========================================================================

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                int count = expenseService.getAllExpenses().size();
                Map<String, Object> status = new LinkedHashMap<>();
                status.put("status", "UP");
                status.put("storage", expenseService.getStorageType());
                status.put("isDatabase", expenseService.isUsingDatabase());
                status.put("totalExpenses", count);
                status.put("javaVersion", System.getProperty("java.version"));
                status.put("timestamp", DateTimeUtil.formatTimestamp(java.time.LocalDateTime.now()));

                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("data", status);
                sendJson(exchange, 200, SimpleJson.toJson(resp));
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    // =========================================================================
    //  REST HANDLER: /api/meta (Categories & Payment Methods)
    // =========================================================================

    private class MetaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            List<Map<String, String>> catList = new ArrayList<>();
            for (Category c : Category.values()) {
                Map<String, String> m = new HashMap<>();
                m.put("code", c.name());
                m.put("name", c.getDisplayName());
                m.put("icon", c.getIcon());
                catList.add(m);
            }

            List<Map<String, String>> pmList = new ArrayList<>();
            for (PaymentMethod pm : PaymentMethod.values()) {
                Map<String, String> m = new HashMap<>();
                m.put("code", pm.name());
                m.put("name", pm.getDisplayName());
                m.put("icon", pm.getIcon());
                pmList.add(m);
            }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("categories", catList);
            resp.put("paymentMethods", pmList);

            sendJson(exchange, 200, SimpleJson.toJson(resp));
        }
    }

    // =========================================================================
    //  STATIC FILE HANDLER (web/index.html, web/style.css, web/app.js)
    // =========================================================================

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.isEmpty() || "/".equals(path)) {
                path = "/index.html";
            }

            // Security check against directory traversal
            if (path.contains("..")) {
                sendError(exchange, 403, "Access Denied");
                return;
            }

            File file = new File("web" + path);
            if (!file.exists() || file.isDirectory()) {
                sendError(exchange, 404, "File Not Found: " + path);
                return;
            }

            String mime = getMimeType(path);
            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, file.length());

            try (InputStream is = new FileInputStream(file);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }

        private String getMimeType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css; charset=UTF-8";
            if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (path.endsWith(".json")) return "application/json; charset=UTF-8";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }

    // =========================================================================
    //  HTTP HELPER METHODS
    // =========================================================================

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("success", false);
        err.put("statusCode", statusCode);
        err.put("error", message);
        err.put("timestamp", DateTimeUtil.formatTimestamp(java.time.LocalDateTime.now()));
        sendJson(exchange, statusCode, SimpleJson.toJson(err));
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return params;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    params.put(key, val);
                } else if (!pair.isEmpty()) {
                    params.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            } catch (Exception ignored) {
            }
        }
        return params;
    }
}

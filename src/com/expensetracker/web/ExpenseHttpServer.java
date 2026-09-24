package com.expensetracker.web;

import com.expensetracker.exception.DatabaseOperationException;
import com.expensetracker.exception.InvalidAmountException;
import com.expensetracker.exception.InvalidTransactionException;
import com.expensetracker.exception.TransactionNotFoundException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategorySummary;
import com.expensetracker.model.DashboardSummary;
import com.expensetracker.model.MonthlySummary;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.service.TransactionService;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Built-in Core Java HTTP Server exposing clean RESTful JSON endpoints.
 * Demonstrates:
 *  - com.sun.net.httpserver.HttpServer (Zero external web frameworks)
 *  - REST API conventions: HTTP Methods (GET, POST, PUT, DELETE), Resource URIs, Status Codes
 *  - Same-origin static file serving (HTML, CSS, JS) with appropriate MIME headers
 *  - Centralized JSON error response mapping without leaking server stack traces
 */
public class ExpenseHttpServer {

    private int port;
    private final TransactionService transactionService;
    private HttpServer server;

    public ExpenseHttpServer(int port, TransactionService transactionService) {
        this.port = port;
        this.transactionService = transactionService;
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

        // REST API Handlers
        server.createContext("/api/transactions", new TransactionsHandler());
        server.createContext("/api/dashboard", new DashboardHandler());
        server.createContext("/api/categories", new CategoriesHandler());
        server.createContext("/api/reports", new ReportsHandler());
        server.createContext("/api/budget", new BudgetHandler());
        server.createContext("/api/export", new ExportHandler());
        server.createContext("/api/status", new StatusHandler());

        // Static Web UI Assets Handler
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("==========================================================");
        System.out.println("  Personal Income & Expense Tracker Server is RUNNING!    ");
        System.out.println("  Access URL:  http://localhost:" + port + "/");
        System.out.println("  Storage:     " + transactionService.getStorageType());
        System.out.println("==========================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // =========================================================================
    //  REST: /api/transactions and /api/transactions/{id}
    // =========================================================================

    private class TransactionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String subPath = path.substring("/api/transactions".length());
            if (subPath.startsWith("/")) {
                subPath = subPath.substring(1);
            }

            try {
                if (subPath.isEmpty()) {
                    // /api/transactions
                    if ("GET".equals(method)) {
                        handleListTransactions(exchange);
                    } else if ("POST".equals(method)) {
                        handleCreateTransaction(exchange);
                    } else {
                        sendError(exchange, 405, "Method Not Allowed: " + method);
                    }
                } else {
                    // /api/transactions/{id}
                    int id;
                    try {
                        id = Integer.parseInt(subPath);
                    } catch (NumberFormatException e) {
                        sendError(exchange, 400, "Invalid transaction ID format: " + subPath);
                        return;
                    }

                    if ("GET".equals(method)) {
                        handleGetTransaction(exchange, id);
                    } else if ("PUT".equals(method)) {
                        handleUpdateTransaction(exchange, id);
                    } else if ("DELETE".equals(method)) {
                        handleDeleteTransaction(exchange, id);
                    } else {
                        sendError(exchange, 405, "Method Not Allowed: " + method);
                    }
                }
            } catch (TransactionNotFoundException e) {
                sendError(exchange, 404, e.getMessage());
            } catch (InvalidAmountException | InvalidTransactionException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (DatabaseOperationException e) {
                sendError(exchange, 500, e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, "An unexpected error occurred: " + e.getMessage());
            }
        }

        private void handleListTransactions(HttpExchange exchange) throws Exception {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());

            String typeStr = params.get("type");
            String catStr = params.get("category");
            String startStr = params.get("startDate");
            String endStr = params.get("endDate");
            String search = params.get("search");
            String sort = params.get("sort");

            TransactionType type = (typeStr != null && !typeStr.isEmpty() && !"ALL".equalsIgnoreCase(typeStr))
                    ? TransactionType.fromString(typeStr) : null;

            Integer categoryId = null;
            if (catStr != null && !catStr.isEmpty() && !"ALL".equalsIgnoreCase(catStr)) {
                try {
                    categoryId = Integer.parseInt(catStr);
                } catch (NumberFormatException e) {
                    // Might be category name
                    for (Category c : transactionService.getAllCategories()) {
                        if (c.getCategoryName().equalsIgnoreCase(catStr.trim())) {
                            categoryId = c.getCategoryId();
                            break;
                        }
                    }
                }
            }

            LocalDate start = (startStr != null && !startStr.isEmpty()) ? DateTimeUtil.parseDate(startStr) : null;
            LocalDate end = (endStr != null && !endStr.isEmpty()) ? DateTimeUtil.parseDate(endStr) : null;

            List<Transaction> transactions = transactionService.getFilteredTransactions(
                    type, categoryId, start, end, search, sort
            );

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("count", transactions.size());
            resp.put("transactions", transactions);

            sendJson(exchange, 200, SimpleJson.toJson(resp));
        }

        private void handleGetTransaction(HttpExchange exchange, int id) throws Exception {
            Transaction tx = transactionService.getTransactionById(id);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("transaction", tx);
            sendJson(exchange, 200, SimpleJson.toJson(resp));
        }

        private void handleCreateTransaction(HttpExchange exchange) throws Exception {
            String body = readRequestBody(exchange);
            Transaction tx = SimpleJson.parseTransaction(body);
            Transaction created = transactionService.addTransaction(tx);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", "Transaction created successfully.");
            resp.put("transaction", created);

            sendJson(exchange, 201, SimpleJson.toJson(resp));
        }

        private void handleUpdateTransaction(HttpExchange exchange, int id) throws Exception {
            String body = readRequestBody(exchange);
            Transaction tx = SimpleJson.parseTransaction(body);
            Transaction updated = transactionService.updateTransaction(id, tx);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", "Transaction #" + id + " updated successfully.");
            resp.put("transaction", updated);

            sendJson(exchange, 200, SimpleJson.toJson(resp));
        }

        private void handleDeleteTransaction(HttpExchange exchange, int id) throws Exception {
            transactionService.deleteTransaction(id);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", "Transaction #" + id + " deleted successfully.");
            sendJson(exchange, 200, SimpleJson.toJson(resp));
        }
    }

    // =========================================================================
    //  REST: /api/dashboard
    // =========================================================================

    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                LocalDate now = LocalDate.now();
                DashboardSummary ds = transactionService.getDashboardSummary(now.getMonthValue(), now.getYear());
                sendJson(exchange, 200, SimpleJson.toJson(ds));
            } catch (DatabaseOperationException e) {
                sendError(exchange, 500, e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, "Failed to load dashboard: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  REST: /api/categories
    // =========================================================================

    private class CategoriesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());
                String typeStr = params.get("type");
                TransactionType type = (typeStr != null && !typeStr.isEmpty() && !"ALL".equalsIgnoreCase(typeStr))
                        ? TransactionType.fromString(typeStr) : null;

                List<Category> categories = transactionService.getCategoriesByType(type);

                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("count", categories.size());
                resp.put("categories", categories);

                sendJson(exchange, 200, SimpleJson.toJson(resp));
            } catch (DatabaseOperationException e) {
                sendError(exchange, 500, e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, "Failed to load categories: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  REST: /api/reports (monthly, categories, top-expenses)
    // =========================================================================

    private class ReportsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());

            LocalDate now = LocalDate.now();
            int month = now.getMonthValue();
            int year = now.getYear();

            if (params.containsKey("month")) {
                try { month = Integer.parseInt(params.get("month")); } catch (NumberFormatException ignored) {}
            }
            if (params.containsKey("year")) {
                try { year = Integer.parseInt(params.get("year")); } catch (NumberFormatException ignored) {}
            }

            try {
                if (path.endsWith("/monthly")) {
                    MonthlySummary ms = transactionService.getMonthlySummary(month, year);
                    sendJson(exchange, 200, SimpleJson.toJson(ms));
                } else if (path.endsWith("/categories")) {
                    List<CategorySummary> catReports = transactionService.getCategoryExpenseReport(month, year);
                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("success", true);
                    resp.put("month", month);
                    resp.put("year", year);
                    resp.put("categories", catReports);
                    sendJson(exchange, 200, SimpleJson.toJson(resp));
                } else if (path.endsWith("/top-expenses")) {
                    int limit = 5;
                    if (params.containsKey("limit")) {
                        try { limit = Integer.parseInt(params.get("limit")); } catch (NumberFormatException ignored) {}
                    }
                    List<Transaction> top = transactionService.getTopExpenses(limit);
                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("success", true);
                    resp.put("topExpenses", top);
                    sendJson(exchange, 200, SimpleJson.toJson(resp));
                } else {
                    sendError(exchange, 404, "Report endpoint not found.");
                }
            } catch (DatabaseOperationException e) {
                sendError(exchange, 500, e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, "Report error: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  REST: /api/budget
    // =========================================================================

    private class BudgetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            LocalDate now = LocalDate.now();
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());
            int month = now.getMonthValue();
            int year = now.getYear();
            if (params.containsKey("month")) {
                try { month = Integer.parseInt(params.get("month")); } catch (NumberFormatException ignored) {}
            }
            if (params.containsKey("year")) {
                try { year = Integer.parseInt(params.get("year")); } catch (NumberFormatException ignored) {}
            }

            try {
                if ("GET".equals(method)) {
                    Budget b = transactionService.getBudget(month, year);
                    sendJson(exchange, 200, SimpleJson.toJson(b));
                } else if ("POST".equals(method) || "PUT".equals(method)) {
                    String body = readRequestBody(exchange);
                    Budget b = SimpleJson.parseBudget(body);
                    transactionService.saveBudget(b);

                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("success", true);
                    resp.put("message", "Monthly budget updated successfully.");
                    resp.put("budget", b);
                    sendJson(exchange, 200, SimpleJson.toJson(resp));
                } else {
                    sendError(exchange, 405, "Method Not Allowed");
                }
            } catch (InvalidAmountException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (DatabaseOperationException e) {
                sendError(exchange, 500, e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, "Budget error: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  REST: /api/export
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
                String csv = transactionService.generateCsvExport();
                byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"transactions.csv\"");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                sendError(exchange, 500, "Export error: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  REST: /api/status
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
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("status", "UP");
                data.put("storage", transactionService.getStorageType());
                data.put("databaseAvailable", transactionService.isDatabaseAvailable());
                data.put("currency", "INR (₹)");
                data.put("serverTime", DateTimeUtil.formatTimestamp(java.time.LocalDateTime.now()));

                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("data", data);
                sendJson(exchange, 200, SimpleJson.toJson(resp));
            } catch (Exception e) {
                sendError(exchange, 500, "Status error: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  STATIC FILE SERVING (Same Origin: HTML, CSS, JS)
    // =========================================================================

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.isEmpty() || "/".equals(path)) {
                path = "/index.html";
            }

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

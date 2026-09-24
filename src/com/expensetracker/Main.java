package com.expensetracker;

import com.expensetracker.exception.ExpenseTrackerException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategorySummary;
import com.expensetracker.model.DashboardSummary;
import com.expensetracker.model.MonthlySummary;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.service.TransactionService;
import com.expensetracker.test.ExpenseTrackerTest;
import com.expensetracker.util.DateTimeUtil;
import com.expensetracker.web.ExpenseHttpServer;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Main application entry point for Personal Income & Expense Tracker.
 * Supports:
 *  1. Web Server Mode (Default)
 *  2. Interactive Console CLI Mode
 *  3. Automated Test Suite Mode
 */
public class Main {

    public static void main(String[] args) {
        String mode = (args.length > 0) ? args[0].trim().toLowerCase() : "web";

        if ("test".equals(mode)) {
            ExpenseTrackerTest.runAllTests();
            return;
        }

        TransactionService transactionService = new TransactionService();

        if ("console".equals(mode) || "cli".equals(mode)) {
            runConsoleApp(transactionService);
        } else {
            int port = 8080;
            if (args.length > 1) {
                try {
                    port = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {
                }
            } else if (args.length == 1 && !mode.equals("web")) {
                try {
                    port = Integer.parseInt(mode);
                } catch (NumberFormatException ignored) {
                }
            }

            try {
                ExpenseHttpServer server = new ExpenseHttpServer(port, transactionService);
                server.start();
            } catch (Exception e) {
                System.err.println("Failed to start HTTP server: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  INTERACTIVE CONSOLE INTERFACE
    // =========================================================================

    private static void runConsoleApp(TransactionService service) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("==========================================================");
        System.out.println("   Personal Income & Expense Tracker (Core Java + JDBC)   ");
        System.out.println("   Storage: " + service.getStorageType());
        System.out.println("==========================================================");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Select an option (1-9): ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    viewDashboard(service);
                    break;
                case "2":
                    viewAllTransactions(service);
                    break;
                case "3":
                    addNewTransaction(service, scanner);
                    break;
                case "4":
                    editTransaction(service, scanner);
                    break;
                case "5":
                    deleteTransaction(service, scanner);
                    break;
                case "6":
                    searchAndFilterTransactions(service, scanner);
                    break;
                case "7":
                    viewMonthlyReport(service);
                    break;
                case "8":
                    setMonthlyBudget(service, scanner);
                    break;
                case "9":
                    System.out.println("\nThank you for using Personal Income & Expense Tracker. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("[!] Invalid option. Please select 1 to 9.");
            }
            if (running) {
                System.out.println("\nPress Enter to return to main menu...");
                scanner.nextLine();
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n----------------------- MAIN MENU -----------------------");
        System.out.println(" 1. View Financial Dashboard");
        System.out.println(" 2. View All Transactions");
        System.out.println(" 3. Add New Transaction (Income / Expense)");
        System.out.println(" 4. Edit Existing Transaction");
        System.out.println(" 5. Delete Transaction");
        System.out.println(" 6. Search, Filter & Sort Transactions");
        System.out.println(" 7. Monthly Summary & Category Reports");
        System.out.println(" 8. Set Monthly Budget");
        System.out.println(" 9. Exit");
        System.out.println("---------------------------------------------------------");
    }

    private static void viewDashboard(TransactionService service) {
        try {
            LocalDate now = LocalDate.now();
            DashboardSummary ds = service.getDashboardSummary(now.getMonthValue(), now.getYear());
            System.out.println("\n================ FINANCIAL DASHBOARD ================");
            System.out.printf(" Current Balance:   ₹%,.2f\n", ds.getBalance());
            System.out.printf(" Total Income:      ₹%,.2f\n", ds.getTotalIncome());
            System.out.printf(" Total Expenses:    ₹%,.2f\n", ds.getTotalExpense());
            System.out.printf(" Monthly Budget:    ₹%,.2f (Used: %.1f%% | Remaining: ₹%,.2f)\n",
                    ds.getMonthlyBudget(), ds.getBudgetPercentage(), ds.getBudgetRemaining());
            System.out.printf(" Total Transactions: %d\n", ds.getTransactionCount());
            System.out.println("-----------------------------------------------------");

            System.out.println("\n--- Recent Transactions ---");
            printTransactionTable(ds.getRecentTransactions());
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void viewAllTransactions(TransactionService service) {
        try {
            List<Transaction> transactions = service.getAllTransactions();
            printTransactionTable(transactions);
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void addNewTransaction(TransactionService service, Scanner scanner) {
        System.out.println("\n--- Add New Transaction ---");
        try {
            System.out.print("Transaction Type (1=Income, 2=Expense) [2]: ");
            String typeChoice = scanner.nextLine().trim();
            TransactionType type = "1".equals(typeChoice) ? TransactionType.INCOME : TransactionType.EXPENSE;

            System.out.print("Enter Description (e.g. Monthly Tech Salary / Grocery run): ");
            String description = scanner.nextLine().trim();

            System.out.print("Enter Amount (₹): ");
            double amount = Double.parseDouble(scanner.nextLine().trim());

            // Load categories matching selected type
            List<Category> categories = service.getCategoriesByType(type);
            System.out.printf("Select %s Category:\n", type.getDisplayName());
            for (int i = 0; i < categories.size(); i++) {
                Category c = categories.get(i);
                System.out.printf("  %d. %s %s\n", i + 1, c.getIcon(), c.getCategoryName());
            }
            System.out.print("Choice (1-" + categories.size() + "): ");
            int catIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            int categoryId = (catIdx >= 0 && catIdx < categories.size()) ? categories.get(catIdx).getCategoryId() : categories.get(0).getCategoryId();

            System.out.print("Payment Method (Cash / UPI / Credit Card / Debit Card / Net Banking) [Cash]: ");
            String pm = scanner.nextLine().trim();
            if (pm.isEmpty()) pm = "Cash";

            System.out.print("Date (YYYY-MM-DD) or press Enter for Today: ");
            String dateStr = scanner.nextLine().trim();
            LocalDate date = dateStr.isEmpty() ? LocalDate.now() : DateTimeUtil.parseDate(dateStr);

            System.out.print("Notes (optional): ");
            String notes = scanner.nextLine().trim();

            Transaction tx = new Transaction(1, categoryId, amount, type, description, notes, pm, date);
            Transaction created = service.addTransaction(tx);
            System.out.printf("[✓] Transaction #%d added successfully!\n", created.getTransactionId());
        } catch (ExpenseTrackerException e) {
            System.out.println("[Validation Error] " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid numeric input.");
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void editTransaction(TransactionService service, Scanner scanner) {
        System.out.println("\n--- Edit Transaction ---");
        try {
            System.out.print("Enter Transaction ID to edit: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            Transaction tx = service.getTransactionById(id);

            System.out.printf("Description [%s]: ", tx.getDescription());
            String desc = scanner.nextLine().trim();
            if (!desc.isEmpty()) tx.setDescription(desc);

            System.out.printf("Amount [₹%.2f]: ", tx.getAmount());
            String amtStr = scanner.nextLine().trim();
            if (!amtStr.isEmpty()) tx.setAmount(Double.parseDouble(amtStr));

            System.out.printf("Payment Method [%s]: ", tx.getPaymentMethod());
            String pm = scanner.nextLine().trim();
            if (!pm.isEmpty()) tx.setPaymentMethod(pm);

            System.out.printf("Date [%s]: ", tx.getTransactionDate());
            String dateStr = scanner.nextLine().trim();
            if (!dateStr.isEmpty()) tx.setTransactionDate(DateTimeUtil.parseDate(dateStr));

            System.out.printf("Notes [%s]: ", tx.getNotes());
            String notes = scanner.nextLine().trim();
            if (!notes.isEmpty()) tx.setNotes(notes);

            service.updateTransaction(id, tx);
            System.out.println("[✓] Transaction updated successfully!");
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void deleteTransaction(TransactionService service, Scanner scanner) {
        System.out.println("\n--- Delete Transaction ---");
        try {
            System.out.print("Enter Transaction ID to delete: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            Transaction tx = service.getTransactionById(id);
            System.out.printf("Are you sure you want to delete '%s' (₹%.2f)? (y/N): ", tx.getDescription(), tx.getAmount());
            if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                service.deleteTransaction(id);
                System.out.println("[✓] Transaction deleted successfully!");
            } else {
                System.out.println("Operation cancelled.");
            }
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void searchAndFilterTransactions(TransactionService service, Scanner scanner) {
        System.out.println("\n--- Search & Filter Transactions ---");
        try {
            System.out.print("Filter by Type (1=All, 2=Income, 3=Expense) [1]: ");
            String tChoice = scanner.nextLine().trim();
            TransactionType type = null;
            if ("2".equals(tChoice)) type = TransactionType.INCOME;
            if ("3".equals(tChoice)) type = TransactionType.EXPENSE;

            System.out.print("Search Keyword (Description, Category, Notes) or Enter to skip: ");
            String query = scanner.nextLine().trim();

            System.out.print("Sort By (newest / oldest / highest / lowest) [newest]: ");
            String sort = scanner.nextLine().trim();

            List<Transaction> results = service.getFilteredTransactions(type, null, null, null, query, sort);
            printTransactionTable(results);
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void viewMonthlyReport(TransactionService service) {
        try {
            LocalDate now = LocalDate.now();
            MonthlySummary ms = service.getMonthlySummary(now.getMonthValue(), now.getYear());
            System.out.printf("\n================ MONTHLY SUMMARY (%s) ================\n", ms.getMonthName());
            System.out.printf(" Income:           ₹%,.2f\n", ms.getIncome());
            System.out.printf(" Expenses:         ₹%,.2f\n", ms.getExpenses());
            System.out.printf(" Savings:          ₹%,.2f\n", ms.getSavings());
            System.out.printf(" Transactions:     %d\n", ms.getTransactionCount());
            if (ms.getHighestExpense() != null) {
                System.out.printf(" Highest Expense:  ₹%,.2f (%s)\n",
                        ms.getHighestExpense().getAmount(), ms.getHighestExpense().getDescription());
            }
            if (ms.getHighestSpendingCategory() != null) {
                System.out.printf(" Top Category:     %s (₹%,.2f)\n",
                        ms.getHighestSpendingCategory(), ms.getHighestSpendingCategoryAmount());
            }
            System.out.printf(" Monthly Budget:   ₹%,.2f (Remaining: ₹%,.2f | Used: %.1f%%)\n",
                    ms.getBudgetAmount(), ms.getRemainingBudget(), ms.getBudgetPercentageUsed());

            System.out.println("\n--- Category Expense Breakdown (SQL Aggregations) ---");
            List<CategorySummary> cats = service.getCategoryExpenseReport(now.getMonthValue(), now.getYear());
            for (CategorySummary cs : cats) {
                System.out.printf("  %-20s : ₹%,8.2f (%5.1f%% across %d tx)\n",
                        cs.getCategoryName(), cs.getTotalAmount(), cs.getPercentage(), cs.getTransactionCount());
            }
            System.out.println("==========================================================");
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void setMonthlyBudget(TransactionService service, Scanner scanner) {
        System.out.println("\n--- Set Monthly Budget ---");
        try {
            LocalDate now = LocalDate.now();
            System.out.print("Enter Monthly Budget Amount (₹): ");
            double amount = Double.parseDouble(scanner.nextLine().trim());

            Budget b = new Budget(1, now.getMonthValue(), now.getYear(), amount);
            service.saveBudget(b);
            System.out.printf("[✓] Monthly budget for %s set to ₹%,.2f\n", now.getMonth(), amount);
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void printTransactionTable(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            System.out.println("(No transactions found)");
            return;
        }

        System.out.printf("%-4s | %-12s | %-24s | %-7s | %-16s | %-14s | %s\n",
                "ID", "Date", "Description", "Type", "Category", "Payment Method", "Amount (₹)");
        System.out.println("---------------------------------------------------------------------------------------------------------");

        for (Transaction t : transactions) {
            String desc = (t.getDescription().length() > 23) ? t.getDescription().substring(0, 20) + "..." : t.getDescription();
            String typeBadge = t.isIncome() ? "INCOME" : "EXPENSE";
            String sign = t.isIncome() ? "+" : "-";
            System.out.printf("#%-3d | %-12s | %-24s | %-7s | %-16s | %-14s | %s ₹%,.2f\n",
                    t.getTransactionId(),
                    t.getTransactionDate(),
                    desc,
                    typeBadge,
                    t.getCategoryName() != null ? t.getCategoryName() : ("Cat#" + t.getCategoryId()),
                    t.getPaymentMethod(),
                    sign,
                    t.getAmount());
        }
        System.out.println("---------------------------------------------------------------------------------------------------------");
        System.out.printf("Total Records: %d\n", transactions.size());
    }
}

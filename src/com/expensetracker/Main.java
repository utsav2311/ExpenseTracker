package com.expensetracker;

import com.expensetracker.dsa.ExpenseSorter;
import com.expensetracker.exception.ExpenseNotFoundException;
import com.expensetracker.exception.InvalidExpenseException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseSummary;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.test.ExpenseTrackerTest;
import com.expensetracker.util.DateTimeUtil;
import com.expensetracker.web.ExpenseHttpServer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Main application entry point.
 * Supports:
 *  1. Web Server Mode (Default): Launches built-in HTTP server with REST API & Web UI
 *  2. Interactive Console CLI Mode: Menu-driven terminal application
 *  3. Automated Test Suite Mode: Executes tests and reports validation assertions
 */
public class Main {

    public static void main(String[] args) {
        String mode = (args.length > 0) ? args[0].trim().toLowerCase() : "web";

        if ("test".equals(mode)) {
            ExpenseTrackerTest.runAllTests();
            return;
        }

        ExpenseService expenseService = new ExpenseService();

        if ("console".equals(mode) || "cli".equals(mode)) {
            runConsoleApp(expenseService);
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
                ExpenseHttpServer server = new ExpenseHttpServer(port, expenseService);
                server.start();
            } catch (Exception e) {
                System.err.println("Failed to start HTTP server: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // =========================================================================
    //  INTERACTIVE CONSOLE INTERFACE
    // =========================================================================

    private static void runConsoleApp(ExpenseService service) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("==========================================================");
        System.out.println("      Welcome to Personal Expense Tracker (Core Java)     ");
        System.out.println("      Persistence: " + service.getStorageType());
        System.out.println("==========================================================");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Select an option (1-9): ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    viewAllExpenses(service);
                    break;
                case "2":
                    addNewExpense(service, scanner);
                    break;
                case "3":
                    editExpense(service, scanner);
                    break;
                case "4":
                    deleteExpense(service, scanner);
                    break;
                case "5":
                    searchAndFilterExpenses(service, scanner);
                    break;
                case "6":
                    viewAnalyticsSummary(service);
                    break;
                case "7":
                    viewBudgets(service);
                    break;
                case "8":
                    setBudget(service, scanner);
                    break;
                case "9":
                    System.out.println("\nThank you for using Personal Expense Tracker. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("[!] Invalid option. Please enter a number between 1 and 9.");
            }
            if (running) {
                System.out.println("\nPress Enter to return to main menu...");
                scanner.nextLine();
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n--------------- MAIN MENU ---------------");
        System.out.println(" 1. View All Expenses");
        System.out.println(" 2. Add New Expense");
        System.out.println(" 3. Edit Existing Expense");
        System.out.println(" 4. Delete Expense");
        System.out.println(" 5. Search, Filter & Sort (DSA Demo)");
        System.out.println(" 6. View Financial Analytics & Summary");
        System.out.println(" 7. View Monthly Budgets & Spending");
        System.out.println(" 8. Set / Update Category Budget");
        System.out.println(" 9. Exit");
        System.out.println("-----------------------------------------");
    }

    private static void viewAllExpenses(ExpenseService service) {
        try {
            List<Expense> expenses = service.getAllExpenses();
            printExpenseTable(expenses);
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void addNewExpense(ExpenseService service, Scanner scanner) {
        System.out.println("\n--- Add New Expense ---");
        try {
            System.out.print("Enter Title (e.g. Grocery shopping): ");
            String title = scanner.nextLine();

            System.out.print("Enter Amount ($): ");
            double amount = Double.parseDouble(scanner.nextLine().trim());

            System.out.println("Available Categories: ");
            Category[] categories = Category.values();
            for (int i = 0; i < categories.length; i++) {
                System.out.printf("  %d. %s %s\n", i + 1, categories[i].getIcon(), categories[i].getDisplayName());
            }
            System.out.print("Select Category (1-" + categories.length + "): ");
            int catIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            Category category = (catIdx >= 0 && catIdx < categories.length) ? categories[catIdx] : Category.OTHER;

            System.out.println("Payment Methods: ");
            PaymentMethod[] methods = PaymentMethod.values();
            for (int i = 0; i < methods.length; i++) {
                System.out.printf("  %d. %s %s\n", i + 1, methods[i].getIcon(), methods[i].getDisplayName());
            }
            System.out.print("Select Payment Method (1-" + methods.length + "): ");
            int pmIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            PaymentMethod pm = (pmIdx >= 0 && pmIdx < methods.length) ? methods[pmIdx] : PaymentMethod.CASH;

            System.out.print("Enter Date (YYYY-MM-DD) or press Enter for Today: ");
            String dateStr = scanner.nextLine().trim();
            LocalDate date = dateStr.isEmpty() ? LocalDate.now() : DateTimeUtil.parseDate(dateStr);

            System.out.print("Enter Notes (optional): ");
            String notes = scanner.nextLine();

            Expense expense = new Expense(title, amount, category, pm, date, notes);
            Expense created = service.createExpense(expense);
            System.out.printf("[✓] Expense created successfully! Assigned ID: #%d\n", created.getId());

            Budget alert = service.checkBudgetAlert(created);
            if (alert != null) {
                System.out.printf("    [!] BUDGET WARNING: Monthly budget for %s has been exceeded by $%.2f!\n",
                        alert.getCategory().getDisplayName(), alert.getSpent() - alert.getMonthlyLimit());
            }
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid numeric input.");
        } catch (InvalidExpenseException e) {
            System.out.println("[Validation Error] " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void editExpense(ExpenseService service, Scanner scanner) {
        System.out.println("\n--- Edit Expense ---");
        try {
            System.out.print("Enter Expense ID to edit: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            Expense existing = service.getExpenseById(id);

            System.out.printf("Current Title [%s]: ", existing.getTitle());
            String title = scanner.nextLine().trim();
            if (!title.isEmpty()) existing.setTitle(title);

            System.out.printf("Current Amount [$%.2f]: ", existing.getAmount());
            String amtStr = scanner.nextLine().trim();
            if (!amtStr.isEmpty()) existing.setAmount(Double.parseDouble(amtStr));

            System.out.print("Update Category? (y/N): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                Category[] categories = Category.values();
                for (int i = 0; i < categories.length; i++) {
                    System.out.printf("  %d. %s %s\n", i + 1, categories[i].getIcon(), categories[i].getDisplayName());
                }
                System.out.print("Select Category: ");
                int catIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (catIdx >= 0 && catIdx < categories.length) existing.setCategory(categories[catIdx]);
            }

            System.out.printf("Current Date [%s]: ", existing.getDate());
            String dateStr = scanner.nextLine().trim();
            if (!dateStr.isEmpty()) existing.setDate(DateTimeUtil.parseDate(dateStr));

            System.out.printf("Current Notes [%s]: ", existing.getNotes());
            String notes = scanner.nextLine().trim();
            if (!notes.isEmpty()) existing.setNotes(notes);

            service.updateExpense(id, existing);
            System.out.println("[✓] Expense updated successfully!");
        } catch (ExpenseNotFoundException e) {
            System.out.println("[!] " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void deleteExpense(ExpenseService service, Scanner scanner) {
        System.out.println("\n--- Delete Expense ---");
        try {
            System.out.print("Enter Expense ID to delete: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            System.out.printf("Are you sure you want to delete Expense #%d? (y/N): ", id);
            String confirm = scanner.nextLine().trim();
            if (confirm.equalsIgnoreCase("y")) {
                service.deleteExpense(id);
                System.out.println("[✓] Expense deleted successfully!");
            } else {
                System.out.println("Operation cancelled.");
            }
        } catch (ExpenseNotFoundException e) {
            System.out.println("[!] " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void searchAndFilterExpenses(ExpenseService service, Scanner scanner) {
        System.out.println("\n--- Search, Filter & Sort (DSA Demo) ---");
        try {
            System.out.print("Enter search keyword (or press Enter to skip): ");
            String q = scanner.nextLine().trim();

            System.out.print("Sort By (date / amount / title / category) [default: date]: ");
            String sortBy = scanner.nextLine().trim();
            if (sortBy.isEmpty()) sortBy = "date";

            System.out.print("Sort Order (desc / asc) [default: desc]: ");
            String order = scanner.nextLine().trim();
            if (order.isEmpty()) order = "desc";

            System.out.print("Sorting Algorithm to Demonstrate (1=TimSort, 2=MergeSort, 3=QuickSort) [1]: ");
            String algoChoice = scanner.nextLine().trim();
            ExpenseSorter.Algorithm algo = ExpenseSorter.Algorithm.TIM_SORT;
            if ("2".equals(algoChoice)) algo = ExpenseSorter.Algorithm.MERGE_SORT;
            if ("3".equals(algoChoice)) algo = ExpenseSorter.Algorithm.QUICK_SORT;

            long startNs = System.nanoTime();
            List<Expense> results = service.getFilteredAndSortedExpenses(
                    q, null, null, null, null, null, null, sortBy, order, algo
            );
            long elapsedNs = System.nanoTime() - startNs;

            System.out.printf("\n[DSA Performance] Algorithm: %s | Matches: %d | Time: %.3f ms\n",
                    algo.name(), results.size(), elapsedNs / 1_000_000.0);
            printExpenseTable(results);
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void viewAnalyticsSummary(ExpenseService service) {
        try {
            ExpenseSummary s = service.getSummary();
            System.out.println("\n================ FINANCIAL SUMMARY ================");
            System.out.printf(" Total Expenses:      $%.2f across %d transactions\n", s.getTotalExpenses(), s.getTotalCount());
            System.out.printf(" Average Expense:     $%.2f\n", s.getAverageExpense());
            if (s.getHighestExpense() != null) {
                System.out.printf(" Highest Expense:     $%.2f (%s)\n", s.getHighestExpense().getAmount(), s.getHighestExpense().getTitle());
            }
            if (s.getTopCategory() != null) {
                System.out.printf(" Top Category:        %s %s ($%.2f)\n",
                        s.getTopCategory().getIcon(), s.getTopCategory().getDisplayName(), s.getTopCategoryAmount());
            }

            System.out.println("\n--- Category Breakdown ---");
            for (Map.Entry<Category, Double> entry : s.getCategoryBreakdown().entrySet()) {
                double pct = s.getCategoryPercentages().getOrDefault(entry.getKey(), 0.0);
                System.out.printf("  %-25s : $%8.2f (%5.1f%%)\n",
                        entry.getKey().getDisplayName(), entry.getValue(), pct);
            }

            System.out.println("\n--- Payment Methods ---");
            for (Map.Entry<PaymentMethod, Double> entry : s.getPaymentMethodBreakdown().entrySet()) {
                System.out.printf("  %-25s : $%8.2f\n", entry.getKey().getDisplayName(), entry.getValue());
            }
            System.out.println("===================================================");
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void viewBudgets(ExpenseService service) {
        try {
            String month = DateTimeUtil.getCurrentMonthYear();
            List<Budget> budgets = service.getBudgetsWithSpending(month);
            System.out.printf("\n============== MONTHLY BUDGETS (%s) ==============\n", month);
            if (budgets.isEmpty()) {
                System.out.println("No budgets configured yet. Use option 8 to set a budget.");
                return;
            }
            System.out.printf("%-4s | %-24s | %-12s | %-12s | %-10s | %-10s\n",
                    "ID", "Category", "Limit", "Spent", "Remaining", "Status");
            System.out.println("-------------------------------------------------------------------------------");
            for (Budget b : budgets) {
                String status = b.isExceeded() ? "EXCEEDED!" : String.format("%.1f%%", b.getPercentageUsed());
                System.out.printf("#%-3d | %s %-20s | $%10.2f | $%10.2f | $%8.2f | %s\n",
                        b.getId(), b.getCategory().getIcon(), b.getCategory().getDisplayName(),
                        b.getMonthlyLimit(), b.getSpent(), b.getRemaining(), status);
            }
            System.out.println("-------------------------------------------------------------------------------");
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void setBudget(ExpenseService service, Scanner scanner) {
        System.out.println("\n--- Set / Update Category Budget ---");
        try {
            Category[] categories = Category.values();
            for (int i = 0; i < categories.length; i++) {
                System.out.printf("  %d. %s %s\n", i + 1, categories[i].getIcon(), categories[i].getDisplayName());
            }
            System.out.print("Select Category: ");
            int catIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (catIdx < 0 || catIdx >= categories.length) {
                System.out.println("[!] Invalid category selection.");
                return;
            }

            System.out.print("Enter Monthly Budget Limit ($): ");
            double limit = Double.parseDouble(scanner.nextLine().trim());

            Budget budget = new Budget(categories[catIdx], limit, DateTimeUtil.getCurrentMonthYear());
            service.saveBudget(budget);
            System.out.printf("[✓] Budget set for %s: $%.2f/month\n", categories[catIdx].getDisplayName(), limit);
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    private static void printExpenseTable(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            System.out.println("\n(No expense records found)");
            return;
        }

        System.out.printf("\n%-4s | %-12s | %-24s | %-10s | %-18s | %-16s | %s\n",
                "ID", "Date", "Title", "Amount", "Category", "Payment Method", "Notes");
        System.out.println("---------------------------------------------------------------------------------------------------------------------");

        for (Expense e : expenses) {
            String title = (e.getTitle().length() > 23) ? e.getTitle().substring(0, 20) + "..." : e.getTitle();
            String notes = (e.getNotes() != null && e.getNotes().length() > 25) ? e.getNotes().substring(0, 22) + "..." : (e.getNotes() != null ? e.getNotes() : "");
            System.out.printf("#%-3d | %-12s | %-24s | $%8.2f | %s %-15s | %-16s | %s\n",
                    e.getId(),
                    DateTimeUtil.formatDate(e.getDate()),
                    title,
                    e.getAmount(),
                    e.getCategory().getIcon(),
                    e.getCategory().getDisplayName(),
                    e.getPaymentMethod().getDisplayName(),
                    notes);
        }
        System.out.println("---------------------------------------------------------------------------------------------------------------------");
        System.out.printf("Total Records: %d\n", expenses.size());
    }
}

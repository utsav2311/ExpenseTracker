package com.expensetracker.test;

import com.expensetracker.dao.InMemoryTransactionDAO;
import com.expensetracker.dsa.TransactionSearcher;
import com.expensetracker.dsa.TransactionSorter;
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
import com.expensetracker.util.SimpleJson;
import com.expensetracker.util.ValidationUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated test suite for Personal Income & Expense Tracker.
 * Demonstrates unit and integration testing without third-party frameworks.
 */
public class ExpenseTrackerTest {

    private static int totalTests = 0;
    private static int passedTests = 0;

    public static void runAllTests() {
        System.out.println("==========================================================");
        System.out.println(" Personal Income & Expense Tracker Automated Test Suite   ");
        System.out.println("==========================================================");

        testModelEncapsulationAndTypes();
        testCategoryTypeValidationConstraints();
        testBalanceAndSavingsCalculations();
        testCustomMergeSort();
        testCustomQuickSort();
        testBinarySearchByDate();
        testLinearSearchAndFilter();
        testSimpleJsonSerializationAndParsing();
        testServiceCrudAndBudgetTracking();

        System.out.println("\n----------------------------------------------------------");
        System.out.printf("Test Results: %d/%d Passed (%.1f%%)\n",
                passedTests, totalTests, (passedTests * 100.0) / totalTests);
        System.out.println("==========================================================");

        if (passedTests < totalTests) {
            System.exit(1);
        }
    }

    private static void assertTrue(String name, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("  [PASS] " + name);
        } else {
            System.err.println("  [FAIL] " + name);
        }
    }

    private static void assertEquals(String name, Object expected, Object actual) {
        totalTests++;
        boolean eq = (expected == null && actual == null) || (expected != null && expected.equals(actual));
        if (eq) {
            passedTests++;
            System.out.println("  [PASS] " + name);
        } else {
            System.err.printf("  [FAIL] %s: Expected [%s] but got [%s]\n", name, expected, actual);
        }
    }

    private static void testModelEncapsulationAndTypes() {
        System.out.println("\n[1] Testing Model Encapsulation & Types...");
        Transaction tx = new Transaction(1, 2, 5000.0, TransactionType.INCOME, "Freelance Gig", "Web app", "UPI", LocalDate.now());
        assertEquals("Transaction description getter", "Freelance Gig", tx.getDescription());
        assertEquals("Transaction amount getter", 5000.0, tx.getAmount());
        assertEquals("Transaction type getter", TransactionType.INCOME, tx.getTransactionType());
        assertTrue("isIncome() helper", tx.isIncome());
        assertTrue("!isExpense() helper", !tx.isExpense());

        boolean threwOnNegative = false;
        try {
            tx.setAmount(-100.0);
        } catch (IllegalArgumentException e) {
            threwOnNegative = true;
        }
        assertTrue("Amount cannot be negative via setter", threwOnNegative);

        boolean threwOnEmptyDesc = false;
        try {
            tx.setDescription("");
        } catch (IllegalArgumentException e) {
            threwOnEmptyDesc = true;
        }
        assertTrue("Description cannot be blank via setter", threwOnEmptyDesc);
    }

    private static void testCategoryTypeValidationConstraints() {
        System.out.println("\n[2] Testing Category Type Match Validation...");
        Category foodCategory = new Category(1, "Food", TransactionType.EXPENSE);
        Category salaryCategory = new Category(2, "Salary", TransactionType.INCOME);

        // Valid expense with Food
        Transaction validExpense = new Transaction(1, 1, 450.0, TransactionType.EXPENSE, "Lunch", "", "Cash", LocalDate.now());
        boolean validPassed = true;
        try {
            ValidationUtil.validateTransaction(validExpense, foodCategory);
        } catch (Exception e) {
            validPassed = false;
        }
        assertTrue("Valid expense with Food category passes validation", validPassed);

        // INVALID: Income transaction with Food category
        Transaction invalidIncome = new Transaction(1, 1, 5000.0, TransactionType.INCOME, "Mistaken Income", "", "Cash", LocalDate.now());
        boolean caughtMismatchedCategory = false;
        try {
            ValidationUtil.validateTransaction(invalidIncome, foodCategory);
        } catch (InvalidTransactionException e) {
            caughtMismatchedCategory = true;
        } catch (Exception ignored) {}
        assertTrue("Income transaction with Expense category throws InvalidTransactionException", caughtMismatchedCategory);

        // INVALID: Zero or negative amount via setter
        boolean caughtZeroAmount = false;
        try {
            validExpense.setAmount(0.0);
        } catch (IllegalArgumentException e) {
            caughtZeroAmount = true;
        }
        assertTrue("Zero amount throws IllegalArgumentException", caughtZeroAmount);
    }

    private static void testBalanceAndSavingsCalculations() {
        System.out.println("\n[3] Testing Balance & Savings Calculations...");
        DashboardSummary ds = new DashboardSummary();
        ds.setTotalIncome(65000.0);
        ds.setTotalExpense(22500.0);
        ds.setBalance(65000.0 - 22500.0);
        assertEquals("Dashboard balance = Total Income - Total Expenses (₹42,500.00)", 42500.0, ds.getBalance());

        MonthlySummary ms = new MonthlySummary();
        ms.setIncome(65000.0);
        ms.setExpenses(22500.0);
        ms.setBudgetAmount(30000.0);
        assertEquals("Monthly savings = Income - Expenses (₹42,500.00)", 42500.0, ms.getSavings());
        assertEquals("Remaining budget = ₹7,500.00", 7500.0, ms.getRemainingBudget());
        assertEquals("Budget percentage used = 75.0%", 75.0, ms.getBudgetPercentageUsed());
    }

    private static void testCustomMergeSort() {
        System.out.println("\n[4] Testing Hand-Crafted MergeSort on Transactions...");
        List<Transaction> list = new ArrayList<>();
        list.add(new Transaction(1, 1, 1500.0, TransactionType.EXPENSE, "A", "", "Cash", LocalDate.of(2026, 9, 10)));
        list.add(new Transaction(1, 1, 250.0, TransactionType.EXPENSE, "B", "", "Cash", LocalDate.of(2026, 9, 15)));
        list.add(new Transaction(1, 1, 5000.0, TransactionType.INCOME, "C", "", "Cash", LocalDate.of(2026, 9, 5)));

        TransactionSorter.mergeSort(list, TransactionSorter.BY_AMOUNT_ASC);
        assertEquals("MergeSort lowest amount first (250.0)", 250.0, list.get(0).getAmount());
        assertEquals("MergeSort middle amount (1500.0)", 1500.0, list.get(1).getAmount());
        assertEquals("MergeSort highest amount last (5000.0)", 5000.0, list.get(2).getAmount());
    }

    private static void testCustomQuickSort() {
        System.out.println("\n[5] Testing Hand-Crafted QuickSort on Transactions...");
        List<Transaction> list = new ArrayList<>();
        list.add(new Transaction(1, 1, 300.0, TransactionType.EXPENSE, "Movie", "", "UPI", LocalDate.of(2026, 9, 10)));
        list.add(new Transaction(1, 1, 50000.0, TransactionType.INCOME, "Salary", "", "Net Banking", LocalDate.of(2026, 9, 1)));
        list.add(new Transaction(1, 1, 50.0, TransactionType.EXPENSE, "Tea", "", "Cash", LocalDate.of(2026, 9, 15)));

        TransactionSorter.quickSort(list, TransactionSorter.BY_AMOUNT_DESC, 0, list.size() - 1);
        assertEquals("QuickSort highest amount first (50000.0)", 50000.0, list.get(0).getAmount());
        assertEquals("QuickSort lowest amount last (50.0)", 50.0, list.get(2).getAmount());
    }

    private static void testBinarySearchByDate() {
        System.out.println("\n[6] Testing Binary Search by Date...");
        List<Transaction> list = new ArrayList<>();
        list.add(new Transaction(1, 1, 100.0, TransactionType.EXPENSE, "D1", "", "Cash", LocalDate.of(2026, 9, 1)));
        list.add(new Transaction(1, 1, 200.0, TransactionType.EXPENSE, "D2", "", "Cash", LocalDate.of(2026, 9, 2)));
        list.add(new Transaction(1, 1, 300.0, TransactionType.EXPENSE, "D3_A", "", "Cash", LocalDate.of(2026, 9, 3)));
        list.add(new Transaction(1, 1, 350.0, TransactionType.INCOME, "D3_B", "", "Cash", LocalDate.of(2026, 9, 3)));
        list.add(new Transaction(1, 1, 400.0, TransactionType.EXPENSE, "D4", "", "Cash", LocalDate.of(2026, 9, 4)));

        List<Transaction> matches = TransactionSearcher.binarySearchByDate(list, LocalDate.of(2026, 9, 3));
        assertEquals("Binary search finds both transactions on Sep 3", 2, matches.size());
    }

    private static void testLinearSearchAndFilter() {
        System.out.println("\n[7] Testing Linear Search & Type Filtering...");
        List<Transaction> list = new ArrayList<>();
        list.add(new Transaction(1, 1, 50000.0, TransactionType.INCOME, "Tech Salary", "", "Bank", LocalDate.now()));
        list.add(new Transaction(1, 2, 750.0, TransactionType.EXPENSE, "Dinner Food", "", "UPI", LocalDate.now()));

        List<Transaction> incomeOnly = TransactionSearcher.filter(list, TransactionType.INCOME, null, null, null);
        assertEquals("Filter by INCOME returns 1 item", 1, incomeOnly.size());
        assertEquals("Income item description", "Tech Salary", incomeOnly.get(0).getDescription());

        List<Transaction> searchFood = TransactionSearcher.linearSearch(list, "dinner");
        assertEquals("Linear search finds 1 match for 'dinner'", 1, searchFood.size());
    }

    private static void testSimpleJsonSerializationAndParsing() {
        System.out.println("\n[8] Testing SimpleJson Parser & Serializer...");
        Transaction tx = new Transaction(101, 1, 5, 1200.0, TransactionType.EXPENSE, "Train Tickets", "AC coach", "UPI", LocalDate.of(2026, 9, 12));
        tx.setCategoryName("Travel");

        String json = SimpleJson.toJson(tx);
        assertTrue("JSON contains description", json.contains("\"description\":\"Train Tickets\""));
        assertTrue("JSON contains amount", json.contains("\"amount\":1200.00"));
        assertTrue("JSON contains type EXPENSE", json.contains("\"transactionType\":\"EXPENSE\""));

        Transaction parsed = SimpleJson.parseTransaction(json);
        assertEquals("Parsed description matches", "Train Tickets", parsed.getDescription());
        assertEquals("Parsed amount matches", 1200.0, parsed.getAmount());
        assertEquals("Parsed type matches EXPENSE", TransactionType.EXPENSE, parsed.getTransactionType());
    }

    private static void testServiceCrudAndBudgetTracking() {
        System.out.println("\n[9] Testing Service Layer CRUD & Budget Tracking...");
        InMemoryTransactionDAO inMemoryDao = new InMemoryTransactionDAO();
        TransactionService service = new TransactionService(inMemoryDao);

        try {
            // Add Income
            Transaction salary = new Transaction(1, 1, 60000.0, TransactionType.INCOME, "Monthly Salary", "", "Net Banking", LocalDate.now());
            Transaction created = service.addTransaction(salary);
            assertTrue("Assigned transaction ID > 0", created.getTransactionId() > 0);

            // Fetch
            Transaction fetched = service.getTransactionById(created.getTransactionId());
            assertEquals("Fetched matches created description", "Monthly Salary", fetched.getDescription());

            // Dashboard verification
            DashboardSummary ds = service.getDashboardSummary(LocalDate.now().getMonthValue(), LocalDate.now().getYear());
            assertTrue("Dashboard reflects positive balance", ds.getBalance() > 0);
            assertTrue("Total income includes new salary", ds.getTotalIncome() >= 60000.0);

            // Delete
            boolean deleted = service.deleteTransaction(created.getTransactionId());
            assertTrue("Transaction deleted successfully", deleted);

            // Verify 404 on deleted
            boolean caught404 = false;
            try {
                service.getTransactionById(created.getTransactionId());
            } catch (TransactionNotFoundException e) {
                caught404 = true;
            }
            assertTrue("Fetching deleted transaction throws TransactionNotFoundException", caught404);

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("Service test encountered unexpected failure: " + e.getMessage(), false);
        }
    }
}

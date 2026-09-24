package com.expensetracker.test;

import com.expensetracker.dsa.ExpenseAnalytics;
import com.expensetracker.dsa.ExpenseFilter;
import com.expensetracker.dsa.ExpenseSearcher;
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
import com.expensetracker.util.ValidationUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated test suite with zero external testing framework dependencies.
 * Demonstrates unit and integration testing capabilities using Core Java assertions.
 */
public class ExpenseTrackerTest {

    private static int totalTests = 0;
    private static int passedTests = 0;

    public static void runAllTests() {
        System.out.println("==========================================================");
        System.out.println("     Personal Expense Tracker Automated Test Suite        ");
        System.out.println("==========================================================");

        testModelEncapsulationAndValidation();
        testCustomExceptions();
        testCustomMergeSort();
        testCustomQuickSort();
        testBinarySearchAlgorithms();
        testLinearSearchAndFiltering();
        testAnalyticsAndAggregations();
        testJsonSerializationAndParsing();
        testServiceCrudAndBudgetTracking();

        System.out.println("\n----------------------------------------------------------");
        System.out.printf("Test Results: %d/%d Passed (%.1f%%)\n",
                passedTests, totalTests, (passedTests * 100.0) / totalTests);
        System.out.println("==========================================================");

        if (passedTests < totalTests) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("  [PASS] " + testName);
        } else {
            System.err.println("  [FAIL] " + testName);
        }
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        totalTests++;
        boolean equal = (expected == null && actual == null) || (expected != null && expected.equals(actual));
        if (equal) {
            passedTests++;
            System.out.println("  [PASS] " + testName);
        } else {
            System.err.printf("  [FAIL] %s: Expected [%s] but got [%s]\n", testName, expected, actual);
        }
    }

    private static void testModelEncapsulationAndValidation() {
        System.out.println("\n[1] Testing Model Encapsulation & Validation...");
        Expense e = new Expense("Groceries", 85.50, Category.FOOD, PaymentMethod.UPI, LocalDate.now(), "Milk and bread");
        assertEquals("Expense title getter", "Groceries", e.getTitle());
        assertEquals("Expense amount getter", 85.50, e.getAmount());
        assertEquals("Expense category getter", Category.FOOD, e.getCategory());
        assertEquals("Expense payment method", PaymentMethod.UPI, e.getPaymentMethod());

        boolean threwOnNegative = false;
        try {
            e.setAmount(-10.0);
        } catch (IllegalArgumentException ex) {
            threwOnNegative = true;
        }
        assertTrue("Amount cannot be negative", threwOnNegative);

        boolean threwOnEmptyTitleSetter = false;
        try {
            e.setTitle("");
        } catch (IllegalArgumentException ex) {
            threwOnEmptyTitleSetter = true;
        }
        assertTrue("Title cannot be empty via setter", threwOnEmptyTitleSetter);

        boolean validationFailedOnBlank = false;
        try {
            Expense blankExpense = new Expense(); // default title is null
            ValidationUtil.validateExpense(blankExpense);
        } catch (InvalidExpenseException ex) {
            validationFailedOnBlank = true;
        }
        assertTrue("Validation fails on empty title", validationFailedOnBlank);
    }

    private static void testCustomExceptions() {
        System.out.println("\n[2] Testing Custom Exception Hierarchy...");
        ExpenseNotFoundException notFound = new ExpenseNotFoundException(999);
        assertEquals("ExpenseNotFoundException ID preservation", 999, notFound.getExpenseId());
        assertTrue("ExpenseNotFoundException has clear message", notFound.getMessage().contains("999"));
    }

    private static void testCustomMergeSort() {
        System.out.println("\n[3] Testing Hand-Crafted MergeSort (O(N log N))...");
        List<Expense> list = new ArrayList<>();
        list.add(new Expense("A", 100.0, Category.FOOD, PaymentMethod.CASH, LocalDate.of(2026, 3, 10), ""));
        list.add(new Expense("B", 25.0, Category.OTHER, PaymentMethod.CASH, LocalDate.of(2026, 3, 15), ""));
        list.add(new Expense("C", 250.0, Category.HOUSING, PaymentMethod.CASH, LocalDate.of(2026, 3, 5), ""));

        // Sort by amount ascending using MergeSort
        ExpenseSorter.mergeSort(list, ExpenseSorter.BY_AMOUNT_ASC);
        assertEquals("MergeSort first element lowest amount", 25.0, list.get(0).getAmount());
        assertEquals("MergeSort middle element", 100.0, list.get(1).getAmount());
        assertEquals("MergeSort last element highest amount", 250.0, list.get(2).getAmount());
    }

    private static void testCustomQuickSort() {
        System.out.println("\n[4] Testing Hand-Crafted QuickSort (In-Place Partitioning)...");
        List<Expense> list = new ArrayList<>();
        list.add(new Expense("Movie", 30.0, Category.ENTERTAINMENT, PaymentMethod.UPI, LocalDate.of(2026, 1, 10), ""));
        list.add(new Expense("Rent", 1200.0, Category.HOUSING, PaymentMethod.NET_BANKING, LocalDate.of(2026, 1, 1), ""));
        list.add(new Expense("Coffee", 5.0, Category.FOOD, PaymentMethod.CASH, LocalDate.of(2026, 1, 15), ""));
        list.add(new Expense("Bus", 2.5, Category.TRANSPORTATION, PaymentMethod.CASH, LocalDate.of(2026, 1, 12), ""));

        // Sort descending by amount using QuickSort
        ExpenseSorter.quickSort(list, ExpenseSorter.BY_AMOUNT_DESC, 0, list.size() - 1);
        assertEquals("QuickSort highest element first", 1200.0, list.get(0).getAmount());
        assertEquals("QuickSort lowest element last", 2.5, list.get(3).getAmount());
    }

    private static void testBinarySearchAlgorithms() {
        System.out.println("\n[5] Testing Binary Search Algorithms...");
        List<Expense> list = new ArrayList<>();
        list.add(new Expense("Day1", 10.0, Category.FOOD, PaymentMethod.CASH, LocalDate.of(2026, 5, 1), ""));
        list.add(new Expense("Day2", 20.0, Category.FOOD, PaymentMethod.CASH, LocalDate.of(2026, 5, 2), ""));
        list.add(new Expense("Day3", 30.0, Category.FOOD, PaymentMethod.CASH, LocalDate.of(2026, 5, 3), ""));
        list.add(new Expense("Day3_B", 35.0, Category.FOOD, PaymentMethod.CASH, LocalDate.of(2026, 5, 3), ""));
        list.add(new Expense("Day4", 40.0, Category.FOOD, PaymentMethod.CASH, LocalDate.of(2026, 5, 4), ""));

        // Binary search by date
        List<Expense> onMay3 = ExpenseSearcher.binarySearchByDate(list, LocalDate.of(2026, 5, 3));
        assertEquals("Binary search finds both expenses on May 3", 2, onMay3.size());

        // Binary search closest amount
        Expense closest = ExpenseSearcher.binarySearchClosestAmount(list, 28.5);
        assertEquals("Binary search finds closest amount to 28.5 (30.0)", 30.0, closest.getAmount());
    }

    private static void testLinearSearchAndFiltering() {
        System.out.println("\n[6] Testing Linear Search & Multi-Criteria Filtering...");
        List<Expense> list = new ArrayList<>();
        list.add(new Expense("Organic Apples", 15.0, Category.FOOD, PaymentMethod.CASH, LocalDate.of(2026, 2, 1), "Sweet"));
        list.add(new Expense("Office Laptop Stand", 45.0, Category.SHOPPING, PaymentMethod.CREDIT_CARD, LocalDate.of(2026, 2, 10), "Aluminum"));
        list.add(new Expense("Apple Music Subscription", 9.99, Category.ENTERTAINMENT, PaymentMethod.UPI, LocalDate.of(2026, 2, 15), "Streaming"));

        List<Expense> searchApples = ExpenseSearcher.linearSearch(list, "apple");
        assertEquals("Linear search finds 2 matches for 'apple'", 2, searchApples.size());

        List<Expense> filtered = ExpenseFilter.filter(list, Category.FOOD, null, null, null, 10.0, 50.0);
        assertEquals("Filtering by FOOD and amount between 10 and 50", 1, filtered.size());
        assertEquals("Filtered item is Organic Apples", "Organic Apples", filtered.get(0).getTitle());
    }

    private static void testAnalyticsAndAggregations() {
        System.out.println("\n[7] Testing Financial Analytics & Aggregations...");
        List<Expense> list = new ArrayList<>();
        list.add(new Expense("Food A", 100.0, Category.FOOD, PaymentMethod.CASH, LocalDate.of(2026, 3, 1), ""));
        list.add(new Expense("Food B", 50.0, Category.FOOD, PaymentMethod.UPI, LocalDate.of(2026, 3, 2), ""));
        list.add(new Expense("Gas", 50.0, Category.TRANSPORTATION, PaymentMethod.CREDIT_CARD, LocalDate.of(2026, 3, 3), ""));

        ExpenseSummary summary = ExpenseAnalytics.computeSummary(list);
        assertEquals("Total expenses sum", 200.0, summary.getTotalExpenses());
        assertEquals("Total transaction count", 3, summary.getTotalCount());
        assertEquals("Average expense calculation", 66.67, summary.getAverageExpense());
        assertEquals("Top category identification", Category.FOOD, summary.getTopCategory());
        assertEquals("Top category amount sum", 150.0, summary.getTopCategoryAmount());
        assertEquals("Category percentage for FOOD (75%)", 75.0, summary.getCategoryPercentages().get(Category.FOOD));
    }

    private static void testJsonSerializationAndParsing() {
        System.out.println("\n[8] Testing SimpleJson Parser & Serializer...");
        Expense orig = new Expense("Dinner", 45.0, Category.FOOD, PaymentMethod.UPI, LocalDate.of(2026, 4, 1), "Tasty");
        String json = SimpleJson.toJson(orig);
        assertTrue("JSON contains title", json.contains("\"title\":\"Dinner\""));
        assertTrue("JSON contains amount", json.contains("\"amount\":45.00"));
        assertTrue("JSON contains category", json.contains("\"category\":\"FOOD\""));

        Expense parsed = SimpleJson.parseExpense(json);
        assertEquals("Parsed expense title", "Dinner", parsed.getTitle());
        assertEquals("Parsed expense amount", 45.0, parsed.getAmount());
        assertEquals("Parsed expense category", Category.FOOD, parsed.getCategory());
        assertEquals("Parsed expense payment method", PaymentMethod.UPI, parsed.getPaymentMethod());
    }

    private static void testServiceCrudAndBudgetTracking() {
        System.out.println("\n[9] Testing Service Layer CRUD & Budget Tracking...");
        ExpenseService service = new ExpenseService();
        try {
            // Add expense
            Expense e = new Expense("Test Item", 99.0, Category.SHOPPING, PaymentMethod.CASH, LocalDate.now(), "Testing");
            Expense created = service.createExpense(e);
            assertTrue("Assigned ID is greater than 0", created.getId() > 0);

            // Fetch
            Expense fetched = service.getExpenseById(created.getId());
            assertEquals("Fetched matches created", created.getTitle(), fetched.getTitle());

            // Set budget and test threshold calculation
            Budget budget = new Budget(Category.SHOPPING, 50.0, DateTimeUtil.getCurrentMonthYear());
            service.saveBudget(budget);

            List<Budget> budgets = service.getBudgetsWithSpending(DateTimeUtil.getCurrentMonthYear());
            Budget shoppingBudget = null;
            for (Budget b : budgets) {
                if (b.getCategory() == Category.SHOPPING) {
                    shoppingBudget = b;
                    break;
                }
            }
            assertTrue("Shopping budget exists", shoppingBudget != null);
            assertTrue("Budget correctly flags exceeded condition", shoppingBudget.isExceeded());

            // Delete
            boolean deleted = service.deleteExpense(created.getId());
            assertTrue("Expense deleted successfully", deleted);
        } catch (Exception ex) {
            ex.printStackTrace();
            assertTrue("Service CRUD operation failed: " + ex.getMessage(), false);
        }
    }
}

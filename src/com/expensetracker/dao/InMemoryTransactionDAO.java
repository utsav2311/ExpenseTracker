package com.expensetracker.dao;

import com.expensetracker.exception.DatabaseOperationException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategorySummary;
import com.expensetracker.model.DashboardSummary;
import com.expensetracker.model.MonthlySummary;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * In-Memory demonstration implementation of {@link TransactionDAO} using Java Collections.
 * Utilized when storage.mode=memory is explicitly enabled or during unit tests.
 * Demonstrates:
 *  - LinkedHashMap for O(1) key-based lookups preserving insertion order
 *  - Thread-safe ID sequencing with AtomicInteger
 *  - Java Streams for group-by, filtering, and aggregation
 */
public class InMemoryTransactionDAO implements TransactionDAO {

    private final Map<Integer, Transaction> transactionMap = new LinkedHashMap<>();
    private final Map<Integer, Category> categoryMap = new LinkedHashMap<>();
    private final Map<String, Budget> budgetMap = new LinkedHashMap<>();

    private final AtomicInteger transactionIdSeq = new AtomicInteger(100);
    private final AtomicInteger categoryIdSeq = new AtomicInteger(1);
    private final AtomicInteger budgetIdSeq = new AtomicInteger(1);

    public InMemoryTransactionDAO() {
        seedInitialData();
    }

    private synchronized void seedInitialData() {
        // Income Categories
        addCategoryInternal("Salary", TransactionType.INCOME);
        addCategoryInternal("Freelance", TransactionType.INCOME);
        addCategoryInternal("Bonus", TransactionType.INCOME);
        addCategoryInternal("Investment", TransactionType.INCOME);
        addCategoryInternal("Other Income", TransactionType.INCOME);

        // Expense Categories
        addCategoryInternal("Food", TransactionType.EXPENSE);
        addCategoryInternal("Travel", TransactionType.EXPENSE);
        addCategoryInternal("Shopping", TransactionType.EXPENSE);
        addCategoryInternal("Bills", TransactionType.EXPENSE);
        addCategoryInternal("Entertainment", TransactionType.EXPENSE);
        addCategoryInternal("Education", TransactionType.EXPENSE);
        addCategoryInternal("Health", TransactionType.EXPENSE);
        addCategoryInternal("Rent", TransactionType.EXPENSE);
        addCategoryInternal("Other Expense", TransactionType.EXPENSE);

        // Seed Sample Realistic Transactions (INR / ₹)
        LocalDate today = LocalDate.now();
        int catSalary = findCategoryIdByName("Salary");
        int catFreelance = findCategoryIdByName("Freelance");
        int catRent = findCategoryIdByName("Rent");
        int catFood = findCategoryIdByName("Food");
        int catTravel = findCategoryIdByName("Travel");
        int catBills = findCategoryIdByName("Bills");
        int catShopping = findCategoryIdByName("Shopping");

        // Income: ₹50,000 + ₹15,000 = ₹65,000
        addInternal(new Transaction(1, catSalary, 50000.00, TransactionType.INCOME, "Monthly Tech Salary", "Direct employer credit", "Net Banking", today.minusDays(5)));
        addInternal(new Transaction(1, catFreelance, 15000.00, TransactionType.INCOME, "Frontend Consulting Project", "Milestone 1 payment", "UPI", today.minusDays(2)));

        // Expenses: ₹15,000 + ₹750 + ₹1,200 + ₹2,400 + ₹3,150 = ₹22,500
        addInternal(new Transaction(1, catRent, 15000.00, TransactionType.EXPENSE, "Apartment Rent", "Monthly residential rent", "Net Banking", today.minusDays(4)));
        addInternal(new Transaction(1, catFood, 750.00, TransactionType.EXPENSE, "Dinner with Friends", "Weekend dining outing", "UPI", today.minusDays(1)));
        addInternal(new Transaction(1, catTravel, 1200.00, TransactionType.EXPENSE, "Metro & Cab Travel", "Weekly office commute", "Debit Card", today.minusDays(3)));
        addInternal(new Transaction(1, catBills, 2400.00, TransactionType.EXPENSE, "Electricity & Wi-Fi Bills", "Utility payments", "UPI", today.minusDays(4)));
        addInternal(new Transaction(1, catShopping, 3150.00, TransactionType.EXPENSE, "Clothing & Essentials", "Weekend shopping mall", "Credit Card", today.minusDays(6)));

        // Seed Monthly Budget: ₹30,000
        saveBudgetInternal(new Budget(1, today.getMonthValue(), today.getYear(), 30000.00));
    }

    private void addCategoryInternal(String name, TransactionType type) {
        int id = categoryIdSeq.getAndIncrement();
        Category c = new Category(id, name, type);
        categoryMap.put(id, c);
    }

    private int findCategoryIdByName(String name) {
        for (Category c : categoryMap.values()) {
            if (c.getCategoryName().equalsIgnoreCase(name)) {
                return c.getCategoryId();
            }
        }
        return 1;
    }

    private void addInternal(Transaction tx) {
        int id = transactionIdSeq.incrementAndGet();
        tx.setTransactionId(id);
        Category c = categoryMap.get(tx.getCategoryId());
        if (c != null) {
            tx.setCategoryName(c.getCategoryName());
        }
        transactionMap.put(id, tx);
    }

    private void saveBudgetInternal(Budget b) {
        if (b.getBudgetId() == 0) {
            b.setBudgetId(budgetIdSeq.incrementAndGet());
        }
        String key = b.getUserId() + "_" + b.getMonth() + "_" + b.getYear();
        budgetMap.put(key, b);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getStorageType() {
        return "In-Memory Java Collections";
    }

    @Override
    public synchronized void addTransaction(Transaction tx) throws DatabaseOperationException {
        int id = transactionIdSeq.incrementAndGet();
        tx.setTransactionId(id);
        Category c = categoryMap.get(tx.getCategoryId());
        if (c != null) {
            tx.setCategoryName(c.getCategoryName());
        }
        transactionMap.put(id, tx);
    }

    @Override
    public synchronized Transaction getTransactionById(int id) throws DatabaseOperationException {
        return transactionMap.get(id);
    }

    @Override
    public synchronized List<Transaction> getAllTransactions() throws DatabaseOperationException {
        List<Transaction> list = new ArrayList<>(transactionMap.values());
        list.sort(Collections.reverseOrder());
        return list;
    }

    @Override
    public synchronized boolean updateTransaction(Transaction tx) throws DatabaseOperationException {
        if (transactionMap.containsKey(tx.getTransactionId())) {
            Category c = categoryMap.get(tx.getCategoryId());
            if (c != null) {
                tx.setCategoryName(c.getCategoryName());
            }
            transactionMap.put(tx.getTransactionId(), tx);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean deleteTransaction(int id) throws DatabaseOperationException {
        return transactionMap.remove(id) != null;
    }

    @Override
    public synchronized List<Transaction> findByType(TransactionType type) throws DatabaseOperationException {
        return transactionMap.values().stream()
                .filter(t -> t.getTransactionType() == type)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Transaction> findByCategory(int categoryId) throws DatabaseOperationException {
        return transactionMap.values().stream()
                .filter(t -> t.getCategoryId() == categoryId)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Transaction> findByDateRange(LocalDate startDate, LocalDate endDate) throws DatabaseOperationException {
        return transactionMap.values().stream()
                .filter(t -> !t.getTransactionDate().isBefore(startDate) && !t.getTransactionDate().isAfter(endDate))
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Transaction> getRecentTransactions(int limit) throws DatabaseOperationException {
        return transactionMap.values().stream()
                .sorted(Collections.reverseOrder())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Transaction> getTopExpenses(int limit) throws DatabaseOperationException {
        return transactionMap.values().stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .sorted((a, b) -> Double.compare(b.getAmount(), a.getAmount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Category> getAllCategories() throws DatabaseOperationException {
        return new ArrayList<>(categoryMap.values());
    }

    @Override
    public synchronized List<Category> getCategoriesByType(TransactionType type) throws DatabaseOperationException {
        return categoryMap.values().stream()
                .filter(c -> c.getCategoryType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized Category getCategoryById(int categoryId) throws DatabaseOperationException {
        return categoryMap.get(categoryId);
    }

    @Override
    public synchronized Budget getBudget(int userId, int month, int year) throws DatabaseOperationException {
        String key = userId + "_" + month + "_" + year;
        Budget b = budgetMap.get(key);
        if (b != null) return b;
        return new Budget(0, userId, month, year, 0.0);
    }

    @Override
    public synchronized void saveBudget(Budget budget) throws DatabaseOperationException {
        saveBudgetInternal(budget);
    }

    @Override
    public synchronized DashboardSummary getDashboardSummary(int month, int year) throws DatabaseOperationException {
        DashboardSummary dashboard = new DashboardSummary();
        double totalIncome = 0.0;
        double totalExpense = 0.0;
        double monthExpense = 0.0;

        for (Transaction t : transactionMap.values()) {
            if (t.getTransactionType() == TransactionType.INCOME) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
                if (t.getTransactionDate().getMonthValue() == month && t.getTransactionDate().getYear() == year) {
                    monthExpense += t.getAmount();
                }
            }
        }

        dashboard.setTotalIncome(totalIncome);
        dashboard.setTotalExpense(totalExpense);
        dashboard.setBalance(totalIncome - totalExpense);
        dashboard.setTransactionCount(transactionMap.size());

        Budget b = getBudget(1, month, year);
        b.setSpent(monthExpense);
        dashboard.setMonthlyBudget(b.getBudgetAmount());
        dashboard.setBudgetRemaining(b.getRemaining());
        dashboard.setBudgetPercentage(b.getPercentageUsed());

        dashboard.setRecentTransactions(getRecentTransactions(5));
        return dashboard;
    }

    @Override
    public synchronized MonthlySummary getMonthlySummary(int month, int year) throws DatabaseOperationException {
        MonthlySummary summary = new MonthlySummary();
        summary.setMonth(month);
        summary.setYear(year);
        summary.setMonthName(Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year);

        double monthIncome = 0.0;
        double monthExpense = 0.0;
        int count = 0;
        Transaction highestExp = null;

        for (Transaction t : transactionMap.values()) {
            if (t.getTransactionDate().getMonthValue() == month && t.getTransactionDate().getYear() == year) {
                count++;
                if (t.getTransactionType() == TransactionType.INCOME) {
                    monthIncome += t.getAmount();
                } else {
                    monthExpense += t.getAmount();
                    if (highestExp == null || t.getAmount() > highestExp.getAmount()) {
                        highestExp = t;
                    }
                }
            }
        }

        summary.setIncome(monthIncome);
        summary.setExpenses(monthExpense);
        summary.setTransactionCount(count);
        summary.setHighestExpense(highestExp);

        List<CategorySummary> catReports = getCategoryExpenseReport(month, year);
        if (!catReports.isEmpty()) {
            CategorySummary top = catReports.get(0);
            summary.setHighestSpendingCategory(top.getCategoryName());
            summary.setHighestSpendingCategoryAmount(top.getTotalAmount());
        }

        Budget b = getBudget(1, month, year);
        summary.setBudgetAmount(b.getBudgetAmount());

        return summary;
    }

    @Override
    public synchronized List<CategorySummary> getCategoryExpenseReport(int month, int year) throws DatabaseOperationException {
        Map<Integer, Double> catTotals = new LinkedHashMap<>();
        Map<Integer, Integer> catCounts = new LinkedHashMap<>();
        double grandTotal = 0.0;

        for (Transaction t : transactionMap.values()) {
            if (t.getTransactionType() == TransactionType.EXPENSE &&
                    t.getTransactionDate().getMonthValue() == month && t.getTransactionDate().getYear() == year) {
                int catId = t.getCategoryId();
                catTotals.put(catId, catTotals.getOrDefault(catId, 0.0) + t.getAmount());
                catCounts.put(catId, catCounts.getOrDefault(catId, 0) + 1);
                grandTotal += t.getAmount();
            }
        }

        List<CategorySummary> list = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : catTotals.entrySet()) {
            Category c = categoryMap.get(entry.getKey());
            String name = (c != null) ? c.getCategoryName() : ("Cat#" + entry.getKey());
            CategorySummary cs = new CategorySummary(entry.getKey(), name, TransactionType.EXPENSE, entry.getValue(), catCounts.get(entry.getKey()));
            if (grandTotal > 0) {
                cs.setPercentage(Math.round((entry.getValue() / grandTotal) * 1000.0) / 10.0);
            }
            list.add(cs);
        }

        list.sort((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));
        return list;
    }

    public synchronized void resetAndReseed() {
        transactionMap.clear();
        categoryMap.clear();
        budgetMap.clear();
        transactionIdSeq.set(100);
        categoryIdSeq.set(1);
        budgetIdSeq.set(1);
        seedInitialData();
    }
}

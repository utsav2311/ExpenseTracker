package com.expensetracker.dao;

import com.expensetracker.exception.DatabaseOperationException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.util.DateTimeUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * In-Memory fallback implementation of {@link ExpenseDAO} using Java Collections Framework.
 * Demonstrates:
 *  - Java Collections: LinkedHashMap, ArrayList, Streams
 *  - Thread-safe ID generation with AtomicInteger
 *  - Seamless fallback when MySQL database server is offline
 */
public class InMemoryExpenseDAO implements ExpenseDAO {

    private final Map<Integer, Expense> expenseMap = new LinkedHashMap<>();
    private final Map<String, Budget> budgetMap = new LinkedHashMap<>();
    private final AtomicInteger expenseIdCounter = new AtomicInteger(100);
    private final AtomicInteger budgetIdCounter = new AtomicInteger(10);

    public InMemoryExpenseDAO() {
        seedInitialData();
    }

    private void seedInitialData() {
        LocalDate today = LocalDate.now();
        String currentMonth = DateTimeUtil.getCurrentMonthYear();

        // Seed realistic sample expenses
        addInternal(new Expense("Whole Foods Groceries", 142.50, Category.FOOD, PaymentMethod.CREDIT_CARD, today.minusDays(1), "Weekly organic grocery run"));
        addInternal(new Expense("Apartment Rent", 1250.00, Category.HOUSING, PaymentMethod.NET_BANKING, today.minusDays(5), "Monthly rent payment"));
        addInternal(new Expense("Metro Card Recharge", 55.00, Category.TRANSPORTATION, PaymentMethod.DEBIT_CARD, today.minusDays(3), "Monthly transit pass"));
        addInternal(new Expense("Electric & Gas Bill", 94.20, Category.UTILITIES, PaymentMethod.UPI, today.minusDays(4), "City power utility bill"));
        addInternal(new Expense("Cinema Tickets & Popcorn", 38.50, Category.ENTERTAINMENT, PaymentMethod.UPI, today.minusDays(2), "Weekend movie outing"));
        addInternal(new Expense("Pharmacy Prescription", 45.00, Category.HEALTHCARE, PaymentMethod.CASH, today.minusDays(6), "Vitamins and allergy medicine"));
        addInternal(new Expense("Running Shoes", 89.99, Category.SHOPPING, PaymentMethod.CREDIT_CARD, today.minusDays(7), "Spring fitness gear"));
        addInternal(new Expense("Udemy Java Masterclass", 19.99, Category.EDUCATION, PaymentMethod.CREDIT_CARD, today.minusDays(8), "Advanced Java course"));
        addInternal(new Expense("Morning Espresso & Bagel", 8.75, Category.FOOD, PaymentMethod.UPI, today, "Coffee before office"));
        addInternal(new Expense("Uber Ride to Airport", 34.20, Category.TRANSPORTATION, PaymentMethod.CREDIT_CARD, today.minusDays(9), "Airport taxi"));

        // Seed monthly budgets
        saveBudgetInternal(new Budget(Category.FOOD, 400.00, currentMonth));
        saveBudgetInternal(new Budget(Category.HOUSING, 1300.00, currentMonth));
        saveBudgetInternal(new Budget(Category.TRANSPORTATION, 150.00, currentMonth));
        saveBudgetInternal(new Budget(Category.UTILITIES, 150.00, currentMonth));
        saveBudgetInternal(new Budget(Category.ENTERTAINMENT, 100.00, currentMonth));
        saveBudgetInternal(new Budget(Category.SHOPPING, 200.00, currentMonth));
    }

    private synchronized void addInternal(Expense e) {
        int id = expenseIdCounter.incrementAndGet();
        e.setId(id);
        expenseMap.put(id, e);
    }

    private synchronized void saveBudgetInternal(Budget b) {
        if (b.getId() == 0) {
            b.setId(budgetIdCounter.incrementAndGet());
        }
        String key = b.getCategory().name() + "_" + b.getMonthYear();
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
    public synchronized void add(Expense expense) throws DatabaseOperationException {
        int id = expenseIdCounter.incrementAndGet();
        expense.setId(id);
        expenseMap.put(id, expense);
    }

    @Override
    public synchronized Expense findById(int id) throws DatabaseOperationException {
        return expenseMap.get(id);
    }

    @Override
    public synchronized List<Expense> findAll() throws DatabaseOperationException {
        List<Expense> list = new ArrayList<>(expenseMap.values());
        list.sort(Collections.reverseOrder());
        return list;
    }

    @Override
    public synchronized boolean update(Expense expense) throws DatabaseOperationException {
        if (expenseMap.containsKey(expense.getId())) {
            expenseMap.put(expense.getId(), expense);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean delete(int id) throws DatabaseOperationException {
        return expenseMap.remove(id) != null;
    }

    @Override
    public synchronized List<Expense> findByCategory(Category category) throws DatabaseOperationException {
        return expenseMap.values().stream()
                .filter(e -> e.getCategory() == category)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Expense> findByDateRange(LocalDate startDate, LocalDate endDate) throws DatabaseOperationException {
        return expenseMap.values().stream()
                .filter(e -> !e.getDate().isBefore(startDate) && !e.getDate().isAfter(endDate))
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Budget> findAllBudgets() throws DatabaseOperationException {
        return new ArrayList<>(budgetMap.values());
    }

    @Override
    public synchronized void saveBudget(Budget budget) throws DatabaseOperationException {
        saveBudgetInternal(budget);
    }

    @Override
    public synchronized boolean deleteBudget(int id) throws DatabaseOperationException {
        String toRemove = null;
        for (Map.Entry<String, Budget> entry : budgetMap.entrySet()) {
            if (entry.getValue().getId() == id) {
                toRemove = entry.getKey();
                break;
            }
        }
        if (toRemove != null) {
            budgetMap.remove(toRemove);
            return true;
        }
        return false;
    }

    public synchronized void clearAndReseed() {
        expenseMap.clear();
        budgetMap.clear();
        expenseIdCounter.set(100);
        budgetIdCounter.set(10);
        seedInitialData();
    }
}

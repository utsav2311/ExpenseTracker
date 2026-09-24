package com.expensetracker.service;

import com.expensetracker.dao.ExpenseDAO;
import com.expensetracker.dao.ExpenseDAOImpl;
import com.expensetracker.dao.InMemoryExpenseDAO;
import com.expensetracker.dsa.ExpenseAnalytics;
import com.expensetracker.dsa.ExpenseFilter;
import com.expensetracker.dsa.ExpenseSearcher;
import com.expensetracker.dsa.ExpenseSorter;
import com.expensetracker.exception.BudgetExceededException;
import com.expensetracker.exception.DatabaseOperationException;
import com.expensetracker.exception.ExpenseNotFoundException;
import com.expensetracker.exception.InvalidExpenseException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseSummary;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.util.DateTimeUtil;
import com.expensetracker.util.ValidationUtil;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service Layer coordinating business logic, validations, budget checks,
 * persistence strategy (MySQL JDBC vs In-Memory), and DSA routines.
 */
public class ExpenseService {

    private final ExpenseDAO activeDao;
    private final InMemoryExpenseDAO inMemoryFallback;
    private final boolean usingDatabase;

    public ExpenseService() {
        this.inMemoryFallback = new InMemoryExpenseDAO();
        ExpenseDAO jdbcDao = null;
        boolean dbAvailable = false;

        try {
            jdbcDao = new ExpenseDAOImpl();
            if (jdbcDao.isAvailable()) {
                dbAvailable = true;
                System.out.println("[ExpenseService] Successfully connected to MySQL database via JDBC.");
            } else {
                System.out.println("[ExpenseService] MySQL not reachable. Falling back to In-Memory Java Collections storage.");
            }
        } catch (Throwable t) {
            System.out.println("[ExpenseService] JDBC initialization notice (" + t.getMessage() + "). Using In-Memory storage.");
        }

        this.usingDatabase = dbAvailable;
        this.activeDao = dbAvailable ? jdbcDao : inMemoryFallback;
    }

    public boolean isUsingDatabase() {
        return usingDatabase;
    }

    public String getStorageType() {
        return activeDao.getStorageType();
    }

    // =========================================================================
    //  EXPENSE CRUD OPERATIONS
    // =========================================================================

    /**
     * Creates a new Expense after validation and budget threshold check.
     */
    public Expense createExpense(Expense expense) throws InvalidExpenseException, DatabaseOperationException {
        ValidationUtil.validateExpense(expense);
        activeDao.add(expense);
        return expense;
    }

    /**
     * Checks if adding this expense would exceed the monthly budget for its category.
     * Returns the projected Budget if exceeded, or null if within budget.
     */
    public Budget checkBudgetAlert(Expense expense) {
        if (expense == null || expense.getCategory() == null) return null;
        try {
            String monthYear = DateTimeUtil.getMonthYear(expense.getDate());
            List<Budget> budgets = getBudgetsWithSpending(monthYear);
            for (Budget b : budgets) {
                if (b.getCategory() == expense.getCategory() && b.getMonthYear().equals(monthYear)) {
                    if (b.isExceeded()) {
                        return b;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public Expense getExpenseById(int id) throws ExpenseNotFoundException, DatabaseOperationException {
        Expense expense = activeDao.findById(id);
        if (expense == null) {
            throw new ExpenseNotFoundException(id);
        }
        return expense;
    }

    public Expense updateExpense(int id, Expense updated) throws ExpenseNotFoundException, InvalidExpenseException, DatabaseOperationException {
        ValidationUtil.validateExpense(updated);
        updated.setId(id);
        boolean success = activeDao.update(updated);
        if (!success) {
            throw new ExpenseNotFoundException(id);
        }
        return updated;
    }

    public boolean deleteExpense(int id) throws ExpenseNotFoundException, DatabaseOperationException {
        boolean deleted = activeDao.delete(id);
        if (!deleted) {
            throw new ExpenseNotFoundException(id);
        }
        return true;
    }

    // =========================================================================
    //  SEARCH, FILTER, AND SORT PIPELINE
    // =========================================================================

    /**
     * Retrieves expenses processed through the full query pipeline:
     * 1. Multi-criteria Filtering (Category, Payment Method, Date Range, Amount Range)
     * 2. Text Search (Linear keyword matching across title, notes, etc.)
     * 3. Sorting (MergeSort, QuickSort, or TimSort with custom Comparator)
     */
    public List<Expense> getFilteredAndSortedExpenses(String query,
                                                      Category category,
                                                      PaymentMethod paymentMethod,
                                                      LocalDate startDate,
                                                      LocalDate endDate,
                                                      Double minAmount,
                                                      Double maxAmount,
                                                      String sortBy,
                                                      String sortOrder,
                                                      ExpenseSorter.Algorithm algo) throws DatabaseOperationException {
        // Base retrieval
        List<Expense> all = activeDao.findAll();

        // Step 1: Filter
        List<Expense> filtered = ExpenseFilter.filter(all, category, paymentMethod, startDate, endDate, minAmount, maxAmount);

        // Step 2: Search
        List<Expense> searched = (query != null && !query.trim().isEmpty())
                ? ExpenseSearcher.linearSearch(filtered, query)
                : filtered;

        // Step 3: Sort using DSA algorithm
        Comparator<Expense> cmp = ExpenseSorter.getComparator(sortBy, sortOrder);
        ExpenseSorter.Algorithm algorithm = (algo != null) ? algo : ExpenseSorter.Algorithm.TIM_SORT;

        return ExpenseSorter.sort(searched, cmp, algorithm);
    }

    public List<Expense> getAllExpenses() throws DatabaseOperationException {
        return activeDao.findAll();
    }

    // =========================================================================
    //  ANALYTICS & SUMMARY
    // =========================================================================

    public ExpenseSummary getSummary() throws DatabaseOperationException {
        List<Expense> expenses = activeDao.findAll();
        return ExpenseAnalytics.computeSummary(expenses);
    }

    // =========================================================================
    //  BUDGET MANAGEMENT
    // =========================================================================

    public List<Budget> getBudgetsWithSpending(String monthYear) throws DatabaseOperationException {
        if (monthYear == null || monthYear.trim().isEmpty()) {
            monthYear = DateTimeUtil.getCurrentMonthYear();
        }
        List<Budget> budgets = activeDao.findAllBudgets();
        List<Expense> allExpenses = activeDao.findAll();

        String targetMonth = monthYear;
        // Calculate spent amount for each budget in this month
        for (Budget b : budgets) {
            double spent = ExpenseAnalytics.computeCategoryMonthlySpend(allExpenses, b.getCategory(), targetMonth);
            b.setSpent(spent);
        }
        return budgets;
    }

    public void saveBudget(Budget budget) throws InvalidExpenseException, DatabaseOperationException {
        if (budget.getCategory() == null) {
            throw new InvalidExpenseException("Budget category is required.");
        }
        if (budget.getMonthlyLimit() < 0) {
            throw new InvalidExpenseException("Budget limit cannot be negative.");
        }
        if (budget.getMonthYear() == null || budget.getMonthYear().trim().isEmpty()) {
            budget.setMonthYear(DateTimeUtil.getCurrentMonthYear());
        }
        activeDao.saveBudget(budget);
    }

    public boolean deleteBudget(int id) throws DatabaseOperationException {
        return activeDao.deleteBudget(id);
    }

    // =========================================================================
    //  CSV EXPORT GENERATOR
    // =========================================================================

    public String generateCsvExport() throws DatabaseOperationException {
        List<Expense> expenses = activeDao.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Title,Amount,Category,Payment Method,Date,Notes\n");

        for (Expense e : expenses) {
            sb.append(e.getId()).append(",");
            sb.append("\"").append(e.getTitle().replace("\"", "\"\"")).append("\",");
            sb.append(String.format(java.util.Locale.US, "%.2f", e.getAmount())).append(",");
            sb.append("\"").append(e.getCategory().getDisplayName()).append("\",");
            sb.append("\"").append(e.getPaymentMethod().getDisplayName()).append("\",");
            sb.append(DateTimeUtil.formatDate(e.getDate())).append(",");
            sb.append("\"").append((e.getNotes() != null ? e.getNotes().replace("\"", "\"\"") : "")).append("\"\n");
        }
        return sb.toString();
    }

    // =========================================================================
    //  DEMO DATA SEEDING
    // =========================================================================

    public void reloadDemoData() throws DatabaseOperationException {
        if (!usingDatabase) {
            inMemoryFallback.clearAndReseed();
        } else {
            // Seed a fresh batch into MySQL
            LocalDate today = LocalDate.now();
            activeDao.add(new Expense("Whole Foods Groceries", 142.50, Category.FOOD, PaymentMethod.CREDIT_CARD, today.minusDays(1), "Weekly organic grocery run"));
            activeDao.add(new Expense("Apartment Rent", 1250.00, Category.HOUSING, PaymentMethod.NET_BANKING, today.minusDays(5), "Monthly rent payment"));
            activeDao.add(new Expense("Metro Card Recharge", 55.00, Category.TRANSPORTATION, PaymentMethod.DEBIT_CARD, today.minusDays(3), "Monthly transit pass"));
            activeDao.add(new Expense("Electric & Gas Bill", 94.20, Category.UTILITIES, PaymentMethod.UPI, today.minusDays(4), "City power utility bill"));
            activeDao.add(new Expense("Cinema Tickets & Popcorn", 38.50, Category.ENTERTAINMENT, PaymentMethod.UPI, today.minusDays(2), "Weekend movie outing"));
            activeDao.add(new Expense("Pharmacy Prescription", 45.00, Category.HEALTHCARE, PaymentMethod.CASH, today.minusDays(6), "Vitamins and allergy medicine"));
            activeDao.add(new Expense("Running Shoes", 89.99, Category.SHOPPING, PaymentMethod.CREDIT_CARD, today.minusDays(7), "Spring fitness gear"));
        }
    }
}

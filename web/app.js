/**
 * ==============================================================================
 * Personal Income & Expense Tracker - Vanilla JavaScript Frontend
 * Demonstrates:
 *  - Modern JavaScript Fetch API (async/await, HTTP GET/POST/PUT/DELETE)
 *  - RESTful API consumption and JSON serialization
 *  - Dynamic form category cascading based on transaction type (INCOME / EXPENSE)
 *  - Pure HTML5 Canvas charting (Zero third-party chart libraries)
 *  - Real-time debouncing, input validation, and toast notifications
 *  - Indian Rupee (₹) currency formatting
 * ==============================================================================
 */

// Application State
const state = {
    categories: [],
    transactions: [],
    dashboard: null,
    monthly: null,
    selectedMonth: new Date().toISOString().substring(0, 7), // "YYYY-MM"
    deleteTarget: null,
    searchDebounceTimer: null,
    isDemoMode: false
};

// Vibrant, accessible category colors for badges and charts
const CATEGORY_COLORS = {
    Salary: '#059669',
    Freelance: '#10b981',
    Bonus: '#14b8a6',
    Investment: '#0ea5e9',
    'Other Income': '#6366f1',
    Food: '#f59e0b',
    Travel: '#3b82f6',
    Shopping: '#8b5cf6',
    Bills: '#ef4444',
    Entertainment: '#ec4899',
    Education: '#06b6d4',
    Health: '#f43f5e',
    Rent: '#64748b',
    'Other Expense': '#94a3b8'
};

// =============================================================================
//  IN-BROWSER DEMO ENGINE (Used on Vercel or when backend is offline)
// =============================================================================
const DemoEngine = {
    STORAGE_KEY: 'EXPENSE_TRACKER_DEMO_DATA_V1',

    DEFAULT_CATEGORIES: [
        { categoryId: 1, categoryName: 'Salary', categoryType: 'INCOME', icon: '💼' },
        { categoryId: 2, categoryName: 'Freelance', categoryType: 'INCOME', icon: '💻' },
        { categoryId: 3, categoryName: 'Bonus', categoryType: 'INCOME', icon: '🎁' },
        { categoryId: 4, categoryName: 'Investment', categoryType: 'INCOME', icon: '📈' },
        { categoryId: 5, categoryName: 'Other Income', categoryType: 'INCOME', icon: '💵' },
        { categoryId: 6, categoryName: 'Food & Dining', categoryType: 'EXPENSE', icon: '🍔' },
        { categoryId: 7, categoryName: 'Transportation', categoryType: 'EXPENSE', icon: '🚗' },
        { categoryId: 8, categoryName: 'Housing & Rent', categoryType: 'EXPENSE', icon: '🏠' },
        { categoryId: 9, categoryName: 'Utilities', categoryType: 'EXPENSE', icon: '💡' },
        { categoryId: 10, categoryName: 'Entertainment', categoryType: 'EXPENSE', icon: '🎬' },
        { categoryId: 11, categoryName: 'Education', categoryType: 'EXPENSE', icon: '📚' },
        { categoryId: 12, categoryName: 'Health & Medical', categoryType: 'EXPENSE', icon: '🏥' },
        { categoryId: 13, categoryName: 'Other Expense', categoryType: 'EXPENSE', icon: '📦' }
    ],

    DEFAULT_TRANSACTIONS: [
        {
            transactionId: 101,
            userId: 1,
            categoryId: 1,
            categoryName: 'Salary',
            categoryIcon: '💼',
            transactionType: 'INCOME',
            amount: 65000.00,
            description: 'Tech Corp Monthly Salary',
            paymentMethod: 'UPI',
            transactionDate: '2026-03-01',
            displayDate: 'Mar 01, 2026',
            notes: 'Credited directly to savings account'
        },
        {
            transactionId: 102,
            userId: 1,
            categoryId: 8,
            categoryName: 'Housing & Rent',
            categoryIcon: '🏠',
            transactionType: 'EXPENSE',
            amount: 14000.00,
            description: 'Monthly Apartment Rent',
            paymentMethod: 'Net Banking',
            transactionDate: '2026-03-02',
            displayDate: 'Mar 02, 2026',
            notes: 'Transferred to landlord'
        },
        {
            transactionId: 103,
            userId: 1,
            categoryId: 6,
            categoryName: 'Food & Dining',
            categoryIcon: '🍔',
            transactionType: 'EXPENSE',
            amount: 3850.00,
            description: 'Grocery & Supermarket Restock',
            paymentMethod: 'Credit Card',
            transactionDate: '2026-03-05',
            displayDate: 'Mar 05, 2026',
            notes: 'Weekly pantry supplies, fruits, and veggies'
        },
        {
            transactionId: 104,
            userId: 1,
            categoryId: 9,
            categoryName: 'Utilities',
            categoryIcon: '💡',
            transactionType: 'EXPENSE',
            amount: 1850.00,
            description: 'Electricity & Water Utility Bill',
            paymentMethod: 'UPI',
            transactionDate: '2026-03-10',
            displayDate: 'Mar 10, 2026',
            notes: 'State electricity board payment'
        },
        {
            transactionId: 105,
            userId: 1,
            categoryId: 2,
            categoryName: 'Freelance',
            categoryIcon: '💻',
            transactionType: 'INCOME',
            amount: 15000.00,
            description: 'Freelance Web Design Project',
            paymentMethod: 'Net Banking',
            transactionDate: '2026-03-12',
            displayDate: 'Mar 12, 2026',
            notes: 'Milestone 2 payment received'
        },
        {
            transactionId: 106,
            userId: 1,
            categoryId: 6,
            categoryName: 'Food & Dining',
            categoryIcon: '🍔',
            transactionType: 'EXPENSE',
            amount: 1200.00,
            description: 'Weekend Dining & Cafe',
            paymentMethod: 'UPI',
            transactionDate: '2026-03-15',
            displayDate: 'Mar 15, 2026',
            notes: 'Dinner with colleagues'
        },
        {
            transactionId: 107,
            userId: 1,
            categoryId: 12,
            categoryName: 'Health & Medical',
            categoryIcon: '🏥',
            transactionType: 'EXPENSE',
            amount: 6500.00,
            description: 'Annual Health Insurance Premium',
            paymentMethod: 'Credit Card',
            transactionDate: '2026-03-18',
            displayDate: 'Mar 18, 2026',
            notes: 'Individual health cover renewal'
        },
        {
            transactionId: 108,
            userId: 1,
            categoryId: 7,
            categoryName: 'Transportation',
            categoryIcon: '🚗',
            transactionType: 'EXPENSE',
            amount: 1100.00,
            description: 'Fuel & Metro Smart Card Recharge',
            paymentMethod: 'Cash',
            transactionDate: '2026-03-20',
            displayDate: 'Mar 20, 2026',
            notes: 'Monthly city commute recharge'
        }
    ],

    DEFAULT_BUDGETS: {
        '2026-03': 35000.00
    },

    loadData() {
        try {
            const raw = localStorage.getItem(this.STORAGE_KEY);
            if (raw) return JSON.parse(raw);
        } catch (e) {
            console.warn('LocalStorage unavailable, using memory store');
        }
        const initial = {
            transactions: [...this.DEFAULT_TRANSACTIONS],
            budgets: { ...this.DEFAULT_BUDGETS }
        };
        this.saveData(initial);
        return initial;
    },

    saveData(data) {
        try {
            localStorage.setItem(this.STORAGE_KEY, JSON.stringify(data));
        } catch (e) {
            console.warn('Could not persist to LocalStorage', e);
        }
    },

    getCategories(type) {
        if (!type || type === 'ALL') return this.DEFAULT_CATEGORIES;
        return this.DEFAULT_CATEGORIES.filter(c => c.categoryType === type);
    },

    getTransactions(filters = {}) {
        const data = this.loadData();
        let list = [...data.transactions];

        if (filters.type && filters.type !== 'ALL') {
            list = list.filter(t => t.transactionType === filters.type);
        }
        if (filters.category && filters.category !== 'ALL') {
            const catId = parseInt(filters.category, 10);
            list = list.filter(t => t.categoryId === catId);
        }
        if (filters.startDate) {
            list = list.filter(t => t.transactionDate >= filters.startDate);
        }
        if (filters.endDate) {
            list = list.filter(t => t.transactionDate <= filters.endDate);
        }
        if (filters.search) {
            const q = filters.search.toLowerCase();
            list = list.filter(t =>
                (t.description && t.description.toLowerCase().includes(q)) ||
                (t.notes && t.notes.toLowerCase().includes(q)) ||
                (t.paymentMethod && t.paymentMethod.toLowerCase().includes(q)) ||
                (t.categoryName && t.categoryName.toLowerCase().includes(q))
            );
        }

        const sort = filters.sort || 'newest';
        list.sort((a, b) => {
            if (sort === 'newest') return b.transactionDate.localeCompare(a.transactionDate);
            if (sort === 'oldest') return a.transactionDate.localeCompare(b.transactionDate);
            if (sort === 'amount-desc') return b.amount - a.amount;
            if (sort === 'amount-asc') return a.amount - b.amount;
            return 0;
        });

        return list;
    },

    addTransaction(tx) {
        const data = this.loadData();
        const cat = this.DEFAULT_CATEGORIES.find(c => c.categoryId === tx.categoryId) || {};
        const newTx = {
            transactionId: Date.now(),
            userId: 1,
            categoryId: tx.categoryId,
            categoryName: cat.categoryName || 'Other',
            categoryIcon: cat.icon || '🏷️',
            transactionType: tx.transactionType,
            amount: parseFloat(tx.amount),
            description: tx.description,
            paymentMethod: tx.paymentMethod || 'Cash',
            transactionDate: tx.transactionDate,
            displayDate: formatDateDisplay(tx.transactionDate),
            notes: tx.notes || ''
        };
        data.transactions.unshift(newTx);
        this.saveData(data);
        return newTx;
    },

    updateTransaction(id, tx) {
        const data = this.loadData();
        const idx = data.transactions.findIndex(t => t.transactionId === parseInt(id, 10));
        if (idx === -1) throw new Error('Transaction not found');
        const cat = this.DEFAULT_CATEGORIES.find(c => c.categoryId === tx.categoryId) || {};
        data.transactions[idx] = {
            ...data.transactions[idx],
            categoryId: tx.categoryId,
            categoryName: cat.categoryName || data.transactions[idx].categoryName,
            categoryIcon: cat.icon || data.transactions[idx].categoryIcon,
            transactionType: tx.transactionType,
            amount: parseFloat(tx.amount),
            description: tx.description,
            paymentMethod: tx.paymentMethod || 'Cash',
            transactionDate: tx.transactionDate,
            displayDate: formatDateDisplay(tx.transactionDate),
            notes: tx.notes || ''
        };
        this.saveData(data);
        return data.transactions[idx];
    },

    deleteTransaction(id) {
        const data = this.loadData();
        data.transactions = data.transactions.filter(t => t.transactionId !== parseInt(id, 10));
        this.saveData(data);
    },

    getDashboard() {
        const data = this.loadData();
        let totalIncome = 0;
        let totalExpense = 0;

        data.transactions.forEach(t => {
            if (t.transactionType === 'INCOME') totalIncome += t.amount;
            else totalExpense += t.amount;
        });

        const currentMonth = new Date().toISOString().substring(0, 7);
        const monthlyBudget = data.budgets[currentMonth] || 35000.00;
        const budgetRemaining = monthlyBudget - totalExpense;
        const budgetPercentage = monthlyBudget > 0 ? (totalExpense / monthlyBudget) * 100 : 0;

        const recent = [...data.transactions]
            .sort((a, b) => b.transactionDate.localeCompare(a.transactionDate))
            .slice(0, 5);

        return {
            balance: totalIncome - totalExpense,
            totalIncome: totalIncome,
            totalExpense: totalExpense,
            monthlyBudget: monthlyBudget,
            budgetRemaining: budgetRemaining,
            budgetPercentage: budgetPercentage,
            recentTransactions: recent
        };
    },

    getMonthlyReport(year, month) {
        const data = this.loadData();
        const monthKey = `${year}-${String(month).padStart(2, '0')}`;
        const monthTxs = data.transactions.filter(t => t.transactionDate.startsWith(monthKey));

        let income = 0;
        let expenses = 0;
        let highestExp = null;
        const catMap = {};

        monthTxs.forEach(t => {
            if (t.transactionType === 'INCOME') {
                income += t.amount;
            } else {
                expenses += t.amount;
                if (!highestExp || t.amount > highestExp.amount) {
                    highestExp = t;
                }
                catMap[t.categoryName] = (catMap[t.categoryName] || 0) + t.amount;
            }
        });

        let highestCat = null;
        let highestCatAmount = 0;
        for (const [cat, amt] of Object.entries(catMap)) {
            if (amt > highestCatAmount) {
                highestCatAmount = amt;
                highestCat = cat;
            }
        }

        const budget = data.budgets[monthKey] || 35000.00;
        const monthNames = ['', 'January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

        return {
            monthName: `${monthNames[month] || monthKey} ${year}`,
            income: income,
            expenses: expenses,
            savings: income - expenses,
            transactionCount: monthTxs.length,
            highestExpense: highestExp,
            highestSpendingCategory: highestCat,
            highestSpendingCategoryAmount: highestCatAmount,
            budgetAmount: budget,
            remainingBudget: budget - expenses,
            budgetPercentageUsed: budget > 0 ? (expenses / budget) * 100 : 0
        };
    },

    getCategoryReport(year, month) {
        const data = this.loadData();
        const monthKey = `${year}-${String(month).padStart(2, '0')}`;
        const monthExpenses = data.transactions.filter(t => t.transactionDate.startsWith(monthKey) && t.transactionType === 'EXPENSE');

        const catMap = {};
        let total = 0;
        monthExpenses.forEach(t => {
            catMap[t.categoryName] = (catMap[t.categoryName] || 0) + t.amount;
            total += t.amount;
        });

        const list = Object.entries(catMap).map(([name, spent]) => {
            const cat = this.DEFAULT_CATEGORIES.find(c => c.categoryName === name) || {};
            return {
                categoryId: cat.categoryId || 0,
                categoryName: name,
                categoryType: 'EXPENSE',
                totalAmount: spent,
                transactionCount: 1,
                percentage: total > 0 ? parseFloat(((spent / total) * 100).toFixed(1)) : 0,
                icon: cat.icon || '🏷️'
            };
        });
        list.sort((a, b) => b.totalAmount - a.totalAmount);
        return list;
    },

    getTopExpenses(limit = 5) {
        const data = this.loadData();
        const expenses = data.transactions.filter(t => t.transactionType === 'EXPENSE');
        expenses.sort((a, b) => b.amount - a.amount);
        return expenses.slice(0, limit);
    },

    setBudget(month, year, amount) {
        const data = this.loadData();
        const monthKey = `${year}-${String(month).padStart(2, '0')}`;
        data.budgets[monthKey] = parseFloat(amount);
        this.saveData(data);
    },

    exportCsv() {
        const data = this.loadData();
        const headers = ['Transaction ID', 'Type', 'Category', 'Amount', 'Date', 'Payment Method', 'Description', 'Notes'];
        const rows = data.transactions.map(t => [
            t.transactionId,
            t.transactionType,
            `"${(t.categoryName || '').replace(/"/g, '""')}"`,
            t.amount.toFixed(2),
            t.transactionDate,
            `"${(t.paymentMethod || '').replace(/"/g, '""')}"`,
            `"${(t.description || '').replace(/"/g, '""')}"`,
            `"${(t.notes || '').replace(/"/g, '""')}"`
        ]);

        const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.setAttribute('href', url);
        link.setAttribute('download', `transactions_export_${new Date().toISOString().split('T')[0]}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
};

// =============================================================================
//  INITIALIZATION
// =============================================================================

document.addEventListener('DOMContentLoaded', async () => {
    initializeDateDefaults();
    bindEventListeners();

    // Check system status and load initial dataset
    await checkSystemStatus();
    await loadCategories();
    await refreshAllData();
});

function initializeDateDefaults() {
    const today = new Date().toISOString().split('T')[0];
    const formDate = document.getElementById('formDate');
    if (formDate) formDate.value = today;

    const summaryPicker = document.getElementById('summaryMonthPicker');
    if (summaryPicker) summaryPicker.value = state.selectedMonth;

    const budgetMonthInput = document.getElementById('budgetMonthInput');
    if (budgetMonthInput) budgetMonthInput.value = state.selectedMonth;
}

// =============================================================================
//  FETCH API REST CLIENT CALLS
// =============================================================================

/**
 * Checks server health and database connection status.
 */
async function checkSystemStatus() {
    const storageLabel = document.getElementById('storageLabel');
    const storageBadge = document.getElementById('storageBadge');
    const dot = storageBadge ? storageBadge.querySelector('.status-dot') : null;

    try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 2000);
        const res = await fetch('/api/status', { signal: controller.signal });
        clearTimeout(timeoutId);

        if (res.ok) {
            const data = await res.json();
            const info = data.data;
            state.isDemoMode = false;

            if (storageLabel) storageLabel.textContent = info.storage || 'Connected';
            if (dot) {
                if (info.databaseAvailable || (info.storage && info.storage.includes('Memory'))) {
                    dot.className = 'status-dot';
                } else {
                    dot.className = 'status-dot status-dot-offline';
                }
            }
            hideBackendError();
            return;
        }
    } catch (err) {
        // Backend offline, timed out, or 404 (e.g. running on Vercel)
    }

    // Backend is unreachable: activate in-browser DemoEngine
    state.isDemoMode = true;
    if (storageLabel) storageLabel.textContent = 'Live Demo (Vercel)';
    if (dot) {
        dot.className = 'status-dot status-dot-demo';
    }
    hideBackendError();
}

/**
 * Loads categories from /api/categories or DemoEngine.
 */
async function loadCategories() {
    if (state.isDemoMode) {
        state.categories = DemoEngine.getCategories();
        populateFilterCategories();
        updateModalCategories('EXPENSE');
        hideBackendError();
        return;
    }

    try {
        const res = await fetch('/api/categories');
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        state.categories = data.categories || [];
        populateFilterCategories();
        updateModalCategories('EXPENSE');
        hideBackendError();
    } catch (err) {
        console.error('Failed to load categories:', err);
        state.isDemoMode = true;
        state.categories = DemoEngine.getCategories();
        populateFilterCategories();
        updateModalCategories('EXPENSE');
        hideBackendError();
    }
}

/**
 * Loads primary dashboard metrics from /api/dashboard or DemoEngine.
 */
async function loadDashboard() {
    if (state.isDemoMode) {
        const data = DemoEngine.getDashboard();
        state.dashboard = data;
        renderDashboardKPIs(data);
        renderRecentTransactions(data.recentTransactions || []);
        hideBackendError();
        return;
    }

    try {
        const res = await fetch('/api/dashboard');
        if (!res.ok) {
            const errData = await res.json().catch(() => ({}));
            throw new Error(errData.error || `HTTP ${res.status}`);
        }
        const data = await res.json();
        state.dashboard = data;
        renderDashboardKPIs(data);
        renderRecentTransactions(data.recentTransactions || []);
        hideBackendError();
    } catch (err) {
        console.error('Failed to load dashboard:', err);
        state.isDemoMode = true;
        const data = DemoEngine.getDashboard();
        state.dashboard = data;
        renderDashboardKPIs(data);
        renderRecentTransactions(data.recentTransactions || []);
        hideBackendError();
    }
}

/**
 * Loads monthly financial summary for the currently selected month/year.
 */
async function loadMonthlyReport() {
    const [yearStr, monthStr] = state.selectedMonth.split('-');
    const year = parseInt(yearStr, 10);
    const month = parseInt(monthStr, 10);

    if (state.isDemoMode) {
        const data = DemoEngine.getMonthlyReport(year, month);
        state.monthly = data;
        renderMonthlySummary(data);
        return;
    }

    try {
        const res = await fetch(`/api/reports/monthly?month=${month}&year=${year}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        state.monthly = data;
        renderMonthlySummary(data);
    } catch (err) {
        console.error('Failed to load monthly report:', err);
        const data = DemoEngine.getMonthlyReport(year, month);
        state.monthly = data;
        renderMonthlySummary(data);
    }
}

/**
 * Loads expense category breakdown report for canvas chart.
 */
async function loadCategoryReport() {
    const [yearStr, monthStr] = state.selectedMonth.split('-');
    const year = parseInt(yearStr, 10);
    const month = parseInt(monthStr, 10);

    if (state.isDemoMode) {
        const categories = DemoEngine.getCategoryReport(year, month);
        renderDonutChart(categories);
        return;
    }

    try {
        const res = await fetch(`/api/reports/categories?month=${month}&year=${year}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        renderDonutChart(data.categories || []);
    } catch (err) {
        console.error('Failed to load category report:', err);
        const categories = DemoEngine.getCategoryReport(year, month);
        renderDonutChart(categories);
    }
}

/**
 * Loads Top 5 highest single expenses.
 */
async function loadTopExpenses() {
    if (state.isDemoMode) {
        const top = DemoEngine.getTopExpenses(5);
        renderTopExpenses(top);
        return;
    }

    try {
        const res = await fetch('/api/reports/top-expenses?limit=5');
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        renderTopExpenses(data.topExpenses || []);
    } catch (err) {
        console.error('Failed to load top expenses:', err);
        renderTopExpenses(DemoEngine.getTopExpenses(5));
    }
}

/**
 * Loads filtered and sorted transactions from /api/transactions.
 */
async function loadTransactions() {
    const search = document.getElementById('filterSearch').value.trim();
    const type = document.getElementById('filterType').value;
    const category = document.getElementById('filterCategory').value;
    const datePreset = document.getElementById('filterDatePreset').value;
    const sort = document.getElementById('filterSort').value;

    let startDate = '';
    let endDate = '';

    const now = new Date();
    const todayStr = now.toISOString().split('T')[0];

    if (datePreset === 'TODAY') {
        startDate = todayStr;
        endDate = todayStr;
    } else if (datePreset === 'THIS_MONTH') {
        const y = now.getFullYear();
        const m = String(now.getMonth() + 1).padStart(2, '0');
        startDate = `${y}-${m}-01`;
        const lastDay = new Date(y, now.getMonth() + 1, 0).getDate();
        endDate = `${y}-${m}-${lastDay}`;
    } else if (datePreset === 'LAST_MONTH') {
        const prev = new Date(now.getFullYear(), now.getMonth() - 1, 1);
        const y = prev.getFullYear();
        const m = String(prev.getMonth() + 1).padStart(2, '0');
        startDate = `${y}-${m}-01`;
        const lastDay = new Date(y, prev.getMonth() + 1, 0).getDate();
        endDate = `${y}-${m}-${lastDay}`;
    } else if (datePreset === 'CUSTOM') {
        startDate = document.getElementById('filterStartDate').value;
        endDate = document.getElementById('filterEndDate').value;
    }

    if (state.isDemoMode) {
        state.transactions = DemoEngine.getTransactions({
            search,
            type,
            category,
            startDate,
            endDate,
            sort
        });
        renderTransactionsTable(state.transactions);
        hideBackendError();
        return;
    }

    const params = new URLSearchParams();
    if (type && type !== 'ALL') params.append('type', type);
    if (category && category !== 'ALL') params.append('category', category);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    if (search) params.append('search', search);
    if (sort) params.append('sort', sort);

    try {
        const res = await fetch(`/api/transactions?${params.toString()}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        state.transactions = data.transactions || [];
        renderTransactionsTable(state.transactions);
        hideBackendError();
    } catch (err) {
        console.error('Failed to load transactions:', err);
        state.transactions = DemoEngine.getTransactions({
            search,
            type,
            category,
            startDate,
            endDate,
            sort
        });
        renderTransactionsTable(state.transactions);
        hideBackendError();
    }
}

/**
 * Refreshes all application data in parallel.
 */
async function refreshAllData() {
    await Promise.all([
        loadDashboard(),
        loadMonthlyReport(),
        loadCategoryReport(),
        loadTopExpenses(),
        loadTransactions()
    ]);
}

// =============================================================================
//  DOM RENDERING: DASHBOARD KPIS (4 Cards)
// =============================================================================

function renderDashboardKPIs(data) {
    if (!data) return;

    // Current Balance = Total Income - Total Expenses
    const balanceEl = document.getElementById('kpiBalance');
    const balanceSub = document.getElementById('kpiBalanceSub');
    const bal = data.balance || 0;

    balanceEl.textContent = formatCurrency(Math.abs(bal));
    if (bal >= 0) {
        balanceEl.className = 'metric-value text-income';
        balanceSub.textContent = 'Net positive financial reserve';
    } else {
        balanceEl.className = 'metric-value text-expense';
        balanceSub.textContent = 'Net deficit (Expenses exceed Income)';
    }

    // Total Income
    const incomeEl = document.getElementById('kpiTotalIncome');
    incomeEl.textContent = '+ ' + formatCurrency(data.totalIncome || 0);

    // Total Expenses
    const expenseEl = document.getElementById('kpiTotalExpense');
    expenseEl.textContent = '- ' + formatCurrency(data.totalExpense || 0);

    // Monthly Budget
    const budgetEl = document.getElementById('kpiMonthlyBudget');
    const budgetFooter = document.getElementById('kpiBudgetFooter');
    const monthlyBudget = data.monthlyBudget || 0;
    const budgetRemaining = data.budgetRemaining || 0;
    const budgetPct = data.budgetPercentage || 0;

    budgetEl.textContent = formatCurrency(monthlyBudget);
    if (monthlyBudget > 0) {
        if (budgetRemaining < 0) {
            budgetFooter.textContent = `Budget exceeded by ${formatCurrency(Math.abs(budgetRemaining))}!`;
            budgetFooter.style.color = 'var(--expense)';
        } else {
            budgetFooter.textContent = `${budgetPct.toFixed(0)}% used • ${formatCurrency(budgetRemaining)} remaining`;
            budgetFooter.style.color = 'var(--text-muted)';
        }
    } else {
        budgetFooter.textContent = 'No budget configured for this month';
    }
}

// =============================================================================
//  DOM RENDERING: MONTHLY SUMMARY & BUDGET PROGRESS
// =============================================================================

function renderMonthlySummary(summary) {
    if (!summary) return;

    document.getElementById('monthlySummaryHeading').textContent = `Monthly Financial Summary (${summary.monthName || state.selectedMonth})`;
    document.getElementById('monthIncomeVal').textContent = '+ ' + formatCurrency(summary.income || 0);
    document.getElementById('monthExpenseVal').textContent = '- ' + formatCurrency(summary.expenses || 0);

    const savingsVal = document.getElementById('monthSavingsVal');
    const savings = summary.savings || 0;
    savingsVal.textContent = (savings >= 0 ? '+ ' : '- ') + formatCurrency(Math.abs(savings));
    savingsVal.className = 'stat-value ' + (savings >= 0 ? 'text-income' : 'text-expense');

    document.getElementById('monthTxCountVal').textContent = summary.transactionCount || 0;

    const highestExpEl = document.getElementById('monthHighestExpenseVal');
    if (summary.highestExpense) {
        highestExpEl.textContent = `${formatCurrency(summary.highestExpense.amount)} (${summary.highestExpense.description})`;
    } else {
        highestExpEl.textContent = 'None';
    }

    const topCatEl = document.getElementById('monthTopCategoryVal');
    if (summary.highestSpendingCategory) {
        topCatEl.textContent = `${summary.highestSpendingCategory} (${formatCurrency(summary.highestSpendingCategoryAmount || 0)})`;
    } else {
        topCatEl.textContent = 'None';
    }

    // Budget Tracker Progress Bar
    const budgetAmount = summary.budgetAmount || 0;
    const expenses = summary.expenses || 0;
    const remaining = summary.remainingBudget || 0;
    const pct = summary.budgetPercentageUsed || 0;

    const progressLabel = document.getElementById('budgetProgressLabel');
    const progressBar = document.getElementById('budgetProgressBar');
    const remainingNote = document.getElementById('budgetRemainingNote');

    if (budgetAmount > 0) {
        progressLabel.textContent = `${formatCurrency(expenses)} spent of ${formatCurrency(budgetAmount)} (${pct.toFixed(1)}%)`;

        let progressClass = 'progress-normal';
        if (pct > 100) {
            progressClass = 'progress-exceeded';
            remainingNote.textContent = `⚠️ Budget exceeded by ${formatCurrency(Math.abs(remaining))}`;
            remainingNote.style.color = 'var(--expense)';
            remainingNote.style.fontWeight = '600';
        } else if (pct >= 90) {
            progressClass = 'progress-warning';
            remainingNote.textContent = `High utilization: ${formatCurrency(remaining)} remaining`;
            remainingNote.style.color = 'var(--warning)';
        } else if (pct >= 70) {
            progressClass = 'progress-warning';
            remainingNote.textContent = `Warning: ${formatCurrency(remaining)} remaining`;
            remainingNote.style.color = 'var(--warning)';
        } else {
            progressClass = 'progress-normal';
            remainingNote.textContent = `Remaining budget: ${formatCurrency(remaining)}`;
            remainingNote.style.color = 'var(--text-muted)';
        }

        progressBar.className = `budget-progress-bar ${progressClass}`;
        progressBar.style.width = `${Math.min(pct, 100)}%`;
    } else {
        progressLabel.textContent = `${formatCurrency(expenses)} spent (No budget set)`;
        progressBar.style.width = '0%';
        remainingNote.textContent = 'Click "Budget" in the header to set an expense limit for this month.';
        remainingNote.style.color = 'var(--text-muted)';
    }
}

// =============================================================================
//  DOM RENDERING: TOP 5 EXPENSES & RECENT TRANSACTIONS
// =============================================================================

function renderTopExpenses(topExpenses) {
    const container = document.getElementById('topExpensesList');
    if (!topExpenses || topExpenses.length === 0) {
        container.innerHTML = '<p class="text-muted text-center py-4 text-sm">No expenses recorded yet.</p>';
        return;
    }

    let html = '';
    topExpenses.forEach((t, idx) => {
        html += `
            <div class="list-item-row">
                <div class="item-left">
                    <span style="font-weight: 700; color: var(--text-muted); font-size: 0.8rem; width: 18px;">#${idx + 1}</span>
                    <div>
                        <div class="item-title">${escapeHtml(t.description)}</div>
                        <div class="item-sub">${escapeHtml(t.categoryName || 'Expense')} • ${t.transactionDate}</div>
                    </div>
                </div>
                <span class="item-amount text-expense">- ${formatCurrency(t.amount)}</span>
            </div>
        `;
    });
    container.innerHTML = html;
}

function renderRecentTransactions(recent) {
    const container = document.getElementById('recentTransactionsList');
    if (!recent || recent.length === 0) {
        container.innerHTML = '<p class="text-muted text-center py-4 text-sm">No recent transactions.</p>';
        return;
    }

    let html = '';
    recent.forEach(t => {
        const isInc = t.transactionType === 'INCOME';
        const colorClass = isInc ? 'text-income' : 'text-expense';
        const sign = isInc ? '+ ' : '- ';

        html += `
            <div class="list-item-row">
                <div class="item-left">
                    <span style="font-size: 1.1rem;">${t.categoryIcon || (isInc ? '💼' : '🛍️')}</span>
                    <div>
                        <div class="item-title">${escapeHtml(t.description)}</div>
                        <div class="item-sub">${escapeHtml(t.categoryName || t.transactionType)} • ${t.transactionDate}</div>
                    </div>
                </div>
                <span class="item-amount ${colorClass}">${sign}${formatCurrency(t.amount)}</span>
            </div>
        `;
    });
    container.innerHTML = html;
}

// =============================================================================
//  DOM RENDERING: TRANSACTIONS TABLE
// =============================================================================

function renderTransactionsTable(transactions) {
    const tbody = document.getElementById('transactionsTableBody');
    const emptyState = document.getElementById('emptyState');
    const badge = document.getElementById('tableRecordCountBadge');

    if (badge) {
        badge.textContent = `${transactions.length} record${transactions.length === 1 ? '' : 's'}`;
    }

    if (!transactions || transactions.length === 0) {
        tbody.innerHTML = '';
        emptyState.classList.remove('hidden');
        return;
    }

    emptyState.classList.add('hidden');
    let html = '';

    transactions.forEach(t => {
        const isInc = t.transactionType === 'INCOME';
        const typeBadge = isInc ? 'badge-income' : 'badge-expense';
        const amountClass = isInc ? 'text-income' : 'text-expense';
        const sign = isInc ? '+ ' : '- ';

        html += `
            <tr>
                <td>
                    <div style="font-weight: 500;">${t.displayDate || t.transactionDate}</div>
                    <div class="text-muted text-xs">${t.transactionDate}</div>
                </td>
                <td>
                    <div class="table-title">${escapeHtml(t.description)}</div>
                    ${t.notes ? `<div class="table-notes" title="${escapeHtml(t.notes)}">${escapeHtml(t.notes)}</div>` : ''}
                </td>
                <td>
                    <span class="badge badge-cat">
                        <span>${t.categoryIcon || '🏷️'}</span>
                        <span>${escapeHtml(t.categoryName || 'General')}</span>
                    </span>
                </td>
                <td style="text-align: center;">
                    <span class="badge badge-type ${typeBadge}">${t.transactionType}</span>
                </td>
                <td>
                    <span class="badge badge-pm">${escapeHtml(t.paymentMethod || 'Cash')}</span>
                </td>
                <td style="text-align: right;">
                    <span class="table-amount ${amountClass}">${sign}${formatCurrency(t.amount)}</span>
                </td>
                <td style="text-align: center;">
                    <div class="table-actions">
                        <button class="action-btn action-btn-edit" onclick="openEditModal(${t.transactionId})" title="Edit Transaction">✏️</button>
                        <button class="action-btn action-btn-delete" onclick="openDeleteModal(${t.transactionId})" title="Delete Transaction">🗑️</button>
                    </div>
                </td>
            </tr>
        `;
    });

    tbody.innerHTML = html;
}

// =============================================================================
//  CANVAS DONUT CHART: EXPENSE BY CATEGORY
// =============================================================================

function renderDonutChart(categories) {
    const canvas = document.getElementById('categoryDonutChart');
    const legend = document.getElementById('chartLegend');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    ctx.clearRect(0, 0, width, height);

    if (!categories || categories.length === 0) {
        ctx.beginPath();
        ctx.arc(width / 2, height / 2, 60, 0, 2 * Math.PI);
        ctx.strokeStyle = '#e2e8f0';
        ctx.lineWidth = 20;
        ctx.stroke();

        ctx.fillStyle = '#94a3b8';
        ctx.font = '13px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('No Expenses', width / 2, height / 2);

        legend.innerHTML = '<span class="text-muted text-xs">No expense data for this month</span>';
        return;
    }

    const totalExpense = categories.reduce((sum, c) => sum + c.totalAmount, 0);
    const centerX = width / 2;
    const centerY = height / 2;
    const radius = 65;
    const lineWidth = 22;

    let startAngle = -0.5 * Math.PI;
    let legendHtml = '';

    categories.forEach(cat => {
        const sliceAngle = (cat.totalAmount / totalExpense) * (2 * Math.PI);
        const color = CATEGORY_COLORS[cat.categoryName] || '#64748b';

        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, startAngle, startAngle + sliceAngle);
        ctx.strokeStyle = color;
        ctx.lineWidth = lineWidth;
        ctx.stroke();

        startAngle += sliceAngle;

        legendHtml += `
            <div class="legend-item" title="${cat.categoryName}: ${formatCurrency(cat.totalAmount)} (${cat.percentage}%)">
                <span class="legend-color-dot" style="background-color: ${color};"></span>
                <span class="legend-label">${cat.icon || '🏷️'} ${escapeHtml(cat.categoryName)}</span>
                <span class="legend-value">${cat.percentage}%</span>
            </div>
        `;
    });

    ctx.fillStyle = '#0f172a';
    ctx.font = 'bold 13px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(formatCurrency(totalExpense), centerX, centerY - 6);

    ctx.fillStyle = '#64748b';
    ctx.font = '10px sans-serif';
    ctx.fillText('Expenses', centerX, centerY + 10);

    legend.innerHTML = legendHtml;
}

// =============================================================================
//  MODAL: CASCADING CATEGORY SELECTION & FORMS
// =============================================================================

function populateFilterCategories() {
    const filterCat = document.getElementById('filterCategory');
    if (!filterCat) return;

    let options = '<option value="ALL">All Categories</option>';
    state.categories.forEach(c => {
        options += `<option value="${c.categoryId}">${c.icon || '🏷️'} ${escapeHtml(c.categoryName)} (${c.categoryType})</option>`;
    });
    filterCat.innerHTML = options;
}

/**
 * Dynamically updates the Category select in Add/Edit modal based on selected type (INCOME or EXPENSE).
 */
function updateModalCategories(selectedType) {
    const formCat = document.getElementById('formCategory');
    const hint = document.getElementById('categoryTypeHint');
    if (!formCat) return;

    const filtered = state.categories.filter(c => c.categoryType === selectedType);
    let options = '';
    filtered.forEach(c => {
        options += `<option value="${c.categoryId}">${c.icon || '🏷️'} ${escapeHtml(c.categoryName)}</option>`;
    });
    formCat.innerHTML = options;

    if (hint) {
        hint.textContent = selectedType === 'INCOME' ? 'Showing income categories' : 'Showing expense categories';
    }
}

function openAddModal() {
    document.getElementById('modalTitle').textContent = 'Add Transaction';
    document.getElementById('txId').value = '';
    document.getElementById('transactionForm').reset();
    initializeDateDefaults();

    document.getElementById('typeRadioExpense').checked = true;
    updateModalCategories('EXPENSE');

    document.getElementById('formErrorBanner').classList.add('hidden');
    document.getElementById('transactionModal').classList.remove('hidden');
    document.getElementById('formAmount').focus();
}

function openEditModal(id) {
    const tx = state.transactions.find(t => t.transactionId === id);
    if (!tx) return;

    document.getElementById('modalTitle').textContent = `Edit Transaction #${tx.transactionId}`;
    document.getElementById('txId').value = tx.transactionId;

    const isInc = tx.transactionType === 'INCOME';
    if (isInc) {
        document.getElementById('typeRadioIncome').checked = true;
        updateModalCategories('INCOME');
    } else {
        document.getElementById('typeRadioExpense').checked = true;
        updateModalCategories('EXPENSE');
    }

    document.getElementById('formAmount').value = tx.amount;
    document.getElementById('formDate').value = tx.transactionDate;
    document.getElementById('formCategory').value = tx.categoryId;
    document.getElementById('formDescription').value = tx.description;
    document.getElementById('formPaymentMethod').value = tx.paymentMethod || 'Cash';
    document.getElementById('formNotes').value = tx.notes || '';
    document.getElementById('formErrorBanner').classList.add('hidden');

    document.getElementById('transactionModal').classList.remove('hidden');
    document.getElementById('formDescription').focus();
}

function closeTransactionModal() {
    document.getElementById('transactionModal').classList.add('hidden');
}

function openBudgetModal() {
    document.getElementById('budgetForm').reset();
    document.getElementById('budgetMonthInput').value = state.selectedMonth;
    document.getElementById('budgetErrorBanner').classList.add('hidden');
    document.getElementById('budgetModal').classList.remove('hidden');
}

function closeBudgetModal() {
    document.getElementById('budgetModal').classList.add('hidden');
}

function openDeleteModal(id) {
    const tx = state.transactions.find(t => t.transactionId === id);
    if (!tx) return;

    state.deleteTarget = tx;
    const preview = document.getElementById('deleteModalPreview');
    const isInc = tx.transactionType === 'INCOME';
    preview.innerHTML = `
        <div>${escapeHtml(tx.description)}</div>
        <div class="text-sm ${isInc ? 'text-income' : 'text-expense'}" style="margin-top: 0.2rem;">
            ${isInc ? '+ ' : '- '}${formatCurrency(tx.amount)} (${tx.transactionType})
        </div>
    `;
    document.getElementById('deleteModal').classList.remove('hidden');
}

function closeDeleteModal() {
    state.deleteTarget = null;
    document.getElementById('deleteModal').classList.add('hidden');
}

// =============================================================================
//  FORM SUBMISSIONS (POST & PUT via Fetch API)
// =============================================================================

async function handleTransactionSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('txId').value;
    const isEdit = Boolean(id);

    const typeRadio = document.querySelector('input[name="formTxType"]:checked');
    const transactionType = typeRadio ? typeRadio.value : 'EXPENSE';

    const payload = {
        amount: parseFloat(document.getElementById('formAmount').value),
        transactionType: transactionType,
        categoryId: parseInt(document.getElementById('formCategory').value),
        description: document.getElementById('formDescription').value.trim(),
        paymentMethod: document.getElementById('formPaymentMethod').value,
        transactionDate: document.getElementById('formDate').value,
        notes: document.getElementById('formNotes').value.trim()
    };

    const errBanner = document.getElementById('formErrorBanner');
    errBanner.classList.add('hidden');

    if (state.isDemoMode) {
        try {
            if (isEdit) {
                DemoEngine.updateTransaction(id, payload);
            } else {
                DemoEngine.addTransaction(payload);
            }
            closeTransactionModal();
            showToast(isEdit ? 'Transaction updated successfully.' : 'Transaction added successfully.', 'success');
            await refreshAllData();
            return;
        } catch (err) {
            errBanner.textContent = err.message;
            errBanner.classList.remove('hidden');
            return;
        }
    }

    try {
        const url = isEdit ? `/api/transactions/${id}` : '/api/transactions';
        const method = isEdit ? 'PUT' : 'POST';

        const res = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (!res.ok || !data.success) {
            throw new Error(data.error || 'Failed to save transaction');
        }

        closeTransactionModal();
        showToast(isEdit ? 'Transaction updated successfully.' : 'Transaction added successfully.', 'success');
        await refreshAllData();

    } catch (err) {
        errBanner.textContent = err.message;
        errBanner.classList.remove('hidden');
    }
}

async function handleBudgetSubmit(e) {
    e.preventDefault();
    const monthStr = document.getElementById('budgetMonthInput').value;
    const [year, month] = monthStr.split('-').map(Number);
    const amount = parseFloat(document.getElementById('budgetAmountInput').value);

    const payload = {
        month: month,
        year: year,
        budgetAmount: amount
    };

    const errBanner = document.getElementById('budgetErrorBanner');
    errBanner.classList.add('hidden');

    if (state.isDemoMode) {
        try {
            DemoEngine.setBudget(month, year, amount);
            closeBudgetModal();
            showToast('Monthly budget updated successfully.', 'success');
            await loadDashboard();
            await loadMonthlyReport();
            return;
        } catch (err) {
            errBanner.textContent = err.message;
            errBanner.classList.remove('hidden');
            return;
        }
    }

    try {
        const res = await fetch('/api/budget', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (!res.ok || !data.success) {
            throw new Error(data.error || 'Failed to save budget');
        }

        closeBudgetModal();
        showToast('Monthly budget updated successfully.', 'success');
        await loadDashboard();
        await loadMonthlyReport();

    } catch (err) {
        errBanner.textContent = err.message;
        errBanner.classList.remove('hidden');
    }
}

async function confirmDeleteTransaction() {
    if (!state.deleteTarget) return;
    const id = state.deleteTarget.transactionId;

    if (state.isDemoMode) {
        DemoEngine.deleteTransaction(id);
        closeDeleteModal();
        showToast('Transaction deleted.', 'success');
        await refreshAllData();
        return;
    }

    try {
        const res = await fetch(`/api/transactions/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok || !data.success) {
            throw new Error(data.error || 'Failed to delete transaction');
        }

        closeDeleteModal();
        showToast('Transaction deleted.', 'success');
        await refreshAllData();

    } catch (err) {
        closeDeleteModal();
        showToast('Delete error: ' + err.message, 'error');
    }
}

// =============================================================================
//  EVENT LISTENERS & FILTERING
// =============================================================================

function bindEventListeners() {
    // Transaction modal triggers
    document.getElementById('btnOpenAddModal').addEventListener('click', openAddModal);
    document.getElementById('btnModalClose').addEventListener('click', closeTransactionModal);
    document.getElementById('btnModalCancel').addEventListener('click', closeTransactionModal);
    document.getElementById('transactionForm').addEventListener('submit', handleTransactionSubmit);

    // Dynamic category switching in modal
    document.getElementById('typeRadioExpense').addEventListener('change', () => updateModalCategories('EXPENSE'));
    document.getElementById('typeRadioIncome').addEventListener('change', () => updateModalCategories('INCOME'));

    // Budget modal triggers
    document.getElementById('btnOpenBudgetModal').addEventListener('click', openBudgetModal);
    document.getElementById('btnBudgetModalClose').addEventListener('click', closeBudgetModal);
    document.getElementById('btnBudgetModalCancel').addEventListener('click', closeBudgetModal);
    document.getElementById('budgetForm').addEventListener('submit', handleBudgetSubmit);

    // Delete modal triggers
    document.getElementById('btnDeleteModalClose').addEventListener('click', closeDeleteModal);
    document.getElementById('btnCancelDelete').addEventListener('click', closeDeleteModal);
    document.getElementById('btnConfirmDelete').addEventListener('click', confirmDeleteTransaction);

    // CSV export
    document.getElementById('btnExportCsv').addEventListener('click', () => {
        if (state.isDemoMode) {
            DemoEngine.exportCsv();
        } else {
            window.location.href = '/api/export';
        }
    });

    // Retry connection button
    document.getElementById('btnRetryConnection').addEventListener('click', async () => {
        showToast('Checking connection...', 'info');
        await checkSystemStatus();
        await refreshAllData();
    });

    // Reset filters
    document.getElementById('btnResetFilters').addEventListener('click', resetFilters);

    // Month Selector in Summary
    document.getElementById('summaryMonthPicker').addEventListener('change', async (e) => {
        state.selectedMonth = e.target.value;
        await Promise.all([loadMonthlyReport(), loadCategoryReport()]);
    });

    // Search input debouncing (280ms)
    document.getElementById('filterSearch').addEventListener('input', () => {
        clearTimeout(state.searchDebounceTimer);
        state.searchDebounceTimer = setTimeout(loadTransactions, 280);
    });

    // Filters
    document.getElementById('filterType').addEventListener('change', loadTransactions);
    document.getElementById('filterCategory').addEventListener('change', loadTransactions);
    document.getElementById('filterSort').addEventListener('change', loadTransactions);

    // Date Preset Filter
    document.getElementById('filterDatePreset').addEventListener('change', (e) => {
        const val = e.target.value;
        const startWrap = document.getElementById('customDateStartWrapper');
        const endWrap = document.getElementById('customDateEndWrapper');
        if (val === 'CUSTOM') {
            startWrap.classList.remove('hidden');
            endWrap.classList.remove('hidden');
        } else {
            startWrap.classList.add('hidden');
            endWrap.classList.add('hidden');
            loadTransactions();
        }
    });

    document.getElementById('filterStartDate').addEventListener('change', loadTransactions);
    document.getElementById('filterEndDate').addEventListener('change', loadTransactions);

    // Escape key modal close
    window.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeTransactionModal();
            closeBudgetModal();
            closeDeleteModal();
        }
    });
}

function resetFilters() {
    document.getElementById('filterSearch').value = '';
    document.getElementById('filterType').value = 'ALL';
    document.getElementById('filterCategory').value = 'ALL';
    document.getElementById('filterDatePreset').value = 'ALL';
    document.getElementById('filterSort').value = 'newest';
    document.getElementById('customDateStartWrapper').classList.add('hidden');
    document.getElementById('customDateEndWrapper').classList.add('hidden');
    document.getElementById('filterStartDate').value = '';
    document.getElementById('filterEndDate').value = '';
    loadTransactions();
}

// =============================================================================
//  ERROR STATES & TOASTS
// =============================================================================

function showBackendError() {
    const el = document.getElementById('backendErrorState');
    if (el) el.classList.remove('hidden');
}

function hideBackendError() {
    const el = document.getElementById('backendErrorState');
    if (el) el.classList.add('hidden');
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '❌';
    if (type === 'warning') icon = '⚠️';

    toast.innerHTML = `
        <span style="font-size: 1.1rem;">${icon}</span>
        <span style="flex: 1;">${escapeHtml(message)}</span>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(20px)';
        toast.style.transition = 'all 0.3s ease-in';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// =============================================================================
//  UTILITY HELPERS (Indian Rupee ₹ formatting)
// =============================================================================

function formatCurrency(amount) {
    if (amount === undefined || amount === null || isNaN(amount)) return '₹0.00';
    return '₹' + Number(amount).toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function formatDateDisplay(dateStr) {
    if (!dateStr) return '';
    try {
        const parts = dateStr.split('-');
        if (parts.length === 3) {
            const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            const m = parseInt(parts[1], 10) - 1;
            return `${months[m] || parts[1]} ${parts[2]}, ${parts[0]}`;
        }
        return dateStr;
    } catch (e) {
        return dateStr;
    }
}

function escapeHtml(text) {
    if (!text) return '';
    return text.toString()
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

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
    searchDebounceTimer: null
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
    try {
        const res = await fetch('/api/status');
        if (res.ok) {
            const data = await res.json();
            const info = data.data;
            const storageLabel = document.getElementById('storageLabel');
            const storageBadge = document.getElementById('storageBadge');
            const dot = storageBadge ? storageBadge.querySelector('.status-dot') : null;

            if (storageLabel) storageLabel.textContent = info.storage || 'Connected';
            if (dot) {
                if (info.databaseAvailable || info.storage.includes('Memory')) {
                    dot.className = 'status-dot';
                } else {
                    dot.className = 'status-dot status-dot-offline';
                }
            }
            hideBackendError();
        } else {
            showBackendError();
        }
    } catch (err) {
        showBackendError();
    }
}

/**
 * Loads categories from /api/categories.
 */
async function loadCategories() {
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
        showBackendError();
    }
}

/**
 * Loads primary dashboard metrics from /api/dashboard.
 */
async function loadDashboard() {
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
        showBackendError();
    }
}

/**
 * Loads monthly financial summary for the currently selected month/year.
 */
async function loadMonthlyReport() {
    try {
        const [yearStr, monthStr] = state.selectedMonth.split('-');
        const res = await fetch(`/api/reports/monthly?month=${parseInt(monthStr)}&year=${parseInt(yearStr)}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        state.monthly = data;
        renderMonthlySummary(data);
    } catch (err) {
        console.error('Failed to load monthly report:', err);
    }
}

/**
 * Loads expense category breakdown report for canvas chart.
 */
async function loadCategoryReport() {
    try {
        const [yearStr, monthStr] = state.selectedMonth.split('-');
        const res = await fetch(`/api/reports/categories?month=${parseInt(monthStr)}&year=${parseInt(yearStr)}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        renderDonutChart(data.categories || []);
    } catch (err) {
        console.error('Failed to load category report:', err);
    }
}

/**
 * Loads Top 5 highest single expenses.
 */
async function loadTopExpenses() {
    try {
        const res = await fetch('/api/reports/top-expenses?limit=5');
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        renderTopExpenses(data.topExpenses || []);
    } catch (err) {
        console.error('Failed to load top expenses:', err);
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
        showToast('Error loading transactions: ' + err.message, 'error');
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
        window.location.href = '/api/export';
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

function escapeHtml(text) {
    if (!text) return '';
    return text.toString()
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

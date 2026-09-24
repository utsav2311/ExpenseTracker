/**
 * ==============================================================================
 * ExpenseTracker - Vanilla JavaScript Frontend Client
 * Demonstrates:
 *  - Modern JavaScript Fetch API (async/await, HTTP GET/POST/PUT/DELETE)
 *  - RESTful API consumption and JSON serialization
 *  - DOM manipulation and event delegation
 *  - Pure HTML5 Canvas charting (Zero external chart libraries)
 *  - Real-time debouncing, input validation, and toast notifications
 * ==============================================================================
 */

// Global State
const state = {
    categories: [],
    paymentMethods: [],
    expenses: [],
    summary: null,
    budgets: [],
    deleteTargetId: null,
    searchDebounceTimer: null
};

// Category Color Palette for Badges and Charts
const CATEGORY_COLORS = {
    FOOD: '#f59e0b',
    TRANSPORTATION: '#3b82f6',
    HOUSING: '#8b5cf6',
    UTILITIES: '#10b981',
    ENTERTAINMENT: '#ec4899',
    HEALTHCARE: '#ef4444',
    SHOPPING: '#6366f1',
    EDUCATION: '#06b6d4',
    PERSONAL: '#14b8a6',
    OTHER: '#64748b'
};

// =============================================================================
//  INITIALIZATION
// =============================================================================

document.addEventListener('DOMContentLoaded', async () => {
    initializeDateDefaults();
    bindEventListeners();

    // Load initial metadata and data
    await loadMeta();
    await checkSystemStatus();
    await refreshAllData();
});

function initializeDateDefaults() {
    const today = new Date().toISOString().split('T')[0];
    const formDate = document.getElementById('formDate');
    if (formDate) formDate.value = today;

    const budgetMonth = document.getElementById('budgetMonth');
    if (budgetMonth) {
        const ym = today.substring(0, 7);
        budgetMonth.value = ym;
    }
}

// =============================================================================
//  FETCH API REST CLIENT CALLS
// =============================================================================

/**
 * Fetch Categories and PaymentMethods metadata from backend.
 */
async function loadMeta() {
    try {
        const res = await fetch('/api/meta');
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        state.categories = data.categories || [];
        state.paymentMethods = data.paymentMethods || [];

        populateSelectOptions();
    } catch (err) {
        console.error('Failed to load metadata:', err);
        showToast('Failed to load metadata from server', 'error');
    }
}

/**
 * Checks server health and persistence storage type (MySQL vs In-Memory).
 */
async function checkSystemStatus() {
    try {
        const res = await fetch('/api/status');
        if (res.ok) {
            const result = await res.json();
            const info = result.data;
            const storageLabel = document.getElementById('storageLabel');
            const storageBadge = document.getElementById('storageBadge');

            if (storageLabel) {
                storageLabel.textContent = info.storage || 'Connected';
            }
            if (storageBadge && !info.isDatabase) {
                storageBadge.title = 'MySQL not detected; operating in high-performance In-Memory Collections mode.';
            }
        }
    } catch (err) {
        console.error('System status check error:', err);
    }
}

/**
 * Loads filtered and sorted expenses via Fetch API with query params.
 */
async function loadExpenses() {
    const search = document.getElementById('filterSearch').value.trim();
    const category = document.getElementById('filterCategory').value;
    const paymentMethod = document.getElementById('filterPaymentMethod').value;
    const startDate = document.getElementById('filterStartDate').value;
    const endDate = document.getElementById('filterEndDate').value;
    const sortBy = document.getElementById('filterSortBy').value;
    const sortOrder = document.getElementById('filterSortOrder').value;
    const algo = document.getElementById('filterAlgo').value;

    const params = new URLSearchParams();
    if (search) params.append('search', search);
    if (category && category !== 'ALL') params.append('category', category);
    if (paymentMethod && paymentMethod !== 'ALL') params.append('paymentMethod', paymentMethod);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    if (sortBy) params.append('sortBy', sortBy);
    if (sortOrder) params.append('sortOrder', sortOrder);
    if (algo) params.append('algo', algo);

    const startTime = performance.now();
    try {
        const res = await fetch(`/api/expenses?${params.toString()}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        const duration = (performance.now() - startTime).toFixed(1);

        state.expenses = data.expenses || [];
        renderExpensesTable(state.expenses);

        // Update DSA Algorithm benchmark indicator
        const algoTag = document.getElementById('algoIndicator');
        const benchTag = document.getElementById('dsaBenchmarkTag');
        if (algoTag) algoTag.textContent = `Algo: ${data.algorithmUsed || algo}`;
        if (benchTag) benchTag.textContent = `${data.count} items (${duration}ms API)`;

    } catch (err) {
        console.error('Failed to load expenses:', err);
        showToast('Error loading expenses: ' + err.message, 'error');
    }
}

/**
 * Loads financial summary metrics and triggers canvas chart rendering.
 */
async function loadSummary() {
    try {
        const res = await fetch('/api/summary');
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        state.summary = data.summary;
        renderSummaryKPIs(state.summary);
        renderDonutChart(state.summary);
    } catch (err) {
        console.error('Failed to load summary:', err);
    }
}

/**
 * Loads monthly budgets and current spend progress.
 */
async function loadBudgets() {
    try {
        const today = new Date().toISOString().substring(0, 7);
        const res = await fetch(`/api/budgets?monthYear=${today}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        state.budgets = data.budgets || [];
        renderBudgetsList(state.budgets);
    } catch (err) {
        console.error('Failed to load budgets:', err);
    }
}

async function refreshAllData() {
    await Promise.all([loadExpenses(), loadSummary(), loadBudgets()]);
}

// =============================================================================
//  DOM RENDERING: EXPENSES TABLE
// =============================================================================

function renderExpensesTable(expenses) {
    const tbody = document.getElementById('expensesTableBody');
    const emptyState = document.getElementById('emptyState');
    const recordCount = document.getElementById('tableRecordCount');

    if (recordCount) {
        recordCount.textContent = `Showing ${expenses.length} transaction${expenses.length === 1 ? '' : 's'}`;
    }

    if (!expenses || expenses.length === 0) {
        tbody.innerHTML = '';
        emptyState.classList.remove('hidden');
        return;
    }

    emptyState.classList.add('hidden');
    let html = '';

    expenses.forEach(e => {
        const catColor = CATEGORY_COLORS[e.category] || '#64748b';
        html += `
            <tr>
                <td style="font-family: var(--font-mono); color: var(--text-muted);">#${e.id}</td>
                <td>
                    <div style="font-weight: 500;">${e.displayDate || e.date}</div>
                    <div class="text-muted text-xs">${e.date}</div>
                </td>
                <td>
                    <div class="table-title">${escapeHtml(e.title)}</div>
                    ${e.notes ? `<div class="table-notes" title="${escapeHtml(e.notes)}">${escapeHtml(e.notes)}</div>` : ''}
                </td>
                <td>
                    <span class="badge badge-cat" style="background-color: ${catColor}15; color: ${catColor}; border: 1px solid ${catColor}35;">
                        <span>${e.categoryIcon || '🏷️'}</span>
                        <span>${escapeHtml(e.categoryName || e.category)}</span>
                    </span>
                </td>
                <td>
                    <span class="badge badge-pm">
                        <span>${e.paymentMethodIcon || '💳'}</span>
                        <span>${escapeHtml(e.paymentMethodName || e.paymentMethod)}</span>
                    </span>
                </td>
                <td style="text-align: right;">
                    <span class="table-amount">$${formatAmount(e.amount)}</span>
                </td>
                <td style="text-align: center;">
                    <div class="table-actions">
                        <button class="action-btn action-btn-edit" onclick="openEditModal(${e.id})" title="Edit Expense">✏️</button>
                        <button class="action-btn action-btn-delete" onclick="openDeleteModal(${e.id}, '${escapeHtml(e.title)}')" title="Delete Expense">🗑️</button>
                    </div>
                </td>
            </tr>
        `;
    });

    tbody.innerHTML = html;
}

// =============================================================================
//  DOM RENDERING: SUMMARY KPIS
// =============================================================================

function renderSummaryKPIs(summary) {
    if (!summary) return;

    document.getElementById('kpiTotalSpent').textContent = `$${formatAmount(summary.totalExpenses)}`;
    document.getElementById('kpiTotalCount').textContent = `${summary.totalCount} total transaction${summary.totalCount === 1 ? '' : 's'}`;
    document.getElementById('kpiAverage').textContent = `$${formatAmount(summary.averageExpense)}`;

    const kpiTopCatName = document.getElementById('kpiTopCatName');
    const kpiTopCatIcon = document.getElementById('kpiTopCatIcon');
    const kpiTopCatAmount = document.getElementById('kpiTopCatAmount');

    if (summary.topCategory) {
        kpiTopCatName.textContent = summary.topCategoryName || summary.topCategory;
        kpiTopCatIcon.textContent = summary.topCategoryIcon || '🛍️';
        kpiTopCatAmount.textContent = `$${formatAmount(summary.topCategoryAmount)} spent`;
    } else {
        kpiTopCatName.textContent = 'None';
        kpiTopCatIcon.textContent = '🛍️';
        kpiTopCatAmount.textContent = '$0.00 spent';
    }

    const kpiHighestAmount = document.getElementById('kpiHighestAmount');
    const kpiHighestTitle = document.getElementById('kpiHighestTitle');

    if (summary.highestExpense) {
        kpiHighestAmount.textContent = `$${formatAmount(summary.highestExpense.amount)}`;
        kpiHighestTitle.textContent = `${summary.highestExpense.title} (${summary.highestExpense.displayDate || summary.highestExpense.date})`;
    } else {
        kpiHighestAmount.textContent = '$0.00';
        kpiHighestTitle.textContent = 'No records';
    }
}

// =============================================================================
//  DOM RENDERING: BUDGETS LIST
// =============================================================================

function renderBudgetsList(budgets) {
    const container = document.getElementById('budgetsListContainer');
    if (!budgets || budgets.length === 0) {
        container.innerHTML = `
            <div class="text-center py-4">
                <p class="text-muted text-sm">No monthly budgets configured.</p>
                <button class="btn btn-outline btn-sm mt-1" onclick="openBudgetModal()">Set Your First Budget</button>
            </div>
        `;
        return;
    }

    let html = '';
    budgets.forEach(b => {
        const pct = b.percentageUsed || 0;
        let colorClass = 'progress-green';
        if (pct >= 90 || b.isExceeded) {
            colorClass = 'progress-red';
        } else if (pct >= 70) {
            colorClass = 'progress-orange';
        }

        html += `
            <div class="budget-item">
                <div class="budget-item-header">
                    <span class="budget-cat-name">
                        <span>${b.categoryIcon || '🎯'}</span>
                        <span>${escapeHtml(b.categoryName || b.category)}</span>
                    </span>
                    <span class="budget-amounts">
                        <strong>$${formatAmount(b.spent)}</strong> / $${formatAmount(b.monthlyLimit)}
                        <span class="text-xs" style="color: ${b.isExceeded ? 'var(--danger)' : 'var(--text-muted)'}; font-weight: 600;">
                            (${b.isExceeded ? 'EXCEEDED' : pct.toFixed(0) + '%'})
                        </span>
                    </span>
                </div>
                <div class="budget-progress-bar">
                    <div class="budget-progress-fill ${colorClass}" style="width: ${Math.min(pct, 100)}%;"></div>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

// =============================================================================
//  CANVAS DONUT CHART (Zero external dependencies)
// =============================================================================

function renderDonutChart(summary) {
    const canvas = document.getElementById('categoryDonutChart');
    const legend = document.getElementById('chartLegend');
    if (!canvas || !summary) return;

    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    ctx.clearRect(0, 0, width, height);

    const breakdown = summary.categoryBreakdown || {};
    const percentages = summary.categoryPercentages || {};
    const categories = Object.keys(breakdown);

    if (categories.length === 0 || summary.totalExpenses <= 0) {
        // Draw empty placeholder circle
        ctx.beginPath();
        ctx.arc(width / 2, height / 2, 70, 0, 2 * Math.PI);
        ctx.strokeStyle = '#e2e8f0';
        ctx.lineWidth = 25;
        ctx.stroke();

        ctx.fillStyle = '#94a3b8';
        ctx.font = '14px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('No Data', width / 2, height / 2);

        legend.innerHTML = '<span class="text-muted text-xs">No expenses to display</span>';
        return;
    }

    const centerX = width / 2;
    const centerY = height / 2;
    const radius = 75;
    const lineWidth = 26;

    let startAngle = -0.5 * Math.PI;
    let legendHtml = '';

    categories.forEach(cat => {
        const amount = breakdown[cat];
        const pct = percentages[cat] || ((amount / summary.totalExpenses) * 100);
        const sliceAngle = (amount / summary.totalExpenses) * (2 * Math.PI);
        const color = CATEGORY_COLORS[cat] || '#64748b';

        // Draw donut segment
        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, startAngle, startAngle + sliceAngle);
        ctx.strokeStyle = color;
        ctx.lineWidth = lineWidth;
        ctx.stroke();

        startAngle += sliceAngle;

        // Build Legend Item
        const catObj = state.categories.find(c => c.code === cat);
        const label = catObj ? catObj.name : cat;
        const icon = catObj ? catObj.icon : '🏷️';

        legendHtml += `
            <div class="legend-item" title="${label}: $${formatAmount(amount)} (${pct.toFixed(1)}%)">
                <span class="legend-color-dot" style="background-color: ${color};"></span>
                <span class="legend-label">${icon} ${escapeHtml(label)}</span>
                <span class="legend-value">${pct.toFixed(0)}%</span>
            </div>
        `;
    });

    // Center total label
    ctx.fillStyle = '#0f172a';
    ctx.font = 'bold 15px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(`$${formatAmount(summary.totalExpenses)}`, centerX, centerY - 6);

    ctx.fillStyle = '#64748b';
    ctx.font = '11px sans-serif';
    ctx.fillText('Total Spent', centerX, centerY + 12);

    legend.innerHTML = legendHtml;
}

// =============================================================================
//  FORM POPULATION & MODAL MANAGEMENT
// =============================================================================

function populateSelectOptions() {
    const filterCat = document.getElementById('filterCategory');
    const filterPm = document.getElementById('filterPaymentMethod');
    const formCat = document.getElementById('formCategory');
    const formPm = document.getElementById('formPaymentMethod');
    const budgetCat = document.getElementById('budgetCategory');

    // Populate Categories
    let filterCatOptions = '<option value="ALL">All Categories</option>';
    let formCatOptions = '';
    state.categories.forEach(c => {
        filterCatOptions += `<option value="${c.code}">${c.icon} ${c.name}</option>`;
        formCatOptions += `<option value="${c.code}">${c.icon} ${c.name}</option>`;
    });

    if (filterCat) filterCat.innerHTML = filterCatOptions;
    if (formCat) formCat.innerHTML = formCatOptions;
    if (budgetCat) budgetCat.innerHTML = formCatOptions;

    // Populate Payment Methods
    let filterPmOptions = '<option value="ALL">All Methods</option>';
    let formPmOptions = '';
    state.paymentMethods.forEach(p => {
        filterPmOptions += `<option value="${p.code}">${p.icon} ${p.name}</option>`;
        formPmOptions += `<option value="${p.code}">${p.icon} ${p.name}</option>`;
    });

    if (filterPm) filterPm.innerHTML = filterPmOptions;
    if (formPm) formPm.innerHTML = formPmOptions;
}

function openAddModal() {
    document.getElementById('modalTitle').textContent = 'Add New Expense';
    document.getElementById('expenseId').value = '';
    document.getElementById('expenseForm').reset();
    initializeDateDefaults();
    document.getElementById('formErrorBanner').classList.add('hidden');
    document.getElementById('expenseModal').classList.remove('hidden');
    document.getElementById('formTitle').focus();
}

function openEditModal(id) {
    const expense = state.expenses.find(e => e.id === id);
    if (!expense) return;

    document.getElementById('modalTitle').textContent = `Edit Expense #${expense.id}`;
    document.getElementById('expenseId').value = expense.id;
    document.getElementById('formTitle').value = expense.title;
    document.getElementById('formAmount').value = expense.amount;
    document.getElementById('formDate').value = expense.date;
    document.getElementById('formCategory').value = expense.category;
    document.getElementById('formPaymentMethod').value = expense.paymentMethod;
    document.getElementById('formNotes').value = expense.notes || '';
    document.getElementById('formErrorBanner').classList.add('hidden');

    document.getElementById('expenseModal').classList.remove('hidden');
    document.getElementById('formTitle').focus();
}

function closeExpenseModal() {
    document.getElementById('expenseModal').classList.add('hidden');
}

function openBudgetModal() {
    document.getElementById('budgetForm').reset();
    initializeDateDefaults();
    document.getElementById('budgetErrorBanner').classList.add('hidden');
    document.getElementById('budgetModal').classList.remove('hidden');
}

function closeBudgetModal() {
    document.getElementById('budgetModal').classList.add('hidden');
}

function openDeleteModal(id, title) {
    state.deleteTargetId = id;
    document.getElementById('deleteModalMessage').textContent = `Are you sure you want to permanently delete expense #${id}: "${title}"?`;
    document.getElementById('deleteModal').classList.remove('hidden');
}

function closeDeleteModal() {
    state.deleteTargetId = null;
    document.getElementById('deleteModal').classList.add('hidden');
}

// =============================================================================
//  FORM SUBMISSIONS (POST & PUT via Fetch API)
// =============================================================================

async function handleExpenseSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('expenseId').value;
    const isEdit = Boolean(id);

    const payload = {
        title: document.getElementById('formTitle').value.trim(),
        amount: parseFloat(document.getElementById('formAmount').value),
        date: document.getElementById('formDate').value,
        category: document.getElementById('formCategory').value,
        paymentMethod: document.getElementById('formPaymentMethod').value,
        notes: document.getElementById('formNotes').value.trim()
    };

    const errBanner = document.getElementById('formErrorBanner');
    errBanner.classList.add('hidden');

    try {
        const url = isEdit ? `/api/expenses/${id}` : '/api/expenses';
        const method = isEdit ? 'PUT' : 'POST';

        const res = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (!res.ok || !data.success) {
            throw new Error(data.error || 'Failed to save expense');
        }

        closeExpenseModal();
        showToast(data.message || 'Expense saved successfully!', 'success');

        if (data.budgetExceeded) {
            showToast(data.budgetWarning || 'Warning: Monthly budget exceeded!', 'warning');
        }

        await refreshAllData();

    } catch (err) {
        errBanner.textContent = err.message;
        errBanner.classList.remove('hidden');
    }
}

async function handleBudgetSubmit(e) {
    e.preventDefault();
    const payload = {
        category: document.getElementById('budgetCategory').value,
        monthlyLimit: parseFloat(document.getElementById('budgetLimit').value),
        monthYear: document.getElementById('budgetMonth').value
    };

    const errBanner = document.getElementById('budgetErrorBanner');
    errBanner.classList.add('hidden');

    try {
        const res = await fetch('/api/budgets', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (!res.ok || !data.success) {
            throw new Error(data.error || 'Failed to save budget');
        }

        closeBudgetModal();
        showToast('Budget saved successfully!', 'success');
        await loadBudgets();

    } catch (err) {
        errBanner.textContent = err.message;
        errBanner.classList.remove('hidden');
    }
}

async function confirmDeleteExpense() {
    if (!state.deleteTargetId) return;
    const id = state.deleteTargetId;

    try {
        const res = await fetch(`/api/expenses/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok || !data.success) {
            throw new Error(data.error || 'Failed to delete expense');
        }

        closeDeleteModal();
        showToast(`Expense #${id} deleted successfully`, 'success');
        await refreshAllData();

    } catch (err) {
        closeDeleteModal();
        showToast('Delete error: ' + err.message, 'error');
    }
}

// =============================================================================
//  DEMO RELOAD & CSV EXPORT
// =============================================================================

async function handleReloadDemo() {
    if (!confirm('Reload sample expenses dataset? This will refresh your records with sample data.')) return;
    try {
        const res = await fetch('/api/demo', { method: 'POST' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Failed to reload demo data');
        showToast('Sample dataset reloaded successfully!', 'success');
        await refreshAllData();
    } catch (err) {
        showToast('Error: ' + err.message, 'error');
    }
}

function handleExportCsv() {
    window.location.href = '/api/export';
}

function resetFilters() {
    document.getElementById('filterSearch').value = '';
    document.getElementById('filterCategory').value = 'ALL';
    document.getElementById('filterPaymentMethod').value = 'ALL';
    document.getElementById('filterStartDate').value = '';
    document.getElementById('filterEndDate').value = '';
    document.getElementById('filterSortBy').value = 'date';
    document.getElementById('filterSortOrder').value = 'desc';
    document.getElementById('filterAlgo').value = 'TIM_SORT';
    loadExpenses();
}

// =============================================================================
//  EVENT LISTENERS & DEBOUNCING
// =============================================================================

function bindEventListeners() {
    // Modal buttons
    document.getElementById('btnOpenAddModal').addEventListener('click', openAddModal);
    document.getElementById('btnModalClose').addEventListener('click', closeExpenseModal);
    document.getElementById('btnModalCancel').addEventListener('click', closeExpenseModal);
    document.getElementById('expenseForm').addEventListener('submit', handleExpenseSubmit);

    document.getElementById('btnOpenBudgetModal').addEventListener('click', openBudgetModal);
    document.getElementById('btnBudgetModalClose').addEventListener('click', closeBudgetModal);
    document.getElementById('btnBudgetModalCancel').addEventListener('click', closeBudgetModal);
    document.getElementById('budgetForm').addEventListener('submit', handleBudgetSubmit);

    document.getElementById('btnDeleteModalClose').addEventListener('click', closeDeleteModal);
    document.getElementById('btnCancelDelete').addEventListener('click', closeDeleteModal);
    document.getElementById('btnConfirmDelete').addEventListener('click', confirmDeleteExpense);

    // Toolbar buttons
    document.getElementById('btnExportCsv').addEventListener('click', handleExportCsv);
    document.getElementById('btnReloadDemo').addEventListener('click', handleReloadDemo);
    document.getElementById('btnResetFilters').addEventListener('click', resetFilters);

    // Search input debouncing (300ms)
    document.getElementById('filterSearch').addEventListener('input', () => {
        clearTimeout(state.searchDebounceTimer);
        state.searchDebounceTimer = setTimeout(loadExpenses, 280);
    });

    // Dropdown filters
    ['filterCategory', 'filterPaymentMethod', 'filterStartDate', 'filterEndDate', 'filterSortBy', 'filterSortOrder', 'filterAlgo']
        .forEach(id => {
            const el = document.getElementById(id);
            if (el) el.addEventListener('change', loadExpenses);
        });

    // Close modals on Escape key
    window.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeExpenseModal();
            closeBudgetModal();
            closeDeleteModal();
        }
    });
}

// =============================================================================
//  TOAST NOTIFICATION ENGINE
// =============================================================================

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
//  UTILITY HELPERS
// =============================================================================

function formatAmount(amount) {
    if (amount === undefined || amount === null || isNaN(amount)) return '0.00';
    return Number(amount).toLocaleString('en-US', {
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

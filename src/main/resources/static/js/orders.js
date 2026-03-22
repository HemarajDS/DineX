let orderBoardEntries = [];
let masterTables = [];
let filteredOrders = [];
let currentPage = 1;

const PAGE_SIZE = 6;

document.addEventListener('DOMContentLoaded', async () => {
    renderNavbar('orders');
    const user = requireRoles(['ADMIN', 'WAITER', 'KITCHEN', 'CASHIER']);
    if (!user) {
        return;
    }

    bindBoardEvents();
    setDefaultDateFilter();

    await Promise.all([loadMasterTables(), loadOrders()]);
});

function bindBoardEvents() {
    document.getElementById('ordersTableBody').addEventListener('click', handleOrderActions);
    document.getElementById('orderDateFilter').addEventListener('change', () => applyBoardFilters(true));
    document.getElementById('orderTableFilter').addEventListener('change', () => applyBoardFilters(true));
    document.getElementById('orderStatusFilter').addEventListener('change', () => applyBoardFilters(true));
    document.getElementById('orderSearchFilter').addEventListener('input', () => applyBoardFilters(true));
    document.getElementById('ordersPrevPage').addEventListener('click', () => changePage(-1));
    document.getElementById('ordersNextPage').addEventListener('click', () => changePage(1));
}

function setDefaultDateFilter() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    document.getElementById('orderDateFilter').value = `${year}-${month}-${day}`;
}

async function loadMasterTables() {
    try {
        const user = getLoggedInUser();
        const tables = await apiRequest('/api/tables', {
            headers: buildHeaders(false),
            loadingText: 'Loading table master data...'
        });

        masterTables = user.role === 'WAITER'
            ? tables.filter(table => (user.assignedTableNumbers || []).includes(table.tableNumber))
            : tables;

        renderBoardTableFilter();
    } catch (error) {
        masterTables = [];
        renderBoardTableFilter();
        showToast(error.message, 'danger');
    }
}

function renderBoardTableFilter() {
    const select = document.getElementById('orderTableFilter');
    if (!select) {
        return;
    }

    select.innerHTML = [
        '<option value="ALL">All tables</option>',
        ...masterTables.map(table => `<option value="${table.tableNumber}">${table.tableNumber}</option>`)
    ].join('');
}

async function loadOrders() {
    try {
        orderBoardEntries = await apiRequest('/api/orders', {
            headers: buildHeaders(false),
            loadingText: 'Loading order board...'
        });

        applyBoardFilters(true);
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

function applyBoardFilters(resetPage = false) {
    const user = getLoggedInUser();
    const selectedDate = document.getElementById('orderDateFilter').value;
    const selectedTable = document.getElementById('orderTableFilter').value;
    const selectedStatus = document.getElementById('orderStatusFilter').value;
    const searchTerm = document.getElementById('orderSearchFilter').value.trim().toLowerCase();

    filteredOrders = orderBoardEntries.filter(order => {
        const orderDate = normalizeDate(order.createdAt);
        const matchesDate = !selectedDate || orderDate === selectedDate;
        const matchesTable = selectedTable === 'ALL' || order.tableNumber === selectedTable;
        const matchesStatus = selectedStatus === 'ALL' || order.status === selectedStatus;
        const searchBlob = [
            order.orderCode,
            order.customerName,
            order.tableNumber,
            ...(order.items || []).map(item => item.menuItemName)
        ].join(' ').toLowerCase();
        const matchesSearch = !searchTerm || searchBlob.includes(searchTerm);

        if (user.role === 'WAITER' && !(user.assignedTableNumbers || []).includes(order.tableNumber)) {
            return false;
        }

        return matchesDate && matchesTable && matchesStatus && matchesSearch;
    });

    if (resetPage) {
        currentPage = 1;
    }

    document.getElementById('ordersCount').textContent = filteredOrders.length;
    renderBillSummaryControls(filteredOrders, user);
    renderOrderBoard(filteredOrders, user);
    renderPagination();
}

function renderOrderBoard(orders, user) {
    const host = document.getElementById('ordersTableBody');
    const emptyState = document.getElementById('ordersEmptyState');

    if (!orders.length) {
        host.innerHTML = '';
        emptyState.classList.remove('hidden');
        return;
    }

    emptyState.classList.add('hidden');
    const pagedOrders = paginateOrders(orders);
    host.innerHTML = pagedOrders.map(order => `
        <article class="order-ticket">
            <div class="order-ticket__topbar">
                <div class="order-ticket__identity">
                    <span class="order-ticket__code">${order.orderCode || order.id}</span>
                    <span class="table-id">${order.tableNumber}</span>
                </div>
                <div class="order-ticket__microcards">
                    <div class="order-mini-card">
                        <div class="order-mini-card__label">Status</div>
                        <div>${statusBadge(order.status)}</div>
                    </div>
                    <div class="order-mini-card">
                        <div class="order-mini-card__label">Billing</div>
                        <div>${order.billed ? '<span class="status-badge status-COMPLETED">Billed</span>' : '<span class="status-badge status-PENDING">Open</span>'}</div>
                    </div>
                    <div class="order-mini-card">
                        <div class="order-mini-card__label">Total</div>
                        <div class="order-mini-card__value">${currency(order.totalAmount)}</div>
                    </div>
                </div>
            </div>

            <div class="order-ticket__summary">
                <div class="order-summary-pill">
                    <span class="order-summary-pill__label">Customer</span>
                    <span class="order-summary-pill__value">${order.customerName}</span>
                </div>
                <div class="order-summary-pill">
                    <span class="order-summary-pill__label">Created</span>
                    <span class="order-summary-pill__value">${formatDate(order.createdAt)}</span>
                </div>
                <div class="order-summary-pill">
                    <span class="order-summary-pill__label">Progress</span>
                    <span class="order-summary-pill__value">${buildProgressText(order)}</span>
                </div>
            </div>

            <div class="order-ticket__items">
                ${order.items.map(item => renderDishRow(order, item, user)).join('')}
            </div>

            <div class="order-ticket__footerbar">
                <div class="order-ticket__hint">${order.items.length} dish${order.items.length === 1 ? '' : 'es'} on this ticket</div>
                <div class="order-ticket__billing-action">
                    ${['ADMIN', 'WAITER', 'CASHIER'].includes(user.role) ? buildBillingAction(order) : '<span class="text-muted small">Billing unavailable</span>'}
                </div>
            </div>
        </article>
    `).join('');
}

function renderDishRow(order, item, user) {
    return `
        <div class="dish-row">
            <div class="dish-row__content">
                <div class="dish-row__title">${item.menuItemName}</div>
                <div class="dish-row__meta">${item.quantity} x ${currency(item.price)} - ${currency(item.lineTotal)}</div>
            </div>
            <div class="dish-row__status">
                ${statusBadge(item.status)}
            </div>
            <div class="dish-row__actions">
                ${buildDishActionInline(order, item, user)}
            </div>
        </div>
    `;
}

function buildDishActionInline(order, item, user) {
    const selectId = `item-status-${order.id}-${item.id}`;
    const canUpdate = ['ADMIN', 'KITCHEN', 'WAITER'].includes(user.role);

    if (!canUpdate) {
        return '<span class="text-muted small">View only</span>';
    }

    if (user.role === 'WAITER') {
        if (item.status !== 'COMPLETED') {
            return '<span class="text-muted small">Wait for kitchen</span>';
        }

        return `
            <select class="form-select form-select-sm status-control__select" id="${selectId}">
                <option value="SERVED" selected>SERVED</option>
            </select>
            <button class="btn btn-sm btn-gold-soft rounded-pill px-3" data-action="save-item-status" data-order-id="${order.id}" data-item-id="${item.id}">Save</button>
        `;
    }

    const statuses = user.role === 'KITCHEN'
        ? ['PENDING', 'PREPARING', 'COMPLETED']
        : ['PENDING', 'PREPARING', 'COMPLETED', 'SERVED'];

    return `
        <select class="form-select form-select-sm status-control__select" id="${selectId}">
            ${statuses.map(status => `<option value="${status}" ${item.status === status ? 'selected' : ''}>${status}</option>`).join('')}
        </select>
        <button class="btn btn-sm btn-gold-soft rounded-pill px-3" data-action="save-item-status" data-order-id="${order.id}" data-item-id="${item.id}">Save</button>
    `;
}

function buildProgressText(order) {
    const servedCount = order.items.filter(item => item.status === 'SERVED').length;
    const readyCount = order.items.filter(item => item.status === 'COMPLETED').length;
    const preparingCount = order.items.filter(item => item.status === 'PREPARING').length;
    const pendingCount = order.items.filter(item => item.status === 'PENDING').length;

    return `${servedCount} served, ${readyCount} ready, ${preparingCount} preparing, ${pendingCount} pending`;
}

function paginateOrders(orders) {
    const startIndex = (currentPage - 1) * PAGE_SIZE;
    return orders.slice(startIndex, startIndex + PAGE_SIZE);
}

function renderPagination() {
    const pagination = document.getElementById('ordersPagination');
    const meta = document.getElementById('ordersPageMeta');
    const prev = document.getElementById('ordersPrevPage');
    const next = document.getElementById('ordersNextPage');
    const totalPages = Math.max(1, Math.ceil(filteredOrders.length / PAGE_SIZE));

    if (currentPage > totalPages) {
        currentPage = totalPages;
    }

    if (filteredOrders.length <= PAGE_SIZE) {
        pagination.classList.add('hidden');
    } else {
        pagination.classList.remove('hidden');
    }

    meta.textContent = `Page ${currentPage} of ${totalPages}`;
    prev.disabled = currentPage === 1;
    next.disabled = currentPage === totalPages;
}

function changePage(direction) {
    const totalPages = Math.max(1, Math.ceil(filteredOrders.length / PAGE_SIZE));
    const nextPage = currentPage + direction;
    if (nextPage < 1 || nextPage > totalPages) {
        return;
    }

    currentPage = nextPage;
    renderOrderBoard(filteredOrders, getLoggedInUser());
    renderPagination();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function handleOrderActions(event) {
    const statusButton = event.target.closest('button[data-action="save-item-status"]');
    if (statusButton) {
        updateItemStatus(statusButton.dataset.orderId, statusButton.dataset.itemId);
        return;
    }

    const billingButton = event.target.closest('button[data-action="toggle-billing"]');
    if (billingButton) {
        updateBilling(billingButton.dataset.orderId, billingButton.dataset.billed === 'true');
    }
}

function renderBillSummaryControls(orders, user) {
    const section = document.getElementById('billSummarySection');
    const select = document.getElementById('billTableSelect');
    if (!section || !select) {
        return;
    }

    if (user.role === 'KITCHEN') {
        section.classList.add('hidden');
        return;
    }

    section.classList.remove('hidden');
    const activeTables = new Set(orders.map(order => order.tableNumber));
    const visibleMasterTables = masterTables.filter(table => activeTables.size === 0 || activeTables.has(table.tableNumber));

    select.innerHTML = [
        '<option value="ALL">All / Overall</option>',
        ...visibleMasterTables.map(table => `<option value="${table.tableNumber}">${table.tableNumber}</option>`)
    ].join('');

    loadTableBill(select.value || 'ALL', orders);
    select.onchange = () => loadTableBill(select.value, orders);
}

async function loadTableBill(tableNumber, visibleOrders = filteredOrders) {
    if (tableNumber === 'ALL') {
        const totalAmount = visibleOrders.reduce((sum, order) => sum + Number(order.totalAmount || 0), 0);
        document.getElementById('billTableLabel').textContent = 'All Tables';
        document.getElementById('billOrderCount').textContent = visibleOrders.length;
        document.getElementById('billTotalAmount').textContent = currency(totalAmount);
        return;
    }

    try {
        const summary = await apiRequest(`/api/orders/table-bill?tableNumber=${encodeURIComponent(tableNumber)}`, {
            headers: buildHeaders(false),
            loadingText: 'Loading table bill...'
        });

        document.getElementById('billTableLabel').textContent = summary.tableNumber;
        document.getElementById('billOrderCount').textContent = summary.orderCount;
        document.getElementById('billTotalAmount').textContent = currency(summary.totalAmount);
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

async function updateItemStatus(orderId, itemId) {
    const status = document.getElementById(`item-status-${orderId}-${itemId}`).value;
    try {
        const updated = await apiRequest(`/api/orders/${orderId}/items/${itemId}/status`, {
            method: 'PATCH',
            headers: buildHeaders(),
            body: JSON.stringify({ status }),
            loadingText: 'Updating dish status...'
        });
        showMessage(`Dish updated for ${updated.orderCode || updated.id}.`);
        await loadOrders();
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

function buildBillingAction(order) {
    return `
        <button class="btn btn-sm ${order.billed ? 'btn-outline-light' : 'btn-gold-soft'} rounded-pill px-3" data-action="toggle-billing" data-order-id="${order.id}" data-billed="${!order.billed}">
            ${order.billed ? 'Mark Unbilled' : 'Mark Billed'}
        </button>
    `;
}

async function updateBilling(orderId, billed) {
    try {
        const updated = await apiRequest(`/api/orders/${orderId}/billing`, {
            method: 'PATCH',
            headers: buildHeaders(),
            body: JSON.stringify({ billed }),
            loadingText: 'Updating billing state...'
        });
        showMessage(`Billing updated for ${updated.orderCode || updated.id}.`);
        await loadOrders();
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

function normalizeDate(value) {
    if (!value) {
        return '';
    }

    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
        return '';
    }

    const year = parsed.getFullYear();
    const month = String(parsed.getMonth() + 1).padStart(2, '0');
    const day = String(parsed.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

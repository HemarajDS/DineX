let menuItems = [];
let filteredMenuItems = [];
let orderCart = [];
let activeUser;
let availableTables = [];
let activeOrder = null;

document.addEventListener('DOMContentLoaded', async () => {
    renderNavbar('create-order');
    activeUser = requireRoles(['ADMIN', 'WAITER', 'CASHIER']);
    if (!activeUser) {
        return;
    }

    document.getElementById('createOrderForm').addEventListener('submit', submitOrder);
    document.getElementById('menuCatalog').addEventListener('click', handleCatalogClick);
    document.getElementById('categoryFilter').addEventListener('change', applyFilters);
    document.getElementById('searchFilter').addEventListener('input', applyFilters);
    document.getElementById('tableNumber').addEventListener('change', handleTableChange);

    await Promise.all([loadTables(), loadMenuCatalog()]);
    renderCart();
    renderActiveOrderPanel();
});

async function loadTables() {
    try {
        const selectedTable = document.getElementById('tableNumber').value;
        const tables = await apiRequest('/api/tables', {
            headers: buildHeaders(false),
            loadingText: 'Loading master tables...'
        });

        availableTables = activeUser.role === 'WAITER'
            ? tables.filter(table => (activeUser.assignedTableNumbers || []).includes(table.tableNumber))
            : tables;

        const select = document.getElementById('tableNumber');
        select.innerHTML = `
            <option value="">Select table</option>
            ${availableTables.map(table => `<option value="${table.tableNumber}">${table.tableNumber} - ${table.capacity} seats${table.occupied ? ' - Occupied' : ''}</option>`).join('')}
        `;

        if (activeUser.role === 'WAITER' && availableTables.length === 1) {
            select.value = availableTables[0].tableNumber;
        } else if (selectedTable && availableTables.some(table => table.tableNumber === selectedTable)) {
            select.value = selectedTable;
        }

        if (select.value) {
            await loadActiveOrderForTable(select.value);
        } else {
            activeOrder = null;
            renderActiveOrderPanel();
        }
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

async function loadMenuCatalog() {
    try {
        menuItems = await apiRequest('/api/menu-items', { loadingText: 'Loading menu catalog...' });
        buildFilterOptions();
        applyFilters();
        document.getElementById('menuCatalogCount').textContent = `${menuItems.length} items available in catalog`;
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

function buildFilterOptions() {
    const categories = [...new Set(menuItems.map(item => item.category).filter(Boolean))].sort();
    document.getElementById('categoryFilter').innerHTML = `
        <option value="">All categories</option>
        ${categories.map(category => `<option value="${category}">${category}</option>`).join('')}
    `;
}

function applyFilters() {
    const selectedCategory = document.getElementById('categoryFilter').value;
    const search = document.getElementById('searchFilter').value.trim().toLowerCase();

    filteredMenuItems = menuItems.filter(item => {
        const matchCategory = !selectedCategory || item.category === selectedCategory;
        const matchSearch = !search
            || item.name.toLowerCase().includes(search)
            || (item.description || '').toLowerCase().includes(search);
        return matchCategory && matchSearch;
    });

    renderMenuCards();
}

function renderMenuCards() {
    const host = document.getElementById('menuCatalog');
    if (filteredMenuItems.length === 0) {
        host.innerHTML = `
            <div class="empty-state">
                <div class="empty-state__title">No dishes match this filter</div>
                <div class="empty-state__text">Try another category or search term to find the dish faster.</div>
            </div>
        `;
        return;
    }

    host.innerHTML = filteredMenuItems.map(item => `
        <article class="menu-card menu-card--interactive menu-card--manual ${!item.available ? 'menu-card--unavailable' : ''}">
            <div class="menu-card__image-wrap">
                <img src="${item.imageUrl}" alt="${item.name}" class="menu-card__image">
                ${!item.available ? '<span class="availability-badge">Sold Out</span>' : '<span class="availability-badge availability-badge--live">Available</span>'}
            </div>
            <div class="menu-card__body">
                <div class="menu-card__top">
                    <div class="menu-card__copy">
                        <span class="menu-category-tag">${item.category}</span>
                        <h5 class="mb-1 mt-3">${item.name}</h5>
                    </div>
                    <div class="price-tag">${currency(item.price)}</div>
                </div>
                <p class="menu-card__text mt-3">${item.description || 'Freshly prepared for dine-in service.'}</p>
                <div class="input-group mt-3">
                    <input type="number" min="1" value="1" class="form-control" id="qty-${item.id}" ${!item.available ? 'disabled' : ''}>
                    <button class="btn btn-dark" type="button" data-action="add-to-cart" data-item-id="${item.id}" ${!item.available ? 'disabled' : ''}>Add</button>
                </div>
            </div>
        </article>
    `).join('');
}

function handleCatalogClick(event) {
    const button = event.target.closest('button[data-action="add-to-cart"]');
    if (!button) {
        return;
    }
    addToCart(button.dataset.itemId);
}

function addToCart(itemId) {
    const quantity = parseInt(document.getElementById(`qty-${itemId}`).value, 10);
    if (!quantity || quantity < 1) {
        showMessage('Quantity must be at least 1.', 'danger');
        return;
    }

    const menuItem = menuItems.find(item => item.id === itemId);
    if (!menuItem || !menuItem.available) {
        showMessage('Selected item is not available right now.', 'danger');
        return;
    }

    const existing = orderCart.find(item => item.menuItemId === itemId);
    if (existing) {
        existing.quantity += quantity;
    } else {
        orderCart.push({
            menuItemId: itemId,
            name: menuItem.name,
            description: menuItem.description || '',
            price: Number(menuItem.price),
            quantity
        });
    }

    renderCart();
    showMessage(`${menuItem.name} added to the current order.`);
}

function renderCart() {
    const total = orderCart.reduce((sum, item) => sum + item.price * item.quantity, 0);
    document.getElementById('cartTableBody').innerHTML = orderCart.map((item, index) => `
        <tr>
            <td>
                <div class="cart-item-name">${item.name}</div>
                <div class="cart-item-meta">${item.description || 'Chef selected item'}</div>
            </td>
            <td>${currency(item.price)}</td>
            <td>${item.quantity}</td>
            <td>${currency(item.price * item.quantity)}</td>
            <td><button class="btn btn-sm btn-outline-danger rounded-pill px-3" type="button" onclick="removeCartItem(${index})">Remove</button></td>
        </tr>
    `).join('') || '<tr><td colspan="5" class="text-center text-muted py-4">No items added yet.</td></tr>';

    document.getElementById('orderTotal').textContent = currency(total);
    document.getElementById('cartCount').textContent = `${orderCart.length} line item${orderCart.length === 1 ? '' : 's'} in cart`;
}

async function handleTableChange(event) {
    const tableNumber = event.target.value;
    if (!tableNumber) {
        activeOrder = null;
        renderActiveOrderPanel();
        return;
    }

    await loadActiveOrderForTable(tableNumber);
}

async function loadActiveOrderForTable(tableNumber) {
    try {
        activeOrder = await apiRequest(`/api/orders/open?tableNumber=${encodeURIComponent(tableNumber)}`, {
            headers: buildHeaders(false),
            loadingText: `Loading live order for ${tableNumber}...`
        });
    } catch (error) {
        showMessage(error.message, 'danger');
        activeOrder = null;
    }

    renderActiveOrderPanel();
}

function renderActiveOrderPanel() {
    const tableNumber = document.getElementById('tableNumber').value;
    const title = document.getElementById('activeOrderTitle');
    const badge = document.getElementById('activeOrderStatusBadge');
    const summary = document.getElementById('activeOrderSummary');
    const meta = document.getElementById('activeOrderMeta');
    const items = document.getElementById('activeOrderItems');
    const button = document.getElementById('submitOrderButton');
    const note = document.getElementById('cartNote');

    if (!tableNumber) {
        title.textContent = 'Select a table to continue';
        badge.innerHTML = '';
        summary.textContent = 'Pick a table from master data to check whether a live order is already open.';
        meta.innerHTML = '';
        items.innerHTML = '<div class="active-order-panel__empty">No live order loaded yet.</div>';
        button.textContent = 'Place Order';
        note.textContent = 'New dishes will be added to the selected table.';
        return;
    }

    if (!activeOrder) {
        title.textContent = `No open ticket for ${tableNumber}`;
        badge.innerHTML = '<span class="status-badge status-PENDING">NEW</span>';
        summary.textContent = 'This table does not have a live unpaid order yet. The items in your cart will start a fresh kitchen ticket.';
        meta.innerHTML = `
            <div class="active-order-pill">Table ${tableNumber}</div>
            <div class="active-order-pill">Fresh ticket</div>
        `;
        items.innerHTML = '<div class="active-order-panel__empty">No dishes have been sent for this table yet.</div>';
        button.textContent = 'Create Table Order';
        note.textContent = 'Submitting now will create a new live order for this table.';
        return;
    }

    const totalDishes = activeOrder.items.reduce((sum, item) => sum + item.quantity, 0);
    title.textContent = `${activeOrder.orderCode} is live for ${activeOrder.tableNumber}`;
    badge.innerHTML = statusBadge(activeOrder.status);
    summary.textContent = 'This table already has an active unpaid order. Any new dishes in your cart will be appended to the same running order.';
    meta.innerHTML = `
        <div class="active-order-pill">${activeOrder.customerName || 'Walk-in guest'}</div>
        <div class="active-order-pill">${totalDishes} dish${totalDishes === 1 ? '' : 'es'} sent</div>
        <div class="active-order-pill">Opened ${formatDate(activeOrder.createdAt)}</div>
        <div class="active-order-pill">${currency(activeOrder.totalAmount)} live total</div>
    `;
    items.innerHTML = activeOrder.items.map(item => `
        <div class="active-order-item">
            <div class="active-order-item__copy">
                <div class="active-order-item__name">${item.menuItemName}</div>
                <div class="active-order-item__meta">${item.quantity} x ${currency(item.price)} = ${currency(item.lineTotal)}</div>
            </div>
            <div class="active-order-item__status">${statusBadge(item.status)}</div>
        </div>
    `).join('');
    button.textContent = `Add Items To ${activeOrder.orderCode}`;
    note.textContent = 'Only the new dishes in your cart will be added now. Existing live dishes stay on the same ticket.';
}

function removeCartItem(index) {
    orderCart.splice(index, 1);
    renderCart();
}

async function submitOrder(event) {
    event.preventDefault();
    const tableNumber = document.getElementById('tableNumber').value.trim().toUpperCase();

    if (!tableNumber) {
        showMessage('Please select a table from the master list.', 'danger');
        return;
    }

    if (orderCart.length === 0) {
        showMessage('Please add at least one item before placing the order.', 'danger');
        return;
    }

    const payload = {
        tableNumber,
        items: orderCart.map(item => ({
            menuItemId: item.menuItemId,
            quantity: item.quantity
        }))
    };

    try {
        const response = await apiRequest('/api/orders', {
            method: 'POST',
            headers: buildHeaders(),
            body: JSON.stringify(payload),
            loadingText: 'Placing order...'
        });
        showMessage(`Items saved to ${response.orderCode} for table ${response.tableNumber}. Total now: ${currency(response.totalAmount)}`);
        activeOrder = response;
        orderCart = [];
        document.querySelectorAll('#menuCatalog input[type="number"]').forEach(input => {
            input.value = '1';
        });
        renderCart();
        renderActiveOrderPanel();
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

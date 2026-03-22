document.addEventListener('DOMContentLoaded', async () => {
    renderNavbar('waiter-dashboard');
    const user = requireRoles(['WAITER']);
    if (!user) {
        return;
    }

    document.getElementById('waiterWelcome').textContent = `${user.name}'s section`;
    await loadWaiterDashboard(user);
});

async function loadWaiterDashboard(user) {
    try {
        const [tables, orders] = await Promise.all([
            apiRequest('/api/tables', { headers: buildHeaders(false), loadingText: 'Loading assigned tables...' }),
            apiRequest('/api/orders', { headers: buildHeaders(false), loadingText: 'Loading section orders...' })
        ]);

        const assignedTables = tables.filter(table => (user.assignedTableNumbers || []).includes(table.tableNumber));
        const readyDishes = orders.flatMap(order => order.items
            .filter(item => item.status === 'COMPLETED')
            .map(item => ({
                orderId: order.id,
                orderCode: order.orderCode,
                tableNumber: order.tableNumber,
                item
            })));

        document.getElementById('waiterAssignedCount').textContent = assignedTables.length;
        document.getElementById('waiterOrderCount').textContent = orders.length;
        document.getElementById('waiterOccupiedCount').textContent = assignedTables.filter(table => table.occupied).length;
        document.getElementById('waiterReadyQueue').innerHTML = readyDishes.map(entry => `
            <div class="active-order-item">
                <div class="active-order-item__copy">
                    <div class="active-order-item__name">${entry.item.menuItemName}</div>
                    <div class="active-order-item__meta">${entry.tableNumber} - ${entry.orderCode} - ${entry.item.quantity} qty</div>
                </div>
                <div class="d-flex align-items-center gap-2 flex-wrap">
                    ${statusBadge(entry.item.status)}
                    <button class="btn btn-sm btn-dark rounded-pill px-3" onclick="markDishServed('${entry.orderId}', '${entry.item.id}')">Mark Served</button>
                </div>
            </div>
        `).join('') || '<div class="empty-state"><div class="empty-state__title">No dishes are waiting for service</div><div class="empty-state__text">Kitchen-complete dishes will appear here for handoff.</div></div>';

        document.getElementById('waiterTableCards').innerHTML = assignedTables.map(table => {
            const activeOrders = orders.filter(order => order.tableNumber === table.tableNumber).length;
            return `
                <article class="floor-table ${table.occupied ? 'floor-table--occupied' : 'floor-table--free'}">
                    <div class="floor-table__number">${table.tableNumber}</div>
                    <div class="floor-table__meta">${table.capacity} seats</div>
                    <div class="floor-table__state">${table.occupied ? 'Occupied' : 'Free'}</div>
                    <div class="text-white-50 small mt-2">${activeOrders} active orders</div>
                </article>
            `;
        }).join('') || '<div class="empty-state"><div class="empty-state__title">No assigned tables</div><div class="empty-state__text">Ask admin to assign your service zone.</div></div>';
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

async function markDishServed(orderId, itemId) {
    try {
        await apiRequest(`/api/orders/${orderId}/items/${itemId}/status`, {
            method: 'PATCH',
            headers: buildHeaders(),
            body: JSON.stringify({ status: 'SERVED' }),
            loadingText: 'Marking dish served...'
        });
        showMessage('Dish handed over to the table.');
        await loadWaiterDashboard(getLoggedInUser());
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

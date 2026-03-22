document.addEventListener('DOMContentLoaded', async () => {
    renderNavbar('kitchen-dashboard');
    const user = requireRoles(['KITCHEN']);
    if (!user) {
        return;
    }

    document.getElementById('kitchenWelcome').textContent = `${user.name}'s kitchen board`;
    document.getElementById('kitchenQueueBody').addEventListener('click', handleKitchenActions);
    await loadKitchenDashboard();
});

async function loadKitchenDashboard() {
    try {
        const orders = await apiRequest('/api/orders', {
            headers: buildHeaders(false),
            loadingText: 'Loading kitchen queue...'
        });

        const dishQueue = orders.flatMap(order => order.items
            .filter(item => ['PENDING', 'PREPARING'].includes(item.status))
            .map(item => ({
            orderId: order.id,
            orderCode: order.orderCode,
            tableNumber: order.tableNumber,
            customerName: order.customerName,
            createdAt: order.createdAt,
            item
        }))).sort((left, right) => new Date(left.createdAt) - new Date(right.createdAt));

        const pendingItems = dishQueue.filter(entry => entry.item.status === 'PENDING');
        const preparingItems = dishQueue.filter(entry => entry.item.status === 'PREPARING');

        document.getElementById('kitchenPendingCount').textContent = pendingItems.length;
        document.getElementById('kitchenPreparingCount').textContent = preparingItems.length;
        document.getElementById('kitchenActiveOrderCount').textContent = new Set(dishQueue.map(entry => entry.orderId)).size;

        document.getElementById('kitchenQueueBody').innerHTML = dishQueue.map(entry => `
            <article class="kds-card">
                <div class="kds-card__head">
                    <div>
                        <div class="eyebrow">${entry.orderCode}</div>
                        <h3 class="kds-card__title">${entry.item.menuItemName}</h3>
                    </div>
                    ${statusBadge(entry.item.status)}
                </div>
                <div class="kds-card__meta">
                    <span class="table-id">${entry.tableNumber}</span>
                    <span>${entry.customerName}</span>
                    <span>${entry.item.quantity} qty</span>
                    <span>${formatDate(entry.createdAt)}</span>
                </div>
                <div class="kds-card__actions">
                    ${entry.item.status === 'PENDING'
                        ? `<button class="btn btn-sm btn-outline-dark rounded-pill px-3" data-action="kitchen-status" data-order-id="${entry.orderId}" data-item-id="${entry.item.id}" data-status="PREPARING">Start Preparing</button>`
                        : ''}
                    <button class="btn btn-sm btn-dark rounded-pill px-3" data-action="kitchen-status" data-order-id="${entry.orderId}" data-item-id="${entry.item.id}" data-status="COMPLETED">
                        ${entry.item.status === 'PREPARING' ? 'Mark Ready For Service' : 'Mark Done'}
                    </button>
                </div>
            </article>
        `).join('') || '<div class="empty-state"><div class="empty-state__title">No active kitchen dishes right now.</div><div class="empty-state__text">New dishes will appear here in FIFO order.</div></div>';
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

function handleKitchenActions(event) {
    const button = event.target.closest('button[data-action="kitchen-status"]');
    if (!button) {
        return;
    }
    updateKitchenDishStatus(button.dataset.orderId, button.dataset.itemId, button.dataset.status);
}

async function updateKitchenDishStatus(orderId, itemId, status) {
    try {
        await apiRequest(`/api/orders/${orderId}/items/${itemId}/status`, {
            method: 'PATCH',
            headers: buildHeaders(),
            body: JSON.stringify({ status }),
            loadingText: 'Updating kitchen dish...'
        });
        showMessage(`Dish moved to ${status}.`);
        await loadKitchenDashboard();
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

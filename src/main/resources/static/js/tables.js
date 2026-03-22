let visibleTablesCache = [];
let editingTableId = null;

document.addEventListener('DOMContentLoaded', async () => {
    renderNavbar('tables');
    const user = requireRoles(['ADMIN', 'WAITER', 'CASHIER']);
    if (!user) {
        return;
    }

    if (user.role !== 'ADMIN') {
        document.getElementById('tableFormPanel').classList.add('hidden');
        document.getElementById('tableRegistryPanel')?.classList.add('hidden');
    } else {
        document.getElementById('tableForm').addEventListener('submit', saveTable);
        document.getElementById('cancelTableEditButton').addEventListener('click', resetTableForm);
    }

    document.getElementById('tableFloor').addEventListener('click', handleTableActions);
    document.getElementById('tableListBody').addEventListener('click', handleTableActions);
    await loadTables();
});

async function loadTables() {
    try {
        const user = getLoggedInUser();
        const tables = await apiRequest('/api/tables', {
            headers: buildHeaders(false),
            loadingText: 'Loading table layout...'
        });

        visibleTablesCache = user.role === 'WAITER'
            ? tables.filter(table => (user.assignedTableNumbers || []).includes(table.tableNumber))
            : tables;

        renderTableCards(user);
        renderTableRegistry(user);
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

function renderTableCards(user) {
    document.getElementById('tableFloor').innerHTML = visibleTablesCache.map(table => `
        <article class="floor-table ${table.occupied ? 'floor-table--occupied' : 'floor-table--free'}">
            <div class="floor-table__number">${table.tableNumber}</div>
            <div class="floor-table__meta">${table.capacity} seats</div>
            <div class="floor-table__state">${table.occupied ? 'Occupied' : 'Free'}</div>
            <div class="d-flex gap-2 flex-wrap justify-content-center mt-3">
                <button class="btn btn-sm btn-outline-light rounded-pill" data-action="toggle-table" data-table-id="${table.id}" data-occupied="${!table.occupied}">
                    Mark ${table.occupied ? 'Free' : 'Occupied'}
                </button>
                ${user.role === 'ADMIN' ? `
                    <button class="btn btn-sm btn-outline-dark rounded-pill" data-action="edit-table" data-table-id="${table.id}">Edit</button>
                    <button class="btn btn-sm btn-outline-danger rounded-pill" data-action="delete-table" data-table-id="${table.id}">Delete</button>
                ` : ''}
            </div>
        </article>
    `).join('') || '<div class="empty-state"><div class="empty-state__title">No tables available</div><div class="empty-state__text">Create tables as admin to start assigning service zones.</div></div>';
}

function renderTableRegistry(user) {
    const host = document.getElementById('tableListBody');
    if (!host) {
        return;
    }

    host.innerHTML = visibleTablesCache.map(table => `
        <tr>
            <td><span class="table-id">${table.tableNumber}</span></td>
            <td>${table.capacity} seats</td>
            <td>${statusBadge(table.occupied ? 'COMPLETED' : 'PENDING')}</td>
            <td>
                <div class="d-flex gap-2 flex-wrap">
                    <button class="btn btn-sm btn-outline-light rounded-pill" data-action="toggle-table" data-table-id="${table.id}" data-occupied="${!table.occupied}">
                        ${table.occupied ? 'Mark Free' : 'Mark Occupied'}
                    </button>
                    ${user.role === 'ADMIN' ? `
                        <button class="btn btn-sm btn-outline-dark rounded-pill" data-action="edit-table" data-table-id="${table.id}">Edit</button>
                        <button class="btn btn-sm btn-outline-danger rounded-pill" data-action="delete-table" data-table-id="${table.id}">Delete</button>
                    ` : ''}
                </div>
            </td>
        </tr>
    `).join('') || '<tr><td colspan="4" class="text-center text-muted py-4">No tables available.</td></tr>';
}

function handleTableActions(event) {
    const toggleButton = event.target.closest('button[data-action="toggle-table"]');
    if (toggleButton) {
        toggleTableStatus(toggleButton.dataset.tableId, toggleButton.dataset.occupied === 'true');
        return;
    }

    const editButton = event.target.closest('button[data-action="edit-table"]');
    if (editButton) {
        startTableEdit(editButton.dataset.tableId);
        return;
    }

    const deleteButton = event.target.closest('button[data-action="delete-table"]');
    if (deleteButton) {
        deleteTable(deleteButton.dataset.tableId);
    }
}

function startTableEdit(tableId) {
    const table = visibleTablesCache.find(entry => entry.id === tableId);
    if (!table) {
        return;
    }

    editingTableId = table.id;
    document.getElementById('tableFormMode').textContent = 'Edit table';
    document.getElementById('tableFormTitle').textContent = `Update ${table.tableNumber}`;
    document.getElementById('tableSubmitButton').textContent = 'Save Table';
    document.getElementById('cancelTableEditButton').classList.remove('hidden');
    document.getElementById('tableNumberInput').value = table.tableNumber;
    document.getElementById('tableCapacityInput').value = table.capacity;
    document.getElementById('tableOccupiedInput').checked = table.occupied;
    document.getElementById('tableNumberInput').focus();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function resetTableForm() {
    editingTableId = null;
    document.getElementById('tableForm').reset();
    document.getElementById('tableCapacityInput').value = 4;
    document.getElementById('tableFormMode').textContent = 'Admin tools';
    document.getElementById('tableFormTitle').textContent = 'Create Table';
    document.getElementById('tableSubmitButton').textContent = 'Create Table';
    document.getElementById('cancelTableEditButton').classList.add('hidden');
}

async function saveTable(event) {
    event.preventDefault();
    try {
        const payload = {
            tableNumber: document.getElementById('tableNumberInput').value.trim(),
            capacity: Number(document.getElementById('tableCapacityInput').value),
            occupied: document.getElementById('tableOccupiedInput').checked
        };

        if (editingTableId) {
            await apiRequest(`/api/tables/${editingTableId}`, {
                method: 'PUT',
                headers: buildHeaders(),
                body: JSON.stringify(payload),
                loadingText: 'Updating table...'
            });
            showMessage('Table updated successfully.');
        } else {
            await apiRequest('/api/tables', {
                method: 'POST',
                headers: buildHeaders(),
                body: JSON.stringify(payload),
                loadingText: 'Creating table...'
            });
            showMessage('Table created successfully.');
        }

        resetTableForm();
        await loadTables();
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

async function toggleTableStatus(tableId, occupied) {
    try {
        await apiRequest(`/api/tables/${tableId}/status`, {
            method: 'PATCH',
            headers: buildHeaders(),
            body: JSON.stringify({ occupied }),
            loadingText: 'Updating table status...'
        });
        showMessage(`Table marked ${occupied ? 'occupied' : 'free'}.`);
        await loadTables();
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

async function deleteTable(tableId) {
    const table = visibleTablesCache.find(entry => entry.id === tableId);
    if (!table || !window.confirm(`Delete table ${table.tableNumber}?`)) {
        return;
    }

    try {
        await apiRequest(`/api/tables/${tableId}`, {
            method: 'DELETE',
            headers: buildHeaders(false),
            loadingText: 'Deleting table...'
        });
        showMessage('Table deleted successfully.');
        if (editingTableId === tableId) {
            resetTableForm();
        }
        await loadTables();
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

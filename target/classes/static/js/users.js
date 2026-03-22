let currentUsers = [];
let masterTables = [];
let editingUserId = null;

document.addEventListener('DOMContentLoaded', async () => {
    renderNavbar('users');
    if (!adminOnly()) {
        return;
    }

    document.getElementById('userForm').addEventListener('submit', saveStaffUser);
    document.getElementById('cancelUserEditButton').addEventListener('click', resetUserForm);
    document.getElementById('usersTableBody').addEventListener('click', handleUserActions);
    document.getElementById('userRole').addEventListener('change', syncAssignedTableState);
    await Promise.all([loadMasterTables(), loadUsers()]);
});

async function loadMasterTables() {
    try {
        masterTables = await apiRequest('/api/tables', {
            headers: buildHeaders(false),
            loadingText: 'Loading table master data...'
        });
        renderAssignedTablesPicker([]);
    } catch (error) {
        masterTables = [];
        renderAssignedTablesPicker([]);
        showToast(error.message, 'danger');
    }
}

function renderAssignedTablesPicker(selectedTables) {
    const host = document.getElementById('assignedTablesPicker');
    host.innerHTML = masterTables.map(table => `
        <label class="selection-chip ${selectedTables.includes(table.tableNumber) ? 'selection-chip--active' : ''}">
            <input
                type="checkbox"
                value="${table.tableNumber}"
                class="selection-chip__input"
                ${selectedTables.includes(table.tableNumber) ? 'checked' : ''}
            >
            <span>${table.tableNumber}</span>
        </label>
    `).join('') || '<div class="text-muted small">No tables available in master data yet.</div>';

    host.querySelectorAll('.selection-chip__input').forEach(input => {
        input.addEventListener('change', () => {
            input.closest('.selection-chip').classList.toggle('selection-chip--active', input.checked);
        });
    });

    syncAssignedTableState();
}

function syncAssignedTableState() {
    const isWaiter = document.getElementById('userRole').value === 'WAITER';
    const host = document.getElementById('assignedTablesPicker');
    host.classList.toggle('selection-grid--disabled', !isWaiter);
    host.querySelectorAll('input').forEach(input => {
        input.disabled = !isWaiter;
    });
}

async function loadUsers() {
    try {
        currentUsers = await apiRequest('/api/users', {
            headers: buildHeaders(false),
            loadingText: 'Loading staff directory...'
        });

        document.getElementById('usersTableBody').innerHTML = currentUsers.map(user => `
            <tr>
                <td>${user.name}</td>
                <td>${user.email || '-'}</td>
                <td>${statusBadge(user.role)}</td>
                <td>${(user.assignedTableNumbers || []).join(', ') || '<span class="text-muted">-</span>'}</td>
                <td>
                    <div class="d-flex gap-2 flex-wrap">
                        <button class="btn btn-sm btn-outline-dark rounded-pill px-3" data-action="edit-user" data-user-id="${user.id}">Edit</button>
                        <button class="btn btn-sm btn-outline-danger rounded-pill px-3" data-action="delete-user" data-user-id="${user.id}">Delete</button>
                    </div>
                </td>
            </tr>
        `).join('') || '<tr><td colspan="5" class="text-center text-muted py-4">No users found.</td></tr>';

        document.getElementById('userCount').textContent = currentUsers.length;
        document.getElementById('waiterCount').textContent = currentUsers.filter(user => user.role === 'WAITER').length;
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

function handleUserActions(event) {
    const editButton = event.target.closest('button[data-action="edit-user"]');
    if (editButton) {
        startUserEdit(editButton.dataset.userId);
        return;
    }

    const deleteButton = event.target.closest('button[data-action="delete-user"]');
    if (deleteButton) {
        deleteUser(deleteButton.dataset.userId);
    }
}

function startUserEdit(userId) {
    const user = currentUsers.find(entry => entry.id === userId);
    if (!user) {
        return;
    }

    editingUserId = user.id;
    document.getElementById('userFormTitle').textContent = 'Update staff user';
    document.getElementById('userFormMode').textContent = 'Edit account';
    document.getElementById('userSubmitButton').textContent = 'Save Changes';
    document.getElementById('cancelUserEditButton').classList.remove('hidden');
    document.getElementById('userName').value = user.name;
    document.getElementById('userEmail').value = user.email || '';
    document.getElementById('userPassword').value = '';
    document.getElementById('userPassword').required = false;
    document.getElementById('userPasswordHelp').textContent = 'Leave blank to keep the current password.';
    document.getElementById('userRole').value = user.role;
    renderAssignedTablesPicker(user.assignedTableNumbers || []);
}

function resetUserForm() {
    editingUserId = null;
    document.getElementById('userForm').reset();
    document.getElementById('userFormTitle').textContent = 'Register staff user';
    document.getElementById('userFormMode').textContent = 'Create account';
    document.getElementById('userSubmitButton').textContent = 'Create Staff Account';
    document.getElementById('cancelUserEditButton').classList.add('hidden');
    document.getElementById('userPassword').required = true;
    document.getElementById('userPasswordHelp').textContent = 'Minimum 6 characters.';
    document.getElementById('userRole').value = 'WAITER';
    renderAssignedTablesPicker([]);
}

function getSelectedAssignedTables() {
    return Array.from(document.querySelectorAll('#assignedTablesPicker input:checked'))
        .map(input => input.value);
}

async function saveStaffUser(event) {
    event.preventDefault();

    const role = document.getElementById('userRole').value;
    const payload = {
        name: document.getElementById('userName').value.trim(),
        email: document.getElementById('userEmail').value.trim(),
        password: document.getElementById('userPassword').value.trim() || null,
        role,
        assignedTableNumbers: role === 'WAITER' ? getSelectedAssignedTables() : []
    };

    try {
        if (editingUserId) {
            await apiRequest(`/api/users/${editingUserId}`, {
                method: 'PUT',
                headers: buildHeaders(),
                body: JSON.stringify(payload),
                loadingText: 'Updating staff account...'
            });
            showMessage('Staff account updated successfully.');
        } else {
            await apiRequest('/api/users', {
                method: 'POST',
                headers: buildHeaders(),
                body: JSON.stringify(payload),
                loadingText: 'Creating staff account...'
            });
            showMessage('Staff account created successfully.');
        }

        resetUserForm();
        await loadUsers();
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

async function deleteUser(userId) {
    const user = currentUsers.find(entry => entry.id === userId);
    if (!user || !window.confirm(`Delete ${user.name}'s account?`)) {
        return;
    }

    try {
        await apiRequest(`/api/users/${userId}`, {
            method: 'DELETE',
            headers: buildHeaders(false),
            loadingText: 'Deleting staff account...'
        });
        showMessage('Staff account deleted successfully.');
        if (editingUserId === userId) {
            resetUserForm();
        }
        await loadUsers();
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

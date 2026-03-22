let editingMenuId = null;
let currentMenuItems = [];

document.addEventListener('DOMContentLoaded', async () => {
    renderNavbar('menu');
    if (!adminOnly()) {
        return;
    }

    document.getElementById('menuForm').addEventListener('submit', handleSaveMenuItem);
    document.getElementById('searchInput').addEventListener('input', loadMenuItems);
    document.getElementById('categoryFilter').addEventListener('input', loadMenuItems);
    document.getElementById('cancelEditBtn').addEventListener('click', resetForm);
    document.getElementById('menuTableBody').addEventListener('click', handleTableActions);
    document.getElementById('itemImageFile').addEventListener('change', previewSelectedImage);

    await loadMenuItems();
});

async function loadMenuItems() {
    try {
        const search = document.getElementById('searchInput').value.trim();
        const category = document.getElementById('categoryFilter').value.trim();
        const params = new URLSearchParams();
        if (search) params.append('search', search);
        if (category) params.append('category', category);

    const query = params.toString();
    currentMenuItems = await apiRequest(`/api/menu-items${query ? `?${query}` : ''}`);
        document.getElementById('menuTableBody').innerHTML = currentMenuItems.map(item => `
            <tr>
                <td><span class="table-id">${item.id}</span></td>
                <td>
                    <div class="d-flex align-items-center gap-3">
                        <img src="${item.imageUrl}" alt="${item.name}" class="menu-thumb">
                        <div>
                            <div class="fw-semibold">${item.name}</div>
                            <div class="small text-muted">${item.type}</div>
                        </div>
                    </div>
                </td>
                <td>${currency(item.price)}</td>
                <td>${item.category}</td>
                <td>${item.available ? '<span class="status-badge status-COMPLETED">Available</span>' : '<span class="status-badge status-PENDING">Sold Out</span>'}</td>
                <td>
                    <div class="d-flex gap-2">
                        <button class="btn btn-sm btn-outline-dark rounded-pill px-3" data-action="edit" data-id="${item.id}">Edit</button>
                        <button class="btn btn-sm btn-outline-danger rounded-pill px-3" data-action="delete" data-id="${item.id}">Delete</button>
                    </div>
                </td>
            </tr>
        `).join('') || '<tr><td colspan="6" class="text-center text-muted py-4">No menu items found.</td></tr>';

        document.getElementById('menuCount').textContent = currentMenuItems.length;
        document.getElementById('categoryCount').textContent = new Set(currentMenuItems.map(item => item.category)).size;
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

function handleTableActions(event) {
    const button = event.target.closest('button[data-action]');
    if (!button) {
        return;
    }

    const itemId = button.dataset.id;
    if (button.dataset.action === 'edit') {
        editMenuItem(itemId);
    }
    if (button.dataset.action === 'delete') {
        deleteMenuItem(itemId);
    }
}

async function handleSaveMenuItem(event) {
    event.preventDefault();
    try {
        const payload = {
            name: document.getElementById('itemName').value.trim(),
            price: document.getElementById('itemPrice').value,
            type: document.getElementById('itemType').value.trim(),
            category: document.getElementById('itemCategory').value.trim(),
            imageUrl: document.getElementById('itemImage').value.trim(),
            description: document.getElementById('itemDescription').value.trim(),
            available: document.getElementById('itemAvailable').checked
        };

        if (!payload.imageUrl) {
            const uploadedPath = await uploadSelectedImage();
            payload.imageUrl = uploadedPath;
        }

        const isEditing = editingMenuId !== null;
        const url = isEditing ? `/api/menu-items/${editingMenuId}` : '/api/menu-items';
        const method = isEditing ? 'PUT' : 'POST';

        await apiRequest(url, {
            method,
            headers: buildHeaders(),
            body: JSON.stringify(payload)
        });

        showMessage(`Menu item ${isEditing ? 'updated' : 'created'} successfully.`);
        resetForm();
        await loadMenuItems();
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

function editMenuItem(itemId) {
    const item = currentMenuItems.find(menuItem => menuItem.id === itemId);
    if (!item) {
        showMessage('Menu item not found.', 'danger');
        return;
    }

    editingMenuId = item.id;
    document.getElementById('itemName').value = item.name;
    document.getElementById('itemPrice').value = item.price;
    document.getElementById('itemType').value = item.type;
    document.getElementById('itemCategory').value = item.category;
    document.getElementById('itemImage').value = item.imageUrl;
    document.getElementById('itemDescription').value = item.description || '';
    document.getElementById('itemAvailable').checked = item.available;
    renderImagePreview(item.imageUrl);
    document.getElementById('formTitle').textContent = 'Update Menu Item';
    document.getElementById('saveBtn').textContent = 'Update Item';
    document.getElementById('cancelEditBtn').classList.remove('hidden');
}

async function deleteMenuItem(id) {
    if (!confirm('Delete this menu item?')) {
        return;
    }

    try {
        const response = await apiRequest(`/api/menu-items/${id}`, {
            method: 'DELETE',
            headers: buildHeaders(false)
        });
        showMessage(response.message);
        await loadMenuItems();
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

function resetForm() {
    editingMenuId = null;
    document.getElementById('menuForm').reset();
    document.getElementById('itemImage').value = '';
    document.getElementById('itemDescription').value = '';
    document.getElementById('itemImageFile').value = '';
    clearImagePreview();
    document.getElementById('formTitle').textContent = 'Add New Menu Item';
    document.getElementById('saveBtn').textContent = 'Save Item';
    document.getElementById('cancelEditBtn').classList.add('hidden');
}

async function uploadSelectedImage() {
    const fileInput = document.getElementById('itemImageFile');
    const file = fileInput.files[0];
    if (!file) {
        throw new Error('Please select an image file.');
    }

    const formData = new FormData();
    formData.append('file', file);

    const response = await apiRequest('/api/uploads/menu-image', {
        method: 'POST',
        headers: buildHeaders(false),
        body: formData,
        loadingText: 'Uploading image...'
    });

    document.getElementById('itemImage').value = response.path;
    renderImagePreview(response.path);
    return response.path;
}

function previewSelectedImage(event) {
    const file = event.target.files[0];
    if (!file) {
        clearImagePreview();
        return;
    }

    const imagePath = URL.createObjectURL(file);
    renderImagePreview(imagePath);
    document.getElementById('itemImage').value = '';
}

function renderImagePreview(src) {
    const preview = document.getElementById('itemImagePreview');
    preview.src = src;
    preview.classList.remove('hidden');
}

function clearImagePreview() {
    const preview = document.getElementById('itemImagePreview');
    preview.src = '';
    preview.classList.add('hidden');
}

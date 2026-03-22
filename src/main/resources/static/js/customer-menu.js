let guestMenuItems = [];
let guestFilteredItems = [];

document.addEventListener('DOMContentLoaded', async () => {
    renderGuestHero();
    const tableNumber = syncGuestTableFromUrl();
    const tableInput = document.getElementById('guestTableNumber');
    tableInput.value = tableNumber;
    document.getElementById('guestTableLabel').textContent = tableNumber || 'Select';

    document.getElementById('guestCategoryFilter').addEventListener('change', applyGuestFilters);
    document.getElementById('guestSearchFilter').addEventListener('input', applyGuestFilters);
    tableInput.addEventListener('change', updateGuestTableSelection);
    tableInput.addEventListener('blur', updateGuestTableSelection);

    await loadGuestMenu();
});

async function loadGuestMenu() {
    const host = document.getElementById('guestMenuCatalog');
    host.innerHTML = `
        <div class="empty-state">
            <div class="empty-state__title">Loading menu</div>
            <div class="empty-state__text">Fetching the latest available dishes for your table.</div>
        </div>
    `;

    try {
        const allItems = await apiRequest('/api/menu-items', { loadingText: 'Loading guest menu...' });
        guestMenuItems = allItems
            .filter(item => item.available)
            .sort((left, right) => (left.category || '').localeCompare(right.category || '') || left.name.localeCompare(right.name));

        document.getElementById('guestMenuCount').textContent = guestMenuItems.length;

        const categories = [...new Set(guestMenuItems.map(item => item.category).filter(Boolean))].sort();
        document.getElementById('guestCategoryFilter').innerHTML = `
            <option value="">All categories</option>
            ${categories.map(category => `<option value="${category}">${category}</option>`).join('')}
        `;

        applyGuestFilters();
    } catch (error) {
        host.innerHTML = `
            <div class="empty-state">
                <div class="empty-state__title">Menu unavailable</div>
                <div class="empty-state__text">${error.message}</div>
            </div>
        `;
        showToast(error.message, 'danger');
    }
}

function updateGuestTableSelection() {
    const tableNumber = document.getElementById('guestTableNumber').value.trim().toUpperCase();
    if (!tableNumber) {
        document.getElementById('guestTableLabel').textContent = 'Select';
        return;
    }

    setGuestTableNumber(tableNumber);
    document.getElementById('guestTableNumber').value = tableNumber;
    document.getElementById('guestTableLabel').textContent = tableNumber;
    renderGuestHero();
}

function applyGuestFilters() {
    const category = document.getElementById('guestCategoryFilter').value;
    const search = document.getElementById('guestSearchFilter').value.trim().toLowerCase();

    guestFilteredItems = guestMenuItems.filter(item => {
        const matchCategory = !category || item.category === category;
        const matchSearch = !search
            || item.name.toLowerCase().includes(search)
            || (item.description || '').toLowerCase().includes(search);
        return matchCategory && matchSearch;
    });

    renderGuestMenuCards();
}

function renderGuestMenuCards() {
    const host = document.getElementById('guestMenuCatalog');
    if (guestFilteredItems.length === 0) {
        host.innerHTML = `
            <div class="empty-state">
                <div class="empty-state__title">No dishes found</div>
                <div class="empty-state__text">Try a different category or search term.</div>
            </div>
        `;
        return;
    }

    host.innerHTML = guestFilteredItems.map(item => `
        <article class="menu-card menu-card--interactive menu-card--customer">
            <div class="menu-card__image-wrap">
                <img src="${item.imageUrl}" alt="${item.name}" class="menu-card__image">
                <span class="availability-badge availability-badge--live">${item.category || 'Menu'}</span>
            </div>
            <div class="menu-card__body">
                <div class="menu-card__top">
                    <div>
                        <h5 class="mb-1">${item.name}</h5>
                        <div class="text-white-50 small">${item.category || 'Chef selection'}</div>
                    </div>
                    <div class="price-tag">${currency(item.price)}</div>
                </div>
                <p class="menu-card__text mt-3">${item.description || 'Freshly prepared for dine-in guests.'}</p>
            </div>
        </article>
    `).join('');
}

const API_BASE = '';
const STORAGE_KEY = 'restaurantUser';
const GUEST_TABLE_KEY = 'guestTableNumber';
const APP_ROUTES = {
    login: '/login',
    guestMenu: '/guest-menu',
    admin: '/admin',
    staff: '/staff',
    waiter: '/waiter',
    kitchen: '/kitchen',
    cashier: '/cashier',
    menuStudio: '/menu-studio',
    tables: '/tables',
    manualOrder: '/manual-order',
    orderBoard: '/order-board'
};
const ROLE_HOME = {
    ADMIN: APP_ROUTES.admin,
    WAITER: APP_ROUTES.waiter,
    KITCHEN: APP_ROUTES.kitchen,
    CASHIER: APP_ROUTES.cashier,
    CUSTOMER: APP_ROUTES.guestMenu
};
let loadingCount = 0;
let notificationsStarted = false;

ensureGlobalUi();

function ensureGlobalUi() {
    const mountUi = () => {
        if (!document.querySelector('.noise-overlay')) {
            const noiseOverlay = document.createElement('div');
            noiseOverlay.className = 'noise-overlay';
            noiseOverlay.setAttribute('aria-hidden', 'true');
            document.body.appendChild(noiseOverlay);
        }

        if (!document.getElementById('toastHost')) {
            const toastHost = document.createElement('div');
            toastHost.id = 'toastHost';
            toastHost.className = 'toast-host';
            document.body.appendChild(toastHost);
        }

        if (!document.getElementById('loadingOverlay')) {
            const loadingOverlay = document.createElement('div');
            loadingOverlay.id = 'loadingOverlay';
            loadingOverlay.className = 'loading-overlay hidden';
            loadingOverlay.innerHTML = `
                <div class="loading-card">
                    <div class="loading-ring"></div>
                    <div class="loading-text mt-3" id="loadingText">Loading...</div>
                </div>
            `;
            document.body.appendChild(loadingOverlay);
        }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', mountUi, { once: true });
    } else {
        mountUi();
    }
}

function getLoggedInUser() {
    const userJson = localStorage.getItem(STORAGE_KEY);
    return userJson ? JSON.parse(userJson) : null;
}

function setLoggedInUser(user) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
}

function getDefaultPageForRole(role) {
    return ROLE_HOME[role] || APP_ROUTES.admin;
}

function logout() {
    localStorage.removeItem(STORAGE_KEY);
    window.location.href = APP_ROUTES.login;
}

function requireAuth() {
    const user = getLoggedInUser();
    if (!user || !user.token) {
        localStorage.removeItem(STORAGE_KEY);
        window.location.href = APP_ROUTES.login;
        return null;
    }
    return user;
}

function requireRoles(roles, redirectPage = null) {
    const user = requireAuth();
    if (!user) {
        return null;
    }

    if (!roles.includes(user.role)) {
        showToast('You do not have access to this page.', 'danger');
        setTimeout(() => {
            window.location.href = redirectPage || getDefaultPageForRole(user.role);
        }, 1100);
        return null;
    }

    return user;
}

function adminOnly() {
    return requireRoles(['ADMIN']);
}

function buildHeaders(includeJson = true) {
    const user = getLoggedInUser();
    const headers = {};

    if (includeJson) {
        headers['Content-Type'] = 'application/json';
    }

    if (user) {
        if (user.token) {
            headers['Authorization'] = `Bearer ${user.token}`;
        }
        if (user.id) {
            headers['X-USER-ID'] = user.id;
        }
        if (user.role) {
            headers['X-USER-ROLE'] = user.role;
        }
    }

    return headers;
}

async function apiRequest(url, options = {}) {
    setLoading(true, options.loadingText || 'Working on your request...');
    try {
        const response = await fetch(`${API_BASE}${url}`, options);
        const contentType = response.headers.get('content-type') || '';
        const data = contentType.includes('application/json') ? await response.json() : null;

        if (!response.ok) {
            if (response.status === 401) {
                localStorage.removeItem(STORAGE_KEY);
            }
            throw new Error(data?.message || 'Request failed');
        }

        return data;
    } finally {
        setLoading(false);
    }
}

function setLoading(show, message = 'Loading...') {
    const overlay = document.getElementById('loadingOverlay');
    const loadingText = document.getElementById('loadingText');
    if (!overlay || !loadingText) {
        return;
    }

    if (show) {
        loadingCount += 1;
        loadingText.textContent = message;
        overlay.classList.remove('hidden');
        return;
    }

    loadingCount = Math.max(loadingCount - 1, 0);
    if (loadingCount === 0) {
        overlay.classList.add('hidden');
        loadingText.textContent = 'Loading...';
    }
}

function currency(value) {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2
    }).format(Number(value || 0));
}

function formatDate(value) {
    if (!value) {
        return '-';
    }
    return new Date(value).toLocaleString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function statusBadge(status) {
    return `<span class="status-badge status-${status}">${status}</span>`;
}

function showToast(message, type = 'info') {
    const host = document.getElementById('toastHost');
    if (!host) {
        return;
    }

    const toast = document.createElement('div');
    toast.className = `toast-message toast-message--${type}`;
    toast.innerHTML = `
        <div class="toast-message__title">${type === 'danger' ? 'Something went wrong' : type === 'success' ? 'Success' : 'Notification'}</div>
        <div class="toast-message__body">${message}</div>
    `;
    host.appendChild(toast);
    requestAnimationFrame(() => toast.classList.add('is-visible'));
    setTimeout(() => {
        toast.classList.remove('is-visible');
        setTimeout(() => toast.remove(), 240);
    }, 3500);
}

function showMessage(message, type = 'success', targetId = 'messageBox') {
    showToast(message, type);
    const target = document.getElementById(targetId);
    if (!target) {
        return;
    }

    target.innerHTML = `<div class="alert alert-${type} alert-dismissible fade show shadow-sm rounded-4 border-0" role="alert">
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    </div>`;
}

function initials(name) {
    return name
        .split(' ')
        .filter(Boolean)
        .slice(0, 2)
        .map(part => part[0].toUpperCase())
        .join('');
}

function renderTracking(status) {
    const steps = ['PENDING', 'PREPARING', 'COMPLETED'];
    const currentIndex = steps.indexOf(status);
    return `
        <div class="tracking-steps">
            ${steps.map((step, index) => `<span class="tracking-step ${index <= currentIndex ? 'is-active' : ''}">${step}</span>`).join('')}
        </div>
    `;
}

function getGuestTableNumber() {
    return localStorage.getItem(GUEST_TABLE_KEY) || '';
}

function setGuestTableNumber(tableNumber) {
    localStorage.setItem(GUEST_TABLE_KEY, tableNumber.trim().toUpperCase());
}

function syncGuestTableFromUrl() {
    const params = new URLSearchParams(window.location.search);
    const queryTable = params.get('table');
    if (queryTable) {
        setGuestTableNumber(queryTable);
    }
    return getGuestTableNumber();
}

function renderNavbar(activePage) {
    const user = requireAuth();
    if (!user) {
        return;
    }

    const links = [
        { id: 'dashboard', label: 'Admin', href: APP_ROUTES.admin, roles: ['ADMIN'] },
        { id: 'users', label: 'Staff', href: APP_ROUTES.staff, roles: ['ADMIN'] },
        { id: 'waiter-dashboard', label: 'Waiter', href: APP_ROUTES.waiter, roles: ['WAITER'] },
        { id: 'kitchen-dashboard', label: 'Kitchen', href: APP_ROUTES.kitchen, roles: ['KITCHEN'] },
        { id: 'cashier-dashboard', label: 'Cashier', href: APP_ROUTES.cashier, roles: ['CASHIER'] },
        { id: 'menu', label: 'Menu Studio', href: APP_ROUTES.menuStudio, roles: ['ADMIN'] },
        { id: 'tables', label: 'Tables', href: APP_ROUTES.tables, roles: ['ADMIN', 'WAITER', 'CASHIER'] },
        { id: 'create-order', label: 'Manual Order', href: APP_ROUTES.manualOrder, roles: ['ADMIN', 'WAITER', 'CASHIER'] },
        { id: 'orders', label: 'Order Board', href: APP_ROUTES.orderBoard, roles: ['ADMIN', 'WAITER', 'KITCHEN', 'CASHIER'] }
    ].filter(link => link.roles.includes(user.role));

    document.getElementById('appNavbar').innerHTML = `
        <header class="topbar">
            <div class="container-fluid topbar__inner">
                <a href="${getDefaultPageForRole(user.role)}" class="brand">
                    <img src="/images/noirtable-logo.svg" alt="NoirTable" class="brand__logo">
                    <span class="brand__name">Noir<span class="brand__accent">Table</span></span>
                </a>
                <nav class="topbar__nav">
                    ${links.map(link => `<a href="${link.href}" class="nav-link ${activePage === link.id ? 'active' : ''}">${link.label}</a>`).join('')}
                </nav>
                <div class="topbar__user">
                    <div class="user-avatar">${initials(user.name)}</div>
                    <span class="user-name">${user.name}</span>
                    <span class="user-role">${user.role.replaceAll('_', ' ')}</span>
                    <button onclick="logout()" class="btn-logout">Sign Out</button>
                </div>
            </div>
        </header>
    `;

    connectNotifications();
}

function renderGuestHero() {
    const currentTable = syncGuestTableFromUrl();
    const host = document.getElementById('guestHeroBar');
    if (!host) {
        return;
    }

    host.innerHTML = `
        <header class="guest-topbar">
            <div class="container guest-topbar__inner">
                <div>
                    <div class="brand brand--guest">
                        <img src="/images/noirtable-logo.svg" alt="NoirTable" class="brand__logo">
                        <span class="brand__name">Noir<span class="brand__accent">Table</span></span>
                    </div>
                    <div class="guest-topbar__meta">Scan, browse, and order from your table in a few taps.</div>
                </div>
                <div class="guest-topbar__actions">
                    <div class="guest-topbar__table">Table ${currentTable || 'Not selected'}</div>
                    <a href="${APP_ROUTES.login}" class="staff-entry-link">Staff Sign In</a>
                </div>
            </div>
        </header>
    `;
}

function animatePageLoad() {
    document.querySelectorAll('.metric-card, .chart-panel, .menu-card, .table-card, .catalog-tile, .data-table tbody tr, .modern-table tbody tr, .action-tile, .floor-table').forEach((element, index) => {
        element.style.opacity = '0';
        element.style.transform = 'translateY(20px)';
        setTimeout(() => {
            element.style.transition = 'opacity 400ms ease, transform 400ms ease';
            element.style.opacity = '1';
            element.style.transform = 'translateY(0)';
        }, 80 + index * 40);
    });
}

document.addEventListener('DOMContentLoaded', () => setTimeout(animatePageLoad, 100));

function connectNotifications() {
    const user = getLoggedInUser();
    if (!user || notificationsStarted) {
        return;
    }
    notificationsStarted = true;

    connectSocket(client => {
        client.subscribe('/topic/orders', message => {
            const payload = JSON.parse(message.body);
            showToast(payload.message, 'info');
        });
        client.subscribe(`/topic/roles/${user.role}`, message => {
            const payload = JSON.parse(message.body);
            showToast(payload.message, 'info');
        });
    });
}

function connectGuestOrderChannel(orderId, onMessage) {
    if (!orderId) {
        return;
    }

    connectSocket(client => {
        client.subscribe(`/topic/order/${orderId}`, message => {
            const payload = JSON.parse(message.body);
            onMessage(payload);
        });
    });
}

function connectSocket(onConnect) {
    const connect = () => {
        const socket = new SockJS('/ws');
        const stompClient = Stomp.over(socket);
        stompClient.debug = () => {};
        stompClient.connect({}, () => onConnect(stompClient));
    };

    if (window.SockJS && window.Stomp) {
        connect();
        return;
    }

    const scripts = [
        'https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js',
        'https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js'
    ];

    loadScriptsSequentially(scripts, connect);
}

function loadScriptsSequentially(urls, onComplete) {
    const [head, ...tail] = urls;
    if (!head) {
        onComplete();
        return;
    }

    const script = document.createElement('script');
    script.src = head;
    script.onload = () => loadScriptsSequentially(tail, onComplete);
    document.body.appendChild(script);
}

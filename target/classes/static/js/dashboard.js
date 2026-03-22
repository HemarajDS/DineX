let statusChart;
let revenueChart;
let categoryChart;
let monthlyRevenueChart;
let todayOrdersChart;

document.addEventListener('DOMContentLoaded', async () => {
    renderNavbar('dashboard');
    const user = requireRoles(['ADMIN']);
    if (!user) {
        return;
    }

    document.getElementById('welcomeText').textContent = `Command center, ${user.name}`;
    document.getElementById('roleText').textContent = 'Monitor revenue, kitchen progress, staffing flow, and menu performance from one admin cockpit.';

    renderRoleHighlights();
    await loadDashboard();
});

function renderRoleHighlights() {
    const actions = [
        { title: 'Manage menu', text: 'Update pricing, categories, and plating images.', href: '/menu-studio' },
        { title: 'Manage staff', text: 'Register waiters, kitchen, cashier, and admin support users.', href: '/staff' },
        { title: 'Review orders', text: 'Oversee the full order lifecycle.', href: '/order-board' },
        { title: 'Table map', text: 'Inspect floor occupancy and service coverage.', href: '/tables' }
    ];

    document.getElementById('quickActionGrid').innerHTML = actions.map(action => `
        <a class="action-tile" href="${action.href}">
            <div class="action-tile__title">${action.title}</div>
            <div class="action-tile__text">${action.text}</div>
        </a>
    `).join('');
}

async function loadDashboard() {
    try {
        const summary = await apiRequest('/api/dashboard/summary', {
            headers: buildHeaders(false)
        });

        document.getElementById('menuItemCount').textContent = summary.menuItemCount;
        document.getElementById('orderCount').textContent = summary.orderCount;
        document.getElementById('revenueCount').textContent = currency(summary.totalRevenue);
        document.getElementById('averageOrderValue').textContent = currency(summary.averageOrderValue);
        document.getElementById('todayOrderCount').textContent = summary.todayOrderCount;
        document.getElementById('todayRevenue').textContent = currency(summary.todayRevenue);
        document.getElementById('staffCount').textContent = summary.staffCount;
        document.getElementById('customerCount').textContent = summary.customerCount;
        document.getElementById('tableCount').textContent = summary.tableCount;
        document.getElementById('occupiedTableCount').textContent = summary.occupiedTableCount;

        renderRecentOrders(summary.recentOrders);
        renderStatusHighlights(summary.statusBreakdown);
        renderCharts(summary);
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

function renderRecentOrders(orders) {
    document.getElementById('recentOrdersBody').innerHTML = orders.map(order => `
        <tr>
            <td><span class="table-id">#${order.id}</span></td>
            <td>${order.customerName}</td>
            <td><span class="table-id">${order.tableNumber}</span></td>
            <td>${currency(order.totalAmount)}</td>
            <td>${statusBadge(order.status)}</td>
            <td>${formatDate(order.createdAt)}</td>
        </tr>
    `).join('') || '<tr><td colspan="6" class="text-center text-muted py-4">No orders found.</td></tr>';
}

function renderStatusHighlights(statusBreakdown) {
    document.getElementById('statusHighlights').innerHTML = statusBreakdown.map(item => `
        <div class="mini-stat">
            <div class="mini-stat__label">${item.status}</div>
            <div class="mini-stat__value">${item.count}</div>
        </div>
    `).join('');
}

function renderCharts(summary) {
    const statusLabels = summary.statusBreakdown.map(item => item.status);
    const statusData = summary.statusBreakdown.map(item => item.count);
    const revenueLabels = summary.revenueTrend.map(item => item.date);
    const revenueData = summary.revenueTrend.map(item => Number(item.revenue));
    const monthlyLabels = summary.monthlyRevenueTrend.map(item => item.month);
    const monthlyData = summary.monthlyRevenueTrend.map(item => Number(item.revenue));
    const todayLabels = summary.todayHourTrend.map(item => item.hour);
    const todayData = summary.todayHourTrend.map(item => item.count);
    const categoryLabels = summary.menuCategoryBreakdown.map(item => item.category);
    const categoryData = summary.menuCategoryBreakdown.map(item => item.count);

    statusChart?.destroy();
    revenueChart?.destroy();
    categoryChart?.destroy();
    monthlyRevenueChart?.destroy();
    todayOrdersChart?.destroy();

    statusChart = new Chart(document.getElementById('statusChart'), {
        type: 'doughnut',
        data: {
            labels: statusLabels,
            datasets: [{
                data: statusData,
                backgroundColor: ['#d4a54f', '#c7773e', '#4e7d6e'],
                borderWidth: 0
            }]
        },
        options: {
            plugins: {
                legend: {
                    position: 'bottom'
                }
            }
        }
    });

    revenueChart = new Chart(document.getElementById('revenueChart'), {
        type: 'line',
        data: {
            labels: revenueLabels,
            datasets: [{
                label: 'Revenue',
                data: revenueData,
                borderColor: '#d4a54f',
                backgroundColor: 'rgba(212, 165, 79, 0.18)',
                fill: true,
                tension: 0.35,
                pointBackgroundColor: '#f8d27a'
            }]
        },
        options: {
            scales: {
                y: {
                    beginAtZero: true
                }
            },
            plugins: {
                legend: {
                    display: false
                }
            }
        }
    });

    monthlyRevenueChart = new Chart(document.getElementById('monthlyRevenueChart'), {
        type: 'bar',
        data: {
            labels: monthlyLabels,
            datasets: [{
                label: 'Monthly Revenue',
                data: monthlyData,
                backgroundColor: '#b76f3b',
                borderRadius: 12
            }]
        },
        options: {
            scales: {
                y: {
                    beginAtZero: true
                }
            },
            plugins: {
                legend: {
                    display: false
                }
            }
        }
    });

    todayOrdersChart = new Chart(document.getElementById('todayOrdersChart'), {
        type: 'line',
        data: {
            labels: todayLabels,
            datasets: [{
                label: 'Orders',
                data: todayData,
                borderColor: '#4e7d6e',
                backgroundColor: 'rgba(78, 125, 110, 0.18)',
                fill: true,
                tension: 0.3,
                pointRadius: 2
            }]
        },
        options: {
            scales: {
                y: {
                    beginAtZero: true
                }
            },
            plugins: {
                legend: {
                    display: false
                }
            }
        }
    });

    categoryChart = new Chart(document.getElementById('categoryChart'), {
        type: 'bar',
        data: {
            labels: categoryLabels,
            datasets: [{
                label: 'Menu Items',
                data: categoryData,
                backgroundColor: ['#d4a54f', '#b76f3b', '#7a9e7e', '#8a6e4d']
            }]
        },
        options: {
            scales: {
                y: {
                    beginAtZero: true
                }
            },
            plugins: {
                legend: {
                    display: false
                }
            }
        }
    });
}

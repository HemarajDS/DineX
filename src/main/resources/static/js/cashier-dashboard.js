let latestReceipt = null;

document.addEventListener('DOMContentLoaded', async () => {
    renderNavbar('cashier-dashboard');
    const user = requireRoles(['CASHIER']);
    if (!user) {
        return;
    }

    document.getElementById('cashierWelcome').textContent = `${user.name}'s cashier desk`;
    document.getElementById('billingPreviewForm').addEventListener('submit', previewReceipt);
    document.getElementById('closeBillButton').addEventListener('click', closeBill);
    document.getElementById('printReceiptButton').addEventListener('click', printReceipt);
    await loadCashierDashboard();
});

async function loadCashierDashboard() {
    try {
        const [orders, tables] = await Promise.all([
            apiRequest('/api/orders', { headers: buildHeaders(false), loadingText: 'Loading billing queue...' }),
            apiRequest('/api/tables', { headers: buildHeaders(false), loadingText: 'Loading floor occupancy...' })
        ]);

        const unbilledOrders = orders.filter(order => !order.billed);
        const unbilledTotal = unbilledOrders.reduce((sum, order) => sum + Number(order.totalAmount), 0);
        const openTables = [...new Set(unbilledOrders.map(order => order.tableNumber))];

        document.getElementById('cashierUnbilledCount').textContent = unbilledOrders.length;
        document.getElementById('cashierPendingValue').textContent = currency(unbilledTotal);
        document.getElementById('cashierOccupiedCount').textContent = tables.filter(table => table.occupied).length;

        document.getElementById('billTableNumber').innerHTML = [
            '<option value="">Select table</option>',
            ...openTables.map(tableNumber => `<option value="${tableNumber}">${tableNumber}</option>`)
        ].join('');

        document.getElementById('cashierQueueBody').innerHTML = unbilledOrders.map(order => `
            <tr>
                <td><span class="table-id">${order.tableNumber}</span></td>
                <td>${order.customerName}</td>
                <td>${currency(order.totalAmount)}</td>
                <td>${statusBadge(order.status)}</td>
                <td>${formatDate(order.createdAt)}</td>
            </tr>
        `).join('') || '<tr><td colspan="5" class="text-center text-muted py-4">No open bills right now.</td></tr>';
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

async function previewReceipt(event) {
    event.preventDefault();
    const payload = buildBillPayload();
    if (!payload) {
        return;
    }

    try {
        latestReceipt = await apiRequest('/api/orders/billing/preview', {
            method: 'POST',
            headers: buildHeaders(),
            body: JSON.stringify(payload),
            loadingText: 'Preparing receipt preview...'
        });
        renderReceipt(latestReceipt);
        showMessage('Receipt preview ready.');
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

async function closeBill() {
    const payload = buildBillPayload();
    if (!payload) {
        return;
    }

    try {
        latestReceipt = await apiRequest('/api/orders/billing/close', {
            method: 'POST',
            headers: buildHeaders(),
            body: JSON.stringify(payload),
            loadingText: 'Closing bill...'
        });
        renderReceipt(latestReceipt);
        showMessage(`Bill closed for ${latestReceipt.tableNumber}.`);
        await loadCashierDashboard();
    } catch (error) {
        showMessage(error.message, 'danger');
    }
}

function buildBillPayload() {
    const tableNumber = document.getElementById('billTableNumber').value;
    if (!tableNumber) {
        showMessage('Please select a table to prepare the bill.', 'danger');
        return null;
    }

    return {
        tableNumber,
        discountAmount: Number(document.getElementById('discountAmount').value || 0),
        discountPercent: Number(document.getElementById('discountPercent').value || 0),
        gstPercent: Number(document.getElementById('gstPercent').value || 0),
        additionalTaxPercent: Number(document.getElementById('additionalTaxPercent').value || 0),
        serviceChargePercent: Number(document.getElementById('serviceChargePercent').value || 0)
    };
}

function renderReceipt(receipt) {
    const host = document.getElementById('receiptPreview');
    host.innerHTML = `
        <div class="receipt-card">
            <div class="receipt-card__header">
                <div>
                    <div class="eyebrow">Receipt preview</div>
                    <h3 class="section-title mb-1">${receipt.receiptNumber}</h3>
                    <div class="text-muted">Table ${receipt.tableNumber} - ${receipt.orderCount} orders - ${receipt.guestCount} guests</div>
                </div>
                <div class="status-badge status-COMPLETED">READY</div>
            </div>
            <div class="receipt-lines">
                ${receipt.lines.map(line => `
                    <div class="receipt-line">
                        <div>
                            <div class="receipt-line__name">${line.itemName}</div>
                            <div class="receipt-line__meta">${line.quantity} x ${currency(line.unitPrice)}</div>
                        </div>
                        <strong>${currency(line.lineTotal)}</strong>
                    </div>
                `).join('')}
            </div>
            <div class="receipt-totals">
                ${renderReceiptTotal('Subtotal', receipt.subtotal)}
                ${renderReceiptTotal('Discount', receipt.discountAmount)}
                ${renderReceiptTotal(`GST (${receipt.gstPercent}%)`, receipt.gstAmount)}
                ${renderReceiptTotal(`Additional Tax (${receipt.additionalTaxPercent}%)`, receipt.additionalTaxAmount)}
                ${renderReceiptTotal(`Service Charge (${receipt.serviceChargePercent}%)`, receipt.serviceChargeAmount)}
                <div class="receipt-total receipt-total--grand">
                    <span>Grand Total</span>
                    <strong>${currency(receipt.grandTotal)}</strong>
                </div>
            </div>
        </div>
    `;
}

function renderReceiptTotal(label, amount) {
    return `
        <div class="receipt-total">
            <span>${label}</span>
            <strong>${currency(amount)}</strong>
        </div>
    `;
}

function printReceipt() {
    if (!latestReceipt) {
        showMessage('Generate a receipt preview first.', 'danger');
        return;
    }

    const receiptHtml = document.getElementById('receiptPreview').innerHTML;
    const printWindow = window.open('', '_blank', 'width=720,height=860');
    if (!printWindow) {
        showMessage('Unable to open the print window.', 'danger');
        return;
    }

    printWindow.document.write(`
        <html>
        <head>
            <title>${latestReceipt.receiptNumber}</title>
            <style>
                body { font-family: Arial, sans-serif; padding: 24px; color: #111; }
                .receipt-card { border: 1px solid #ddd; border-radius: 16px; padding: 24px; }
                .receipt-card__header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
                .receipt-line, .receipt-total { display: flex; justify-content: space-between; gap: 16px; padding: 10px 0; border-bottom: 1px solid #eee; }
                .receipt-total--grand { font-size: 1.1rem; font-weight: 700; border-bottom: none; padding-top: 16px; }
                .receipt-line__name { font-weight: 700; }
                .receipt-line__meta { color: #666; font-size: 0.92rem; margin-top: 4px; }
            </style>
        </head>
        <body>${receiptHtml}</body>
        </html>
    `);
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
}

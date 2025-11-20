document.addEventListener('DOMContentLoaded', () => {
    // --- CONFIG ---
    const API_BASE_URL = 'http://127.0.0.1:5002/api';
    const TOKEN_KEY = 'smart_inventory_token';
    const USER_KEY = 'smart_inventory_user';

    // --- SECURITY CHECK ---
    const token = localStorage.getItem(TOKEN_KEY);
    const userJson = localStorage.getItem(USER_KEY);

    if (!token || !userJson) {
        alert("You are not logged in. Redirecting to login page.");
        window.location.href = 'auth.html';
        return;
    }

    let user;
    try {
        user = JSON.parse(userJson);
    } catch (e) {
        localStorage.clear();
        window.location.href = 'auth.html';
        return;
    }

    // --- DATA STORAGE ---
    let allProducts = [];
    let allSuppliers = [];
    let allClients = [];
    let allTransactions = [];

    // --- INITIALIZE PAGE ---
    function initPage() {
        // Populate user info
        document.getElementById('sidebar-username').textContent = user.username;
        document.getElementById('sidebar-avatar').textContent = user.username.charAt(0).toUpperCase();
        document.getElementById('sidebar-role').textContent = user.role || 'User';

        // Set current date
        document.getElementById('current-date-display').textContent = new Date().toLocaleDateString('en-US', {
            year: 'numeric', month: 'long', day: 'numeric'
        });

        // Bind events
        document.getElementById('logout-btn').addEventListener('click', handleLogout);
        setupTabs();
        setupFormHandlers();
        setupSaleItemManagement();
        setupHistoryFilters();

        // Load all data
        loadAllData();
    }

    // --- AUTH FUNCTIONS ---
    function handleLogout(e) {
        e.preventDefault();
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        showAlert('You have been logged out successfully.', 'success');
        setTimeout(() => {
            window.location.href = 'auth.html';
        }, 1000);
    }

    // --- TAB SYSTEM ---
    function setupTabs() {
        const tabs = document.querySelectorAll('.tab');
        tabs.forEach(tab => {
            tab.addEventListener('click', () => {
                const tabName = tab.getAttribute('data-tab');
                switchTab(tabName);
            });
        });
    }

    function switchTab(tabName) {
        // Update tab buttons
        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');

        // Update tab content
        document.querySelectorAll('.tab-content').forEach(tc => tc.classList.remove('active'));
        document.getElementById(`${tabName}-tab`).classList.add('active');
    }

    // --- FORM HANDLERS ---
    function setupFormHandlers() {
        document.getElementById('sale-form').addEventListener('submit', processSale);
        document.getElementById('purchase-form').addEventListener('submit', processPurchase);
        document.getElementById('return-form').addEventListener('submit', processReturn);
        document.getElementById('removal-form').addEventListener('submit', processRemoval);
    }

    // --- SALE ITEM MANAGEMENT ---
    function setupSaleItemManagement() {
        // Setup initial sale item row
        setupSaleItemRow();

        // Add item button
        document.getElementById('add-sale-item').addEventListener('click', addSaleItemRow);

        // Discount change
        document.getElementById('sale-discount').addEventListener('input', calculateSaleTotal);
    }

    function setupSaleItemRow() {
        const container = document.querySelector('.sale-items-container');
        const rows = container.querySelectorAll('.sale-item-row');

        // If no rows exist, keep the template
        if (rows.length === 0) return;

        rows.forEach(row => {
            const select = row.querySelector('.sale-stock-select');
            const quantity = row.querySelector('.sale-quantity');
            const removeBtn = row.querySelector('.remove-sale-item');

            // Populate stock options
            populateSaleStockOptions(select);

            // Handle stock selection
            select.addEventListener('change', function() {
                const option = this.options[this.selectedIndex];
                const price = parseFloat(option.getAttribute('data-price')) || 0;
                row.querySelector('.sale-unit-price').value = price.toFixed(2);
                calculateRowSubtotal(row);
            });

            // Handle quantity change
            quantity.addEventListener('input', () => calculateRowSubtotal(row));

            // Handle remove
            removeBtn.addEventListener('click', () => {
                if (container.querySelectorAll('.sale-item-row').length > 1) {
                    row.remove();
                    calculateSaleTotal();
                } else {
                    showAlert('At least one item is required', 'error');
                }
            });
        });
    }

    function addSaleItemRow() {
        const container = document.querySelector('.sale-items-container');
        const template = document.getElementById('sale-item-template');
        const newRow = template.cloneNode(true);
        newRow.removeAttribute('id');
        container.appendChild(newRow);
        setupSaleItemRow();
    }

    function calculateRowSubtotal(row) {
        const quantity = parseFloat(row.querySelector('.sale-quantity').value) || 0;
        const unitPrice = parseFloat(row.querySelector('.sale-unit-price').value) || 0;
        const subtotal = quantity * unitPrice;
        row.querySelector('.sale-subtotal').value = subtotal.toFixed(2);
        calculateSaleTotal();
    }

    function calculateSaleTotal() {
        const rows = document.querySelectorAll('.sale-item-row');
        let subtotal = 0;

        rows.forEach(row => {
            const rowSubtotal = parseFloat(row.querySelector('.sale-subtotal').value) || 0;
            subtotal += rowSubtotal;
        });

        const discount = parseFloat(document.getElementById('sale-discount').value) || 0;
        const discountAmount = subtotal * (discount / 100);
        const total = subtotal - discountAmount;

        document.getElementById('sale-subtotal-display').textContent = `$${subtotal.toFixed(2)}`;
        document.getElementById('sale-discount-display').textContent = `-$${discountAmount.toFixed(2)}`;
        document.getElementById('sale-total-display').textContent = `$${total.toFixed(2)}`;
    }

    function populateSaleStockOptions(select) {
        select.innerHTML = '<option value="">Select stock item...</option>';
        allProducts.filter(p => (p.currentStock || 0) > 0).forEach(product => {
            const stock = product.currentStock || 0;
            const price = product.sellingPrice || 0;
            select.innerHTML += `<option value="${product.id}" data-price="${price}" data-qty="${stock}">${product.name} - $${price} (Available: ${stock})</option>`;
        });
    }

    // --- HISTORY FILTERS ---
    function setupHistoryFilters() {
        document.getElementById('history-search').addEventListener('input', filterHistory);
        document.getElementById('history-type-filter').addEventListener('change', filterHistory);
        document.getElementById('history-date-filter').addEventListener('change', filterHistory);
    }

    function filterHistory() {
        const searchTerm = document.getElementById('history-search').value.toLowerCase();
        const typeFilter = document.getElementById('history-type-filter').value;
        const dateFilter = document.getElementById('history-date-filter').value;

        let filtered = allTransactions;

        if (searchTerm) {
            filtered = filtered.filter(t =>
                (t.item_name || '').toLowerCase().includes(searchTerm) ||
                (t.details || '').toLowerCase().includes(searchTerm)
            );
        }

        if (typeFilter && typeFilter !== 'all') {
            filtered = filtered.filter(t => t.type === typeFilter);
        }

        if (dateFilter) {
            filtered = filtered.filter(t => {
                const transDate = new Date(t.date).toDateString();
                const filterDate = new Date(dateFilter).toDateString();
                return transDate === filterDate;
            });
        }

        renderTransactionHistory(filtered);
    }

    // --- LOAD ALL DATA ---
    async function loadAllData() {
        try {
            const [products, suppliers, clients] = await Promise.all([
                fetchFromApi('/products'),
                fetchFromApi('/suppliers'),
                fetchFromApi('/clients').catch(() => [])
            ]);

            allProducts = products;
            allSuppliers = suppliers;
            allClients = clients;

            // Populate dropdowns
            populateDropdowns();

            // Fetch transaction history
            await fetchTransactionHistory();

            // Update metrics
            updateMetrics();

        } catch (error) {
            console.error('Failed to load data:', error);
            showAlert('Could not load data. Please refresh the page.', 'error');
        }
    }

    // --- API HELPER ---
    async function fetchFromApi(endpoint) {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });
        if (!response.ok) {
            throw new Error(`Failed to fetch ${endpoint}: ${response.statusText}`);
        }
        return await response.json();
    }

    // --- POPULATE DROPDOWNS ---
    function populateDropdowns() {
        // Client dropdown
        const clientSelect = document.getElementById('sale-client');
        clientSelect.innerHTML = '<option value="">Walk-in Customer</option>';
        allClients.forEach(client => {
            clientSelect.innerHTML += `<option value="${client.id}">${client.name}</option>`;
        });

        // Purchase product dropdown
        const purchaseProductSelect = document.getElementById('purchase-product');
        purchaseProductSelect.innerHTML = '<option value="">Select a product...</option>';
        allProducts.forEach(prod => {
            purchaseProductSelect.innerHTML += `<option value="${prod.id}">${prod.name}</option>`;
        });

        // Purchase supplier dropdown
        const purchaseSupplierSelect = document.getElementById('purchase-supplier');
        purchaseSupplierSelect.innerHTML = '<option value="">Select a supplier...</option>';
        allSuppliers.forEach(sup => {
            purchaseSupplierSelect.innerHTML += `<option value="${sup.id}">${sup.name}</option>`;
        });

        // Return stock dropdown
        const returnStockSelect = document.getElementById('return-stock');
        returnStockSelect.innerHTML = '<option value="">Select returned item...</option>';
        allProducts.forEach(prod => {
            returnStockSelect.innerHTML += `<option value="${prod.id}">${prod.name} (Stock: ${prod.currentStock || 0})</option>`;
        });

        // Removal stock dropdown
        const removalStockSelect = document.getElementById('removal-stock');
        removalStockSelect.innerHTML = '<option value="">Select item to remove...</option>';
        allProducts.filter(p => (p.currentStock || 0) > 0).forEach(prod => {
            removalStockSelect.innerHTML += `<option value="${prod.id}">${prod.name} (Stock: ${prod.currentStock || 0})</option>`;
        });

        // Setup sale stock options for existing rows
        document.querySelectorAll('.sale-stock-select').forEach(select => {
            populateSaleStockOptions(select);
        });
    }

    // --- PROCESS SALE ---
    async function processSale(e) {
        e.preventDefault();

        // Collect sale items
        const items = [];
        let isValid = true;

        document.querySelectorAll('.sale-item-row').forEach(row => {
            const productId = row.querySelector('.sale-stock-select').value;
            const quantity = parseInt(row.querySelector('.sale-quantity').value);
            const unitPrice = parseFloat(row.querySelector('.sale-unit-price').value);

            if (!productId || !quantity || !unitPrice) {
                isValid = false;
                return;
            }

            items.push({
                product: { id: parseInt(productId) },
                quantity: quantity,
                unitPrice: unitPrice,
                discount: 0
            });
        });

        if (!isValid || items.length === 0) {
            showAlert('Please fill in all item details', 'error');
            return;
        }

        const clientId = parseInt(document.getElementById('sale-client').value);
        const discount = parseFloat(document.getElementById('sale-discount').value) || 0;

        // Calculate totals
        const subtotal = items.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0);
        const discountAmount = subtotal * (discount / 100);
        const total = subtotal - discountAmount;

        const saleData = {
            client: clientId ? { id: clientId } : null,
            saleReference: `SALE-${Date.now()}`,
            status: 'PAID',
            paymentMethod: document.getElementById('sale-payment-method').value.toUpperCase(),
            items: items,
            totalAmount: total
        };

        try {
            const response = await fetch(`${API_BASE_URL}/sales`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(saleData)
            });

            const result = await response.json();
            displayResponse('sale-response', result, response.status);

            if (response.ok) {
                showAlert('Sale completed successfully!', 'success');
                e.target.reset();

                // Reset sale items
                const container = document.querySelector('.sale-items-container');
                container.innerHTML = '';
                const template = document.getElementById('sale-item-template');
                const newRow = template.cloneNode(true);
                newRow.removeAttribute('id');
                container.appendChild(newRow);
                setupSaleItemRow();
                calculateSaleTotal();

                await loadAllData();
            } else {
                showAlert(result.error || 'Failed to record sale', 'error');
            }
        } catch (error) {
            displayResponse('sale-response', { message: `Network error: ${error.message}` }, 0);
            showAlert('Network error occurred', 'error');
        }
    }

    // --- PROCESS PURCHASE (ADD STOCK) ---
    async function processPurchase(e) {
        e.preventDefault();

        const productId = parseInt(document.getElementById('purchase-product').value);
        const supplierId = parseInt(document.getElementById('purchase-supplier').value);
        const quantity = parseInt(document.getElementById('purchase-quantity').value);
        const costPrice = parseFloat(document.getElementById('purchase-cost-price').value) || 0;
        const sellingPrice = parseFloat(document.getElementById('purchase-selling-price').value);

        if (!productId || !supplierId || !quantity || !sellingPrice) {
            showAlert('Please fill in all required fields', 'error');
            return;
        }

        try {
            // Update product with prices and supplier
            await fetch(`${API_BASE_URL}/products/${productId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    costPrice: costPrice,
                    sellingPrice: sellingPrice,
                    supplier: { id: supplierId }
                })
            });

            // Add stock
            const stockResponse = await fetch(`${API_BASE_URL}/stock/add`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    productId: productId,
                    quantity: quantity,
                    reason: 'Purchase from supplier',
                    reference: `PO-${Date.now()}`
                })
            });

            const result = await stockResponse.json();
            displayResponse('purchase-response', result, stockResponse.status);

            if (stockResponse.ok) {
                showAlert('Stock added successfully!', 'success');
                e.target.reset();
                await loadAllData();
            } else {
                showAlert(result.error || 'Failed to add stock', 'error');
            }

        } catch (error) {
            displayResponse('purchase-response', { message: `Network error: ${error.message}` }, 0);
            showAlert('Network error occurred', 'error');
        }
    }

    // --- PROCESS RETURN ---
    async function processReturn(e) {
        e.preventDefault();

        const productId = parseInt(document.getElementById('return-stock').value);
        const quantity = parseInt(document.getElementById('return-quantity').value);
        const reason = document.getElementById('return-reason').value;
        const restock = document.getElementById('return-restock').checked;
        const notes = document.getElementById('return-notes').value;

        if (!productId || !quantity || !reason) {
            showAlert('Please select an item, quantity, and reason', 'error');
            return;
        }

        try {
            if (restock) {
                const response = await fetch(`${API_BASE_URL}/stock/add`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({
                        productId: productId,
                        quantity: quantity,
                        reason: `Return: ${reason}. ${notes}`,
                        reference: `RETURN-${Date.now()}`
                    })
                });

                const result = await response.json();
                displayResponse('return-response', result, response.status);

                if (response.ok) {
                    showAlert('Return processed and stock added back successfully!', 'success');
                    e.target.reset();
                    document.getElementById('return-restock').checked = true;
                    await loadAllData();
                } else {
                    showAlert(result.error || 'Failed to process return', 'error');
                }
            } else {
                showAlert('Return noted (stock not restocked)', 'success');
                e.target.reset();
                document.getElementById('return-restock').checked = true;
            }

        } catch (error) {
            displayResponse('return-response', { message: `Network error: ${error.message}` }, 0);
            showAlert('Network error occurred', 'error');
        }
    }

    // --- PROCESS REMOVAL ---
    async function processRemoval(e) {
        e.preventDefault();

        const productId = parseInt(document.getElementById('removal-stock').value);
        const quantity = parseInt(document.getElementById('removal-quantity').value);
        const reason = document.getElementById('removal-reason').value;
        const notes = document.getElementById('removal-notes').value;

        if (!productId || !quantity || !reason) {
            showAlert('Please select an item, quantity, and reason', 'error');
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/stock/remove`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    productId: productId,
                    quantity: quantity,
                    reason: `${reason}. ${notes}`,
                    reference: `REMOVAL-${Date.now()}`
                })
            });

            const result = await response.json();
            displayResponse('removal-response', result, response.status);

            if (response.ok) {
                showAlert('Stock removed successfully!', 'success');
                e.target.reset();
                await loadAllData();
            } else {
                showAlert(result.error || 'Failed to remove stock', 'error');
            }

        } catch (error) {
            displayResponse('removal-response', { message: `Network error: ${error.message}` }, 0);
            showAlert('Network error occurred', 'error');
        }
    }

    // --- FETCH TRANSACTION HISTORY ---
    async function fetchTransactionHistory() {
        try {
            const response = await fetch(`${API_BASE_URL}/sales`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) throw new Error('Failed to fetch sales history');

            const sales = await response.json();

            // Format the API data
            allTransactions = sales.map(sale => {
                let itemName = "Multiple Items";
                if (sale.items && sale.items.length > 0) {
                    const firstItem = sale.items[0];
                    itemName = firstItem.product?.name || "Unknown Product";
                    if (sale.items.length > 1) {
                        itemName += ` + ${sale.items.length - 1} more`;
                    }
                }

                return {
                    id: sale.id,
                    type: 'sale',
                    item_name: itemName,
                    quantity: sale.items?.reduce((sum, item) => sum + (item.quantity || 0), 0) || 0,
                    amount: parseFloat(sale.totalAmount || 0),
                    date: sale.saleDate,
                    user: 'System',
                    details: `Payment: ${sale.paymentMethod || 'N/A'} | Status: ${sale.status}`
                };
            });

            renderTransactionHistory(allTransactions);

        } catch (error) {
            console.error('Error fetching transaction history:', error);
            showAlert('Failed to load transaction history', 'error');
            renderTransactionHistory([]);
        }
    }

    // --- RENDER TRANSACTION HISTORY ---
    function renderTransactionHistory(transactions) {
        const container = document.getElementById('transaction-history');

        if (transactions.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <span class="empty-icon">📋</span>
                    <p>No transactions found</p>
                </div>
            `;
            return;
        }

        container.innerHTML = transactions.map((trans, index) => {
            const typeIcon = {
                'sale': '🛒',
                'purchase': '📥',
                'return': '↩️',
                'removal': '🗑️'
            }[trans.type] || '📄';

            const typeColor = {
                'sale': 'green',
                'purchase': 'blue',
                'return': 'orange',
                'removal': 'red'
            }[trans.type] || 'gray';

            return `
                <div class="transaction-item" style="animation: slideInLeft 0.3s ease ${index * 0.03}s forwards; opacity: 0;">
                    <div class="transaction-icon icon-${typeColor}">${typeIcon}</div>
                    <div class="transaction-details">
                        <div class="transaction-name">${trans.item_name}</div>
                        <div class="transaction-meta">
                            ${new Date(trans.date).toLocaleString()} | ${trans.type.toUpperCase()} | Qty: ${trans.quantity}
                        </div>
                        ${trans.details ? `<div class="transaction-details-text">${trans.details}</div>` : ''}
                    </div>
                    <div class="transaction-amount ${trans.type === 'sale' ? 'positive' : trans.type === 'return' ? 'negative' : 'neutral'}">
                        ${trans.type === 'sale' ? '+' : trans.type === 'return' ? '-' : ''}$${trans.amount.toFixed(2)}
                    </div>
                </div>
            `;
        }).join('');
    }

    // --- UPDATE METRICS ---
    function updateMetrics() {
        const sales = allTransactions.filter(t => t.type === 'sale');
        const purchases = allTransactions.filter(t => t.type === 'purchase');
        const returns = allTransactions.filter(t => t.type === 'return');
        const removals = allTransactions.filter(t => t.type === 'removal');

        animateValue('total-sales', 0, sales.length, 1000);
        animateValue('total-purchases', 0, purchases.length, 1000);
        animateValue('total-returns', 0, returns.length, 1000);
        animateValue('total-removals', 0, removals.length, 1000);

        const salesAmount = sales.reduce((sum, t) => sum + t.amount, 0);
        const returnAmount = returns.reduce((sum, t) => sum + t.amount, 0);
        const purchaseItems = purchases.reduce((sum, t) => sum + t.quantity, 0);
        const removalItems = removals.reduce((sum, t) => sum + t.quantity, 0);

        document.getElementById('sales-amount').textContent = `$${salesAmount.toFixed(2)}`;
        document.getElementById('return-amount').textContent = `$${returnAmount.toFixed(2)}`;
        document.getElementById('purchase-items').textContent = `${purchaseItems} items`;
        document.getElementById('removal-items').textContent = `${removalItems} items`;

        // Update notification count
        const lowStockCount = allProducts.filter(p => {
            const stock = p.currentStock || 0;
            return stock > 0 && stock < 10;
        }).length;
        document.getElementById('notification-count').textContent = lowStockCount;
    }

    // --- HELPER FUNCTIONS ---
    function animateValue(elementId, start, end, duration) {
        const element = document.getElementById(elementId);
        if (!element) return;

        let startTimestamp = null;
        const step = (timestamp) => {
            if (!startTimestamp) startTimestamp = timestamp;
            const progress = Math.min((timestamp - startTimestamp) / duration, 1);
            const value = Math.floor(progress * (end - start) + start);
            element.textContent = value;
            if (progress < 1) {
                window.requestAnimationFrame(step);
            }
        };
        window.requestAnimationFrame(step);
    }

    function displayResponse(elementId, data, status) {
        const element = document.getElementById(elementId);
        if (!element) return;
        element.textContent = JSON.stringify(data, null, 2);
        element.className = `response-pre ${status >= 200 && status < 300 ? 'success' : 'error'}`;
        element.style.display = 'block';

        setTimeout(() => {
            element.style.display = 'none';
        }, 5000);
    }

    function showAlert(message, type) {
        const container = document.getElementById('alert-container');
        const alert = document.createElement('div');
        alert.className = `alert alert-${type}`;
        alert.innerHTML = `
            <span class="alert-icon">${type === 'success' ? '✓' : '⚠️'}</span>
            <span>${message}</span>
        `;
        container.appendChild(alert);

        setTimeout(() => {
            alert.style.transition = 'all 0.4s ease';
            alert.style.opacity = '0';
            alert.style.transform = 'translateX(100%)';
            setTimeout(() => alert.remove(), 400);
        }, 3000);
    }

    // --- RUN THE APP ---
    initPage();
});

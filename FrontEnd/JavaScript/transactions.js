document.addEventListener('DOMContentLoaded', () => {

    // --- CONFIG ---
    const API_BASE_URL = 'http://127.0.0.1:5001/api';
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

    // --- GLOBAL DATA CACHE ---
    let allStock = [];
    let allProducts = [];
    let allSuppliers = [];
    let allClients = [];
    let allTransactions = [];

    // --- INITIALIZE PAGE ---
    function initPage() {
        // Populate user info
        document.getElementById('sidebar-username').textContent = user.username;
        document.getElementById('sidebar-avatar').textContent = user.username.charAt(0).toUpperCase();
        document.getElementById('sidebar-role').textContent = 'User';

        // Set current date
        document.getElementById('current-date-display').textContent = new Date().toLocaleDateString('en-US', {
            year: 'numeric', month: 'long', day: 'numeric'
        });

        // --- BIND EVENT LISTENERS ---
        document.getElementById('logout-btn').addEventListener('click', handleLogout);

        // Tab Switching
        document.querySelectorAll('.tab').forEach(tab => {
            tab.addEventListener('click', () => switchTab(tab));
        });

        // Form Submissions
        document.getElementById('sale-form').addEventListener('submit', processSale);
        document.getElementById('purchase-form').addEventListener('submit', processPurchase);
        document.getElementById('return-form').addEventListener('submit', processReturn);
        document.getElementById('removal-form').addEventListener('submit', processRemoval);

        // Sale item management
        document.getElementById('add-sale-item').addEventListener('click', addSaleItemRow);
        
        // Sale calculations
        document.getElementById('sale-discount').addEventListener('input', calculateSaleTotal);

        // History filters
        document.getElementById('history-search')?.addEventListener('input', filterHistory);
        document.getElementById('history-type-filter')?.addEventListener('change', filterHistory);
        document.getElementById('history-date-filter')?.addEventListener('change', filterHistory);

        // Load initial data
        loadAllData();
    }

    // --- AUTH ---
    function handleLogout(e) {
        e.preventDefault();
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        showAlert('You have been logged out successfully.', 'success');
        setTimeout(() => {
            window.location.href = 'auth.html';
        }, 1000);
    }

    // --- TAB CONTROLS ---
    function switchTab(clickedTab) {
        document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
        
        clickedTab.classList.add('active');
        const tabData = clickedTab.getAttribute('data-tab');
        document.getElementById(`${tabData}-tab`).classList.add('active');
    }

    // --- LOAD ALL DATA ---
    async function loadAllData() {
        await Promise.all([
            fetchStock(),
            fetchProducts(),
            fetchSuppliers(),
            fetchClients(),
            fetchTransactionHistory() // This is still mock, we will update it later
        ]);
        updateMetrics(); // This is still mock, we will update it later
        populateDropdowns();
        
        // Ensure first row is set up (it's inside a template tag in transactions.html)
        const firstRow = document.getElementById('sale-item-template');
        if (firstRow) {
             populateSaleStockOptions(firstRow.querySelector('.sale-stock-select'));
             bindSaleItemEvents(firstRow);
        }
    }

    // --- FETCH FUNCTIONS ---
    async function fetchStock() {
        try {
            const response = await fetch(`${API_BASE_URL}/stocks`);
            if (!response.ok) throw new Error('Failed to fetch stock');
            allStock = await response.json();
        } catch (error) {
            console.error('Error fetching stock:', error);
            showAlert('Failed to load stock data', 'error');
        }
    }

    async function fetchProducts() {
        try {
            const response = await fetch(`${API_BASE_URL}/products`);
            if (!response.ok) throw new Error('Failed to fetch products');
            allProducts = await response.json();
        } catch (error) {
            console.error('Error fetching products:', error);
        }
    }

    async function fetchSuppliers() {
        try {
            const response = await fetch(`${API_BASE_URL}/suppliers`);
            if (!response.ok) throw new Error('Failed to fetch suppliers');
            allSuppliers = await response.json();
        } catch (error) {
            console.error('Error fetching suppliers:', error);
        }
    }

    async function fetchClients() {
        try {
            // --- UPDATED: Fetch real clients ---
            const response = await fetch(`${API_BASE_URL}/clients`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) throw new Error('Failed to fetch clients');
            allClients = await response.json();
        } catch (error) {
            console.error('Error fetching clients:', error);
            // Fallback to mock clients if API fails
            allClients = [
                {id: 1, name: 'John Doe (Mock)'},
                {id: 2, name: 'Jane Smith (Mock)'},
            ];
        }
    }

    async function fetchTransactionHistory() {
        try {
            // --- UPDATED: Fetch real sales from the API ---
            const response = await fetch(`${API_BASE_URL}/sales`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) throw new Error('Failed to fetch sales history');
            
            const sales = await response.json();

            // Format the API data to match what the render function expects
            allTransactions = sales.map(sale => {
                // Get the first item to use as a name, or a summary
                let itemName = "Multiple Items";
                if (sale.items.length > 0) {
                    itemName = sale.items[0].product_name || "Unknown Product";
                    if (sale.items.length > 1) {
                         itemName += ` + ${sale.items.length - 1} more`;
                    }
                }

                return {
                    id: sale.id,
                    type: 'sale', // We are only fetching sales for now
                    item_name: itemName,
                    quantity: sale.items.reduce((sum, item) => sum + item.quantity_sold, 0),
                    amount: parseFloat(sale.total_amount),
                    date: sale.sale_date,
                    user: sale.salesperson_username,
                    details: `Payment: ${sale.payment_method}`
                };
            });

            // Note: This history will only show 'sales'
            // A full implementation would also fetch purchases, returns, etc.
            renderTransactionHistory(allTransactions);

        } catch (error) {
            console.error('Error fetching real transaction history:', error);
            showAlert('Failed to load transaction history', 'error');
            // Fallback to empty if API fails
            renderTransactionHistory([]);
        }
    }

    // --- GENERATE MOCK TRANSACTIONS ---
    function generateMockTransactions() {
        const now = new Date();
        return [
            {
                id: 1,
                type: 'sale',
                item_name: 'iPhone 14 Pro',
                quantity: 2,
                amount: 1998.00,
                date: new Date(now - 2 * 60 * 1000).toISOString(),
                user: user.username,
                details: 'Cash payment'
            },
            {
                id: 2,
                type: 'purchase',
                item_name: 'Samsung Galaxy S23',
                quantity: 50,
                amount: 37500.00,
                date: new Date(now - 15 * 60 * 1000).toISOString(),
                user: user.username,
                details: 'From TechSupply Co.'
            },
            {
                id: 3,
                type: 'sale',
                item_name: 'AirPods Pro',
                quantity: 1,
                amount: 249.00,
                date: new Date(now - 60 * 60 * 1000).toISOString(),
                user: user.username,
                details: 'Card payment'
            },
            {
                id: 4,
                type: 'return',
                item_name: 'MacBook Pro',
                quantity: 1,
                amount: 1499.00,
                date: new Date(now - 2 * 60 * 60 * 1000).toISOString(),
                user: user.username,
                details: 'Customer not satisfied'
            },
            {
                id: 5,
                type: 'removal',
                item_name: 'Damaged Cable',
                quantity: 5,
                amount: 0,
                date: new Date(now - 3 * 60 * 60 * 1000).toISOString(),
                user: user.username,
                details: 'Damaged during shipping'
            }
        ];
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

        // Update notification count (low stock)
        const lowStockCount = allStock.filter(s => s.quantity < 10).length;
        document.getElementById('notification-count').textContent = lowStockCount;
    }

    // --- POPULATE DROPDOWNS ---
    function populateDropdowns() {
        // Sale clients
        const saleClientSelect = document.getElementById('sale-client');
        saleClientSelect.innerHTML = '<option value="">Walk-in Customer</option>';
        allClients.forEach(client => {
            saleClientSelect.innerHTML += `<option value="${client.id}">${client.name}</option>`;
        });

        // Purchase products
        const purchaseProductSelect = document.getElementById('purchase-product');
        purchaseProductSelect.innerHTML = '<option value="">Select a product...</option>';
        allProducts.forEach(prod => {
            purchaseProductSelect.innerHTML += `<option value="${prod.id}">${prod.name}</option>`;
        });

        // Purchase suppliers
        const purchaseSupplierSelect = document.getElementById('purchase-supplier');
        purchaseSupplierSelect.innerHTML = '<option value="">Select a supplier...</option>';
        allSuppliers.forEach(sup => {
            purchaseSupplierSelect.innerHTML += `<option value="${sup.id}">${sup.name}</option>`;
        });

        // Return & Removal stock selects
        ['return-stock', 'removal-stock'].forEach(selectId => {
            const select = document.getElementById(selectId);
            select.innerHTML = '<option value="">Select item...</option>';
            allStock.forEach(stock => { // Show all stock, even 0 quantity for returns
                select.innerHTML += `<option value="${stock.id}">${stock.product_name} (Qty: ${stock.quantity})</option>`;
            });
        });
    }

    // --- SALE ITEM MANAGEMENT ---
    function setupSaleItemRow() {
        const template = document.getElementById('sale-item-template');
        
        // Clear existing rows except the template
        const container = document.querySelector('.sale-items-container');
        container.innerHTML = ''; // Clear it
        container.appendChild(template); // Add the template back
        
        // Reset the template's values
        template.querySelectorAll('input, select').forEach(input => {
             if (input.classList.contains('sale-quantity')) {
                input.value = '1';
            } else {
                input.value = '';
            }
        });

        populateSaleStockOptions(template.querySelector('.sale-stock-select'));
        bindSaleItemEvents(template);
    }

    function addSaleItemRow() {
        const container = document.querySelector('.sale-items-container');
        const template = document.getElementById('sale-item-template');
        const newRow = template.cloneNode(true);
        newRow.removeAttribute('id');
        
        // Reset values
        newRow.querySelectorAll('input, select').forEach(input => {
            if (input.classList.contains('sale-quantity')) {
                input.value = '1';
            } else {
                input.value = '';
            }
        });
        
        populateSaleStockOptions(newRow.querySelector('.sale-stock-select'));
        bindSaleItemEvents(newRow);
        container.appendChild(newRow);
        
        showAlert('Item row added', 'success');
    }

    function populateSaleStockOptions(select) {
        select.innerHTML = '<option value="">Select stock item...</option>';
        allStock.filter(s => s.quantity > 0).forEach(stock => {
            select.innerHTML += `<option value="${stock.id}" data-price="${stock.selling_price}" data-qty="${stock.quantity}">${stock.product_name} - $${stock.selling_price} (Available: ${stock.quantity})</option>`;
        });
    }

    function bindSaleItemEvents(row) {
        const stockSelect = row.querySelector('.sale-stock-select');
        const quantityInput = row.querySelector('.sale-quantity');
        const unitPriceInput = row.querySelector('.sale-unit-price');
        const subtotalInput = row.querySelector('.sale-subtotal');
        const removeBtn = row.querySelector('.remove-sale-item');

        stockSelect.addEventListener('change', function() {
            const selected = this.options[this.selectedIndex];
            const price = selected.dataset.price || 0;
            unitPriceInput.value = price;
            quantityInput.max = selected.dataset.qty || 999;
            calculateItemSubtotal(row);
        });

        quantityInput.addEventListener('input', () => calculateItemSubtotal(row));

        removeBtn.addEventListener('click', function() {
            if (document.querySelectorAll('.sale-item-row').length > 1) {
                row.remove();
                calculateSaleTotal();
                showAlert('Item removed', 'success');
            } else {
                showAlert('At least one item is required', 'error');
            }
        });
    }

    function calculateItemSubtotal(row) {
        const quantity = parseFloat(row.querySelector('.sale-quantity').value) || 0;
        const unitPrice = parseFloat(row.querySelector('.sale-unit-price').value) || 0;
        const subtotal = quantity * unitPrice;
        row.querySelector('.sale-subtotal').value = subtotal.toFixed(2);
        calculateSaleTotal();
    }

    function calculateSaleTotal() {
        let subtotal = 0;
        document.querySelectorAll('.sale-item-row').forEach(row => {
            subtotal += parseFloat(row.querySelector('.sale-subtotal').value) || 0;
        });

        const discountPercent = parseFloat(document.getElementById('sale-discount').value) || 0;
        const discountAmount = subtotal * (discountPercent / 100);
        const total = subtotal - discountAmount;

        document.getElementById('sale-subtotal-display').textContent = `$${subtotal.toFixed(2)}`;
        document.getElementById('sale-discount-display').textContent = `-$${discountAmount.toFixed(2)}`;
        document.getElementById('sale-total-display').textContent = `$${total.toFixed(2)}`;
    }

    // --- PROCESS SALE ---
    async function processSale(e) {
        e.preventDefault();

        // Collect sale items
        const items = [];
        let isValid = true;

        document.querySelectorAll('.sale-item-row').forEach(row => {
            const stockId = row.querySelector('.sale-stock-select').value;
            const quantity = parseInt(row.querySelector('.sale-quantity').value);
            const unitPrice = parseFloat(row.querySelector('.sale-unit-price').value);

            if (!stockId || !quantity || !unitPrice) {
                isValid = false;
                return;
            }

            items.push({
                stock_id: parseInt(stockId),
                quantity_sold: quantity,
                unit_price_at_sale: unitPrice
            });
        });

        if (!isValid || items.length === 0) {
            showAlert('Please fill in all item details', 'error');
            return;
        }

        const saleData = {
            client_id: parseInt(document.getElementById('sale-client').value) || null,
            payment_method: document.getElementById('sale-payment-method').value,
            discount_applied: parseFloat(document.getElementById('sale-discount').value) || 0,
            items: items
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
                e.target.reset(); // Reset the form
                setupSaleItemRow(); // Reset the item rows
                calculateSaleTotal(); // Reset the total display
                await loadAllData(); // Refresh all data (stock, etc.)
            } else {
                showAlert(result.message || 'Failed to record sale', 'error');
            }
        } catch (error) {
            displayResponse('sale-response', { message: `Network error: ${error.message}` }, 0);
            showAlert('Network error occurred', 'error');
        }
    }

    // --- PROCESS PURCHASE (Stock Addition) ---
    async function processPurchase(e) {
        e.preventDefault();

        const body = {
            product_id: parseInt(document.getElementById('purchase-product').value),
            supplier_id: parseInt(document.getElementById('purchase-supplier').value),
            quantity: parseInt(document.getElementById('purchase-quantity').value),
            cost_price: parseFloat(document.getElementById('purchase-cost-price').value) || null,
            selling_price: parseFloat(document.getElementById('purchase-selling-price').value),
            location: document.getElementById('purchase-location').value || null,
            expiration_date: document.getElementById('purchase-expiration').value || null
        };

        try {
            const response = await fetch(`${API_BASE_URL}/stocks`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(body)
            });

            const result = await response.json();
            displayResponse('purchase-response', result, response.status);

            if (response.ok) {
                showAlert('Stock added successfully!', 'success');
                e.target.reset();
                await loadAllData();
            } else {
                showAlert(result.message || 'Failed to add stock', 'error');
            }
        } catch (error) {
            displayResponse('purchase-response', { message: `Network error: ${error.message}` }, 0);
            showAlert('Network error occurred', 'error');
        }
    }

    // --- PROCESS RETURN ---
    // --- UPDATED SECTION: Replace Mock API call with real one ---
    async function processReturn(e) {
        e.preventDefault();

        const body = {
            stock_id: parseInt(document.getElementById('return-stock').value),
            quantity: parseInt(document.getElementById('return-quantity').value),
            reason: document.getElementById('return-reason').value,
            restock: document.getElementById('return-restock').checked,
            notes: document.getElementById('return-notes').value
        };

        if (!body.stock_id || !body.quantity || !body.reason) {
            showAlert('Please select an item, quantity, and reason', 'error');
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/stock/return`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(body)
            });

            const result = await response.json();
            displayResponse('return-response', result, response.status);

            if (response.ok) {
                showAlert(result.message, 'success');
                e.target.reset();
                document.getElementById('return-restock').checked = true; // Reset checkbox
                await loadAllData();
            } else {
                showAlert(result.message || 'Failed to process return', 'error');
            }

        } catch (error) {
            displayResponse('return-response', { message: `Network error: ${error.message}` }, 0);
            showAlert('Network error occurred', 'error');
        }
    }

    // --- PROCESS REMOVAL ---
    // --- UPDATED SECTION: Replace Mock API call with real one ---
    async function processRemoval(e) {
        e.preventDefault();

        const body = {
            stock_id: parseInt(document.getElementById('removal-stock').value),
            quantity: parseInt(document.getElementById('removal-quantity').value),
            reason: document.getElementById('removal-reason').value,
            notes: document.getElementById('removal-notes').value
        };

        if (!body.stock_id || !body.quantity || !body.reason) {
            showAlert('Please select an item, quantity, and reason', 'error');
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/stock/removal`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(body)
            });

            const result = await response.json();
            displayResponse('removal-response', result, response.status);

            if (response.ok) {
                showAlert(result.message, 'success');
                e.target.reset();
                await loadAllData();
            } else {
                showAlert(result.message || 'Failed to remove stock', 'error');
            }

        } catch (error) {
            displayResponse('removal-response', { message: `Network error: ${error.message}` }, 0);
            showAlert('Network error occurred', 'error');
        }
    }

    // --- RENDER TRANSACTION HISTORY ---
    function renderTransactionHistory(transactions) {
        const container = document.getElementById('transaction-history');

        if (transactions.length === 0) {
            container.innerHTML = '<div class="empty-state"><span class="empty-icon">📋</span><p>No transactions found</p></div>';
            return;
        }

        const getIcon = (type) => {
            const icons = {sale: '🛒', purchase: '📥', return: '↩️', removal: '🗑️'};
            return icons[type] || '📋';
        };

        const getIconClass = (type) => {
            const classes = {sale: 'icon-sale', purchase: 'icon-purchase', return: 'icon-return', removal: 'icon-removal'};
            return classes[type] || 'icon-sale';
        };

        const getTimeAgo = (dateString) => {
            const date = new Date(dateString);
            const now = new Date();
            const seconds = Math.floor((now - date) / 1000);
            
            if (seconds < 60) return `${seconds}s ago`;
            if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
            if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
            return date.toLocaleDateString();
        };

        container.innerHTML = transactions.map((trans, index) => `
            <div class="transaction-item" style="animation-delay: ${index * 0.03}s">
                <div class="transaction-icon ${getIconClass(trans.type)}">
                    ${getIcon(trans.type)}
                </div>
                <div class="transaction-details">
                    <div class="transaction-name">${trans.item_name} (${trans.type})</div>
                    <div class="transaction-meta">
                        Qty: ${trans.quantity} | ${getTimeAgo(trans.date)} | ${trans.user}
                        ${trans.details ? `| ${trans.details}` : ''}
                    </div>
                </div>
                <div class="transaction-amount ${trans.type === 'sale' ? 'positive' : trans.type === 'purchase' ? 'negative' : ''}">
                    ${trans.type === 'sale' ? '+' : trans.type === 'purchase' ? '-' : ''}$${trans.amount.toFixed(2)}
                </div>
            </div>
        `).join('');
    }

    // --- FILTER HISTORY ---
    function filterHistory() {
        const searchTerm = document.getElementById('history-search').value.toLowerCase();
        const typeFilter = document.getElementById('history-type-filter').value;
        const dateFilter = document.getElementById('history-date-filter').value;

        let filtered = allTransactions.filter(t => {
            const matchesSearch = t.item_name.toLowerCase().includes(searchTerm) || 
                                  t.details.toLowerCase().includes(searchTerm);
            const matchesType = typeFilter === 'all' || t.type === typeFilter;
            const matchesDate = !dateFilter || t.date.startsWith(dateFilter);
            
            return matchesSearch && matchesType && matchesDate;
        });

        renderTransactionHistory(filtered);
    }

    // --- HELPERS ---
    function displayResponse(elementId, data, status) {
        const element = document.getElementById(elementId);
        element.textContent = `Status: ${status}\n\n${JSON.stringify(data, null, 2)}`;
        element.className = (status >= 400) ? 'response-pre error' : 'response-pre success';
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

    // --- RUN THE APP ---
    initPage();
});
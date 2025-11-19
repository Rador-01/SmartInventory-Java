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

    // --- INITIALIZE PAGE ---
    function initPage() {
        // Populate user info
        document.getElementById('sidebar-username').textContent = user.username;
        document.getElementById('top-bar-username').textContent = user.username;
        document.getElementById('sidebar-avatar').textContent = user.username.charAt(0).toUpperCase();
        document.getElementById('sidebar-role').textContent = 'User';

        // Set current date
        document.getElementById('current-date-display').textContent = new Date().toLocaleDateString('en-US', {
            year: 'numeric', month: 'long', day: 'numeric'
        });

        // Bind events
        document.getElementById('logout-btn').addEventListener('click', handleLogout);
        document.getElementById('time-filter').addEventListener('change', handleTimeFilter);

        // Load dashboard data
        loadDashboardData();
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

    // --- API HELPER ---
    async function fetchFromApi(endpoint) {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) {
            throw new Error(`Failed to fetch ${endpoint}: ${response.statusText}`);
        }
        return await response.json();
    }

    // --- LOAD ALL DASHBOARD DATA ---
    async function loadDashboardData() {
        try {
            // Fetch all required data in parallel
            const [summaryData, allStock, allSales, performanceData] = await Promise.all([
                fetchFromApi('/reports/summary'),
                fetchFromApi('/stocks'),
                fetchFromApi('/sales'),
                fetchFromApi('/reports/performance_by_product') // FIX: Correct endpoint
            ]);

            // Pass data to render functions
            updateMetricsCards(summaryData, allStock, allSales);
            renderLowStockAlerts(allStock);
            renderRecentTransactions(allSales);
            renderTopProducts(performanceData);
            renderStockStatus(allStock);

            // Update notification count from the 'allStock' data we already fetched
            const lowStockCount = allStock.filter(s => s.quantity > 0 && s.quantity < 10).length;
            document.getElementById('notification-count').textContent = lowStockCount;

        } catch (error) {
            console.error('Failed to load dashboard data:', error);
            showAlert('Could not load all dashboard data. Please try again.', 'error');
        }
    }

    // --- UPDATE METRIC CARDS ---
    // FIX: This function now gets all data it needs and maps it to the correct HTML
    function updateMetricsCards(summary, stocks, sales) {
        
        // 1. Total Products (from /api/stocks)
        animateValue('total-products', 0, stocks.length, 1500);
        document.getElementById('new-products').textContent = `+0 this week`; // (Mocked)

        // 2. Low Stock (from /api/stocks)
        const lowStockCount = stocks.filter(s => s.quantity > 0 && s.quantity < 10).length;
        animateValue('low-stock-count', 0, lowStockCount, 1500);

        // 3. Today's Revenue (from /api/reports/summary)
        const revenue = parseFloat(summary.total_revenue).toFixed(2);
        document.getElementById('today-revenue').textContent = `$${revenue}`;
        document.getElementById('revenue-change').textContent = `+0% vs yesterday`; // (Mocked)

        // 4. Total Sales (from /api/sales)
        animateValue('total-sales', 0, sales.length, 1500);
        document.getElementById('sales-count').textContent = `${sales.length} transactions`;
    }

    // --- RENDER LOW STOCK ALERTS ---
    // FIX: Changed from 'fetch' to 'render' - it now receives data
    function renderLowStockAlerts(stocks) {
        const lowStockItems = stocks.filter(s => s.quantity < 10 && s.quantity > 0).slice(0, 5); // Get first 5
        
        const container = document.getElementById('low-stock-list');
        
        if (lowStockItems.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <span class="empty-icon">✓</span>
                    <p>All items are well stocked!</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = `<div class="alert-list">${lowStockItems.map((item, index) => `
            <div class="alert-item" style="animation-delay: ${index * 0.05}s">
                <div class="alert-item-icon">📦</div>
                <div class="alert-item-content">
                    <div class="alert-item-name">${item.product_name || 'Unknown Product'}</div>
                    <div class="alert-item-info">
                        Stock: <span class="text-danger">${item.quantity}</span> / Threshold: 10
                    </div>
                </div>
                <a href="items.html?action=restock&id=${item.id}" class="btn-reorder">Reorder</a>
            </div>
        `).join('')}</div>`;
    }

    // --- RENDER RECENT TRANSACTIONS ---
    // FIX: Changed from 'fetch' to 'render' - it now receives data
    function renderRecentTransactions(sales) {
        const recentSales = sales.slice(0, 8); // Get top 8 recent
        
        const container = document.getElementById('recent-transactions');
        
        if (recentSales.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <span class="empty-icon">📋</span>
                    <p>No transactions yet</p>
                </div>
            `;
            return;
        }
        
        const getIcon = (type) => '🛒'; // Only sales for now
        const getIconClass = (type) => 'icon-sale';
        
        container.innerHTML = `<div class="transaction-list">${recentSales.map((trans, index) => `
            <div class="transaction-item" style="animation-delay: ${index * 0.03}s">
                <div class="transaction-icon ${getIconClass('sale')}">
                    ${getIcon('sale')}
                </div>
                <div class="transaction-details">
                    <div class="transaction-name">Sale #${trans.id} (${trans.items.length} items)</div>
                    <div class="transaction-meta">${new Date(trans.sale_date).toLocaleString()} • ${trans.salesperson_username}</div>
                </div>
                <div class="transaction-amount positive">
                    +$${parseFloat(trans.total_amount).toFixed(2)}
                </div>
            </div>
        `).join('')}</div>`;
    }

    // --- RENDER TOP PRODUCTS ---
    // FIX: Changed from 'fetch' to 'render' and uses correct data
    function renderTopProducts(topProductsData) {
        const top5Products = topProductsData.slice(0, 5); // Get top 5

        const container = document.getElementById('top-products-list');
        
        if (top5Products.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <span class="empty-icon">📊</span>
                    <p>No sales data available</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = `<div class="top-products-list">${top5Products.map((product, index) => `
            <div class="top-product-item" style="animation-delay: ${index * 0.05}s">
                <div class="product-rank">${index + 1}</div>
                <div class="product-info">
                    <div class="product-name">${product.name}</div>
                    <div class="product-sales">${product.quantity} units sold</div>
                </div>
                <div class="product-revenue">$${parseFloat(product.revenue).toFixed(2)}</div>
            </div>
        `).join('')}</div>`;
    }

    // --- RENDER STOCK STATUS ---
    // FIX: Changed from 'fetch' to 'render' and uses 'allStock' data
    function renderStockStatus(stocks) {
        // Calculate status counts
        const inStock = stocks.filter(s => s.quantity >= 10).length;
        const lowStock = stocks.filter(s => s.quantity > 0 && s.quantity < 10).length;
        const outOfStock = stocks.filter(s => s.quantity === 0).length;
        // Check for expiration date
        const expired = stocks.filter(s => s.expiration_date && new Date(s.expiration_date) < new Date()).length;
        
        // Update UI with animation
        animateValue('in-stock-count', 0, inStock, 1500);
        animateValue('low-stock-status', 0, lowStock, 1500);
        animateValue('out-of-stock-count', 0, outOfStock, 1500);
        animateValue('expired-count', 0, expired, 1500);
    }

    // --- HANDLE TIME FILTER ---
    // FIX: This now re-fetches just the top products and renders them
    async function handleTimeFilter(e) {
        const period = e.target.value;
        const container = document.getElementById('top-products-list');
        
        // Add loading state
        container.style.opacity = '0.5';
        container.style.pointerEvents = 'none';
        
        try {
            // Re-fetch only the data that needs to change
            // Note: The period parameter isn't implemented in the API, 
            // but this is the correct frontend structure.
            const topProductsData = await fetchFromApi('/reports/performance_by_product');
            renderTopProducts(topProductsData);
        } catch (error) {
            showAlert('Failed to refresh top products', 'error');
        } finally {
            container.style.opacity = '1';
            container.style.pointerEvents = 'auto';
        }
    }

    // --- HELPER: ANIMATE VALUE ---
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

    // --- HELPER: SHOW ALERT ---
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
    // FIX: Removed the broken fetchStock() call
    initPage();
});
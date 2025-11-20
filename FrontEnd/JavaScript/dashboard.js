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
        document.getElementById('sidebar-role').textContent = user.role || 'User';

        // Set current date
        document.getElementById('current-date-display').textContent = new Date().toLocaleDateString('en-US', {
            year: 'numeric', month: 'long', day: 'numeric'
        });

        // Bind events
        document.getElementById('logout-btn').addEventListener('click', handleLogout);

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

    // --- LOAD ALL DASHBOARD DATA ---
    async function loadDashboardData() {
        try {
            // Fetch all required data in parallel
            const [allProducts, allSales] = await Promise.all([
                fetchFromApi('/products'),
                fetchFromApi('/sales')
            ]);

            // Update metrics cards
            updateMetricsCards(allProducts, allSales);

            // Render sections
            renderLowStockAlerts(allProducts);
            renderRecentTransactions(allSales);
            renderTopProducts(allSales, allProducts);
            renderStockStatus(allProducts);

            // Update notification count
            const lowStockCount = allProducts.filter(p => {
                const stock = p.currentStock || 0;
                return stock > 0 && stock < 10;
            }).length;
            document.getElementById('notification-count').textContent = lowStockCount;

        } catch (error) {
            console.error('Failed to load dashboard data:', error);
            showAlert('Could not load all dashboard data. Please try again.', 'error');
        }
    }

    // --- UPDATE METRIC CARDS ---
    function updateMetricsCards(products, sales) {
        // 1. Total Products
        animateValue('total-products', 0, products.length, 1500);
        document.getElementById('new-products').textContent = `+0 this week`;

        // 2. Low Stock
        const lowStockCount = products.filter(p => {
            const stock = p.currentStock || 0;
            return stock > 0 && stock < 10;
        }).length;
        animateValue('low-stock-count', 0, lowStockCount, 1500);

        // 3. Today's Revenue
        const today = new Date().toDateString();
        const todaysRevenue = sales
            .filter(s => s.status === 'PAID' && new Date(s.saleDate).toDateString() === today)
            .reduce((sum, s) => sum + (s.totalAmount || 0), 0);
        document.getElementById('today-revenue').textContent = `$${todaysRevenue.toFixed(2)}`;

        // 4. Total Sales
        const totalSales = sales.filter(s => s.status === 'PAID').length;
        animateValue('total-sales', 0, totalSales, 1500);
        document.getElementById('sales-count').textContent = `${totalSales} transactions`;
    }

    // --- RENDER LOW STOCK ALERTS ---
    function renderLowStockAlerts(products) {
        const lowStockItems = products.filter(p => {
            const stock = p.currentStock || 0;
            return stock < 10 && stock > 0;
        }).slice(0, 5);

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
                    <div class="alert-item-name">${item.name || 'Unknown Product'}</div>
                    <div class="alert-item-info">
                        Stock: <span class="text-danger">${item.currentStock || 0}</span> / Threshold: 10
                    </div>
                </div>
                <a href="items.html?action=restock&id=${item.id}" class="btn-reorder">Reorder</a>
            </div>
        `).join('')}</div>`;
    }

    // --- RENDER RECENT TRANSACTIONS ---
    function renderRecentTransactions(sales) {
        const recentSales = sales.slice(0, 8);

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

        container.innerHTML = `<div class="transaction-list">${recentSales.map((trans, index) => `
            <div class="transaction-item" style="animation-delay: ${index * 0.03}s">
                <div class="transaction-icon icon-sale">🛒</div>
                <div class="transaction-details">
                    <div class="transaction-name">Sale #${trans.id} (${trans.items?.length || 0} items)</div>
                    <div class="transaction-meta">${new Date(trans.saleDate).toLocaleString()} • ${trans.status}</div>
                </div>
                <div class="transaction-amount positive">
                    +$${parseFloat(trans.totalAmount || 0).toFixed(2)}
                </div>
            </div>
        `).join('')}</div>`;
    }

    // --- RENDER TOP PRODUCTS ---
    function renderTopProducts(sales, products) {
        // Calculate product sales
        const productSales = {};
        sales.forEach(sale => {
            if (sale.items && sale.status === 'PAID') {
                sale.items.forEach(item => {
                    const productId = item.product?.id || item.productId;
                    if (!productSales[productId]) {
                        productSales[productId] = {
                            quantity: 0,
                            revenue: 0,
                            name: item.product?.name || 'Unknown'
                        };
                    }
                    productSales[productId].quantity += item.quantity || 0;
                    productSales[productId].revenue += item.subtotal || 0;
                });
            }
        });

        // Convert to array and sort
        const topProducts = Object.values(productSales)
            .sort((a, b) => b.revenue - a.revenue)
            .slice(0, 5);

        const container = document.getElementById('top-products-list');

        if (topProducts.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <span class="empty-icon">📊</span>
                    <p>No sales data available</p>
                </div>
            `;
            return;
        }

        container.innerHTML = `<div class="top-products-list">${topProducts.map((product, index) => `
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
    function renderStockStatus(products) {
        const inStock = products.filter(p => (p.currentStock || 0) >= 10).length;
        const lowStock = products.filter(p => {
            const stock = p.currentStock || 0;
            return stock > 0 && stock < 10;
        }).length;
        const outOfStock = products.filter(p => (p.currentStock || 0) === 0).length;
        const expired = 0; // Not tracking expiration in current backend

        animateValue('in-stock-count', 0, inStock, 1500);
        animateValue('low-stock-status', 0, lowStock, 1500);
        animateValue('out-of-stock-count', 0, outOfStock, 1500);
        animateValue('expired-count', 0, expired, 1500);
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
    initPage();
});
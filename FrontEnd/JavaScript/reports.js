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

    // --- GLOBAL DATA ---
    let allProducts = [];
    let allSales = [];
    let allCategories = [];
    let allSuppliers = [];

    // Chart instances
    let charts = {};

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

        // --- BIND EVENT LISTENERS ---
        document.getElementById('logout-btn').addEventListener('click', handleLogout);
        document.getElementById('generate-report').addEventListener('click', generateReport);
        document.getElementById('export-report').addEventListener('click', exportReport);
        document.getElementById('best-products-metric').addEventListener('change', updateBestProducts);
        document.getElementById('worst-products-metric').addEventListener('change', updateWorstProducts);

        // Load data and generate initial report
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

    // --- API HELPER ---
    async function fetchFromApi(endpoint) {
        try {
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
        } catch (error) {
            console.error(`Error fetching ${endpoint}:`, error);
            showAlert(`Failed to load data for ${endpoint}`, 'error');
            throw error;
        }
    }

    // --- LOAD ALL DATA ---
    async function loadAllData() {
        showAlert('Loading report data...', 'success');
        try {
            [allProducts, allSales, allCategories, allSuppliers] = await Promise.all([
                fetchFromApi('/products'),
                fetchFromApi('/sales'),
                fetchFromApi('/categories'),
                fetchFromApi('/suppliers')
            ]);

            // Generate initial report
            generateReport();
            showAlert('Reports loaded successfully!', 'success');

        } catch (error) {
            console.error('Failed to load all dashboard data:', error);
            showAlert('Could not load all report data. Please try again.', 'error');
        }
    }

    // --- GENERATE REPORT ---
    function generateReport() {
        // Calculate all metrics
        const summaryMetrics = calculateSummaryMetrics();
        const salesTrendData = calculateSalesTrend();
        const productPerformance = calculateProductPerformance();
        const categoryPerformance = calculateCategoryPerformance();
        const supplierPerformance = calculateSupplierPerformance();
        const stockStatusData = calculateStockStatus();
        const inventoryStats = calculateInventoryStats();

        // Update key metrics
        calculateKeyMetrics(summaryMetrics);

        // Generate all charts
        createSalesTrendChart(salesTrendData.labels, salesTrendData.revenue, salesTrendData.profit);
        createRevenueCategoryChart(categoryPerformance.map(c => c.name), categoryPerformance.map(c => c.revenue));
        createTopProductsChart(productPerformance);
        createProfitMarginChart(productPerformance);
        createStockStatusChart(stockStatusData);
        createROIChart(categoryPerformance);
        createSupplierPerformanceChart(supplierPerformance.labels, supplierPerformance.data);

        // Update tables
        updateBestProducts();
        updateWorstProducts();

        // Generate recommendations
        generateRecommendations(productPerformance, stockStatusData);

        // Update quick stats
        updateQuickStats(inventoryStats);

        // Update notification count
        document.getElementById('notification-count').textContent = stockStatusData.low_stock || 0;
    }

    // --- CALCULATE SUMMARY METRICS ---
    function calculateSummaryMetrics() {
        const paidSales = allSales.filter(s => s.status === 'PAID');

        const totalRevenue = paidSales.reduce((sum, s) => sum + (s.totalAmount || 0), 0);

        // Calculate total cost
        let totalCost = 0;
        paidSales.forEach(sale => {
            if (sale.items) {
                sale.items.forEach(item => {
                    const product = allProducts.find(p => p.id === item.product?.id);
                    if (product && product.costPrice) {
                        totalCost += product.costPrice * item.quantity;
                    }
                });
            }
        });

        const totalProfit = totalRevenue - totalCost;
        const profitMargin = totalRevenue > 0 ? (totalProfit / totalRevenue) * 100 : 0;
        const roi = totalCost > 0 ? (totalProfit / totalCost) * 100 : 0;

        // Calculate turnover rate (simple: sales / products)
        const turnoverRate = allProducts.length > 0 ? paidSales.length / allProducts.length : 0;

        return {
            total_revenue: totalRevenue,
            total_profit: totalProfit,
            profit_margin_percent: profitMargin,
            roi_percent: roi,
            turnover_rate: turnoverRate
        };
    }

    // --- CALCULATE SALES TREND ---
    function calculateSalesTrend() {
        // Get last 30 days of sales
        const days = 30;
        const labels = [];
        const revenue = [];
        const profit = [];

        const today = new Date();

        for (let i = days - 1; i >= 0; i--) {
            const date = new Date(today);
            date.setDate(date.getDate() - i);
            const dateStr = date.toISOString().split('T')[0];

            labels.push(dateStr);

            // Calculate revenue for this day
            const daySales = allSales.filter(s => {
                const saleDate = new Date(s.saleDate).toISOString().split('T')[0];
                return saleDate === dateStr && s.status === 'PAID';
            });

            const dayRevenue = daySales.reduce((sum, s) => sum + (s.totalAmount || 0), 0);
            revenue.push(dayRevenue);

            // Calculate profit for this day
            let dayCost = 0;
            daySales.forEach(sale => {
                if (sale.items) {
                    sale.items.forEach(item => {
                        const product = allProducts.find(p => p.id === item.product?.id);
                        if (product && product.costPrice) {
                            dayCost += product.costPrice * item.quantity;
                        }
                    });
                }
            });
            profit.push(dayRevenue - dayCost);
        }

        return { labels, revenue, profit };
    }

    // --- CALCULATE PRODUCT PERFORMANCE ---
    function calculateProductPerformance() {
        const performance = {};

        // Initialize all products
        allProducts.forEach(product => {
            performance[product.id] = {
                id: product.id,
                name: product.name,
                quantity: 0,
                revenue: 0,
                cost: 0,
                profit: 0,
                profit_margin: 0,
                roi: 0
            };
        });

        // Calculate from sales
        allSales.filter(s => s.status === 'PAID').forEach(sale => {
            if (sale.items) {
                sale.items.forEach(item => {
                    const productId = item.product?.id;
                    if (productId && performance[productId]) {
                        const product = allProducts.find(p => p.id === productId);

                        performance[productId].quantity += item.quantity || 0;
                        performance[productId].revenue += item.subtotal || 0;

                        if (product && product.costPrice) {
                            const itemCost = product.costPrice * item.quantity;
                            performance[productId].cost += itemCost;
                        }
                    }
                });
            }
        });

        // Calculate profit, margin, and ROI
        Object.values(performance).forEach(p => {
            p.profit = p.revenue - p.cost;
            p.profit_margin = p.revenue > 0 ? (p.profit / p.revenue) * 100 : 0;
            p.roi = p.cost > 0 ? (p.profit / p.cost) * 100 : 0;
        });

        return Object.values(performance);
    }

    // --- CALCULATE CATEGORY PERFORMANCE ---
    function calculateCategoryPerformance() {
        const performance = {};

        // Initialize categories
        allCategories.forEach(cat => {
            performance[cat.id] = {
                id: cat.id,
                name: cat.name,
                revenue: 0,
                cost: 0,
                profit: 0,
                roi: 0,
                quantity: 0
            };
        });

        // Calculate from sales
        allSales.filter(s => s.status === 'PAID').forEach(sale => {
            if (sale.items) {
                sale.items.forEach(item => {
                    const product = allProducts.find(p => p.id === item.product?.id);
                    if (product && product.category) {
                        const catId = product.category.id;
                        if (performance[catId]) {
                            performance[catId].revenue += item.subtotal || 0;
                            performance[catId].quantity += item.quantity || 0;

                            if (product.costPrice) {
                                const itemCost = product.costPrice * item.quantity;
                                performance[catId].cost += itemCost;
                            }
                        }
                    }
                });
            }
        });

        // Calculate profit and ROI
        Object.values(performance).forEach(p => {
            p.profit = p.revenue - p.cost;
            p.roi = p.cost > 0 ? (p.profit / p.cost) * 100 : 0;
        });

        return Object.values(performance).filter(p => p.revenue > 0);
    }

    // --- CALCULATE SUPPLIER PERFORMANCE ---
    function calculateSupplierPerformance() {
        const performance = {};

        // Initialize suppliers
        allSuppliers.forEach(sup => {
            performance[sup.id] = {
                name: sup.name,
                revenue: 0
            };
        });

        // Calculate from sales
        allSales.filter(s => s.status === 'PAID').forEach(sale => {
            if (sale.items) {
                sale.items.forEach(item => {
                    const product = allProducts.find(p => p.id === item.product?.id);
                    if (product && product.supplier) {
                        const supId = product.supplier.id;
                        if (performance[supId]) {
                            performance[supId].revenue += item.subtotal || 0;
                        }
                    }
                });
            }
        });

        const sorted = Object.values(performance)
            .filter(p => p.revenue > 0)
            .sort((a, b) => b.revenue - a.revenue);

        return {
            labels: sorted.map(s => s.name),
            data: sorted.map(s => s.revenue)
        };
    }

    // --- CALCULATE STOCK STATUS ---
    function calculateStockStatus() {
        const inStock = allProducts.filter(p => (p.currentStock || 0) >= 10).length;
        const lowStock = allProducts.filter(p => {
            const stock = p.currentStock || 0;
            return stock > 0 && stock < 10;
        }).length;
        const outOfStock = allProducts.filter(p => (p.currentStock || 0) === 0).length;

        return {
            in_stock: inStock,
            low_stock: lowStock,
            out_of_stock: outOfStock
        };
    }

    // --- CALCULATE INVENTORY STATS ---
    function calculateInventoryStats() {
        const totalItems = allProducts.reduce((sum, p) => sum + (p.currentStock || 0), 0);
        const totalValue = allProducts.reduce((sum, p) => {
            const stock = p.currentStock || 0;
            const cost = p.costPrice || 0;
            return sum + (stock * cost);
        }, 0);

        const paidSales = allSales.filter(s => s.status === 'PAID');
        const totalItemsSold = paidSales.reduce((sum, sale) => {
            if (sale.items) {
                return sum + sale.items.reduce((s, item) => s + (item.quantity || 0), 0);
            }
            return sum;
        }, 0);

        // Calculate average profit margin
        const productsWithMargin = allProducts.filter(p => p.costPrice && p.sellingPrice);
        const avgMargin = productsWithMargin.length > 0
            ? productsWithMargin.reduce((sum, p) => {
                const margin = ((p.sellingPrice - p.costPrice) / p.costPrice) * 100;
                return sum + margin;
            }, 0) / productsWithMargin.length
            : 0;

        return {
            total_items_in_stock: totalItems,
            total_inventory_value: totalValue,
            average_profit_margin: avgMargin,
            total_items_sold: totalItemsSold
        };
    }

    // --- CALCULATE KEY METRICS ---
    function calculateKeyMetrics(summary) {
        animateValue('total-revenue', 0, parseFloat(summary.total_revenue), 1500, true);
        animateValue('total-profit', 0, parseFloat(summary.total_profit), 1500, true);

        document.getElementById('revenue-change').textContent = '+12.5%'; // Mock
        document.getElementById('profit-margin').textContent = `${parseFloat(summary.profit_margin_percent).toFixed(1)}% margin`;
        document.getElementById('roi-value').textContent = `${parseFloat(summary.roi_percent).toFixed(1)}%`;
        document.getElementById('roi-status').textContent = summary.roi_percent > 20 ? 'Excellent' : summary.roi_percent > 10 ? 'Good' : 'Fair';
        document.getElementById('turnover-rate').textContent = `${parseFloat(summary.turnover_rate).toFixed(1)}x`;
        document.getElementById('turnover-status').textContent = summary.turnover_rate > 2 ? 'Excellent' : summary.turnover_rate > 1 ? 'Good' : 'Slow';
    }

    // --- CREATE SALES TREND CHART ---
    function createSalesTrendChart(labels, revenueData, profitData) {
        const ctx = document.getElementById('salesTrendChart');
        if (charts.salesTrend) charts.salesTrend.destroy();

        charts.salesTrend = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels.map(date => new Date(date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })),
                datasets: [
                    {
                        label: 'Revenue',
                        data: revenueData,
                        borderColor: '#2196F3',
                        backgroundColor: 'rgba(33, 150, 243, 0.1)',
                        tension: 0.4,
                        fill: true
                    },
                    {
                        label: 'Profit',
                        data: profitData,
                        borderColor: '#4CAF50',
                        backgroundColor: 'rgba(76, 175, 80, 0.1)',
                        tension: 0.4,
                        fill: true
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'top' },
                    tooltip: {
                        callbacks: {
                            label: (context) => `${context.dataset.label}: $${parseFloat(context.parsed.y).toFixed(2)}`
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: (value) => '$' + value.toFixed(0)
                        }
                    }
                }
            }
        });
    }

    // --- CREATE REVENUE BY CATEGORY CHART ---
    function createRevenueCategoryChart(labels, data) {
        const ctx = document.getElementById('revenueCategoryChart');
        if (charts.revenueCategory) charts.revenueCategory.destroy();

        charts.revenueCategory = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: [
                        '#2196F3',
                        '#4CAF50',
                        '#FF9800',
                        '#9C27B0',
                        '#F44336',
                        '#00BCD4'
                    ]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'right' },
                    tooltip: {
                        callbacks: {
                            label: (context) => `${context.label}: $${parseFloat(context.parsed).toFixed(2)}`
                        }
                    }
                }
            }
        });
    }

    // --- CREATE TOP PRODUCTS CHART ---
    function createTopProductsChart(products) {
        const sorted = [...products].sort((a, b) => b.quantity - a.quantity).slice(0, 10);
        const ctx = document.getElementById('topProductsChart');
        if (charts.topProducts) charts.topProducts.destroy();

        charts.topProducts = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: sorted.map(p => p.name),
                datasets: [{
                    label: 'Units Sold',
                    data: sorted.map(p => p.quantity),
                    backgroundColor: '#2196F3'
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                }
            }
        });
    }

    // --- CREATE PROFIT MARGIN CHART ---
    function createProfitMarginChart(products) {
        const sorted = [...products].sort((a, b) => b.profit_margin - a.profit_margin).slice(0, 10);
        const ctx = document.getElementById('profitMarginChart');
        if (charts.profitMargin) charts.profitMargin.destroy();

        charts.profitMargin = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: sorted.map(p => p.name),
                datasets: [{
                    label: 'Profit Margin (%)',
                    data: sorted.map(p => p.profit_margin),
                    backgroundColor: '#4CAF50'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: (value) => parseFloat(value).toFixed(1) + '%'
                        }
                    }
                }
            }
        });
    }

    // --- CREATE STOCK STATUS CHART ---
    function createStockStatusChart(statusData) {
        const ctx = document.getElementById('stockStatusChart');
        if (charts.stockStatus) charts.stockStatus.destroy();

        charts.stockStatus = new Chart(ctx, {
            type: 'pie',
            data: {
                labels: ['In Stock', 'Low Stock', 'Out of Stock'],
                datasets: [{
                    data: [statusData.in_stock, statusData.low_stock, statusData.out_of_stock],
                    backgroundColor: ['#4CAF50', '#FF9800', '#F44336']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom' }
                }
            }
        });
    }

    // --- CREATE ROI CHART ---
    function createROIChart(categoryData) {
        const sorted = [...categoryData].sort((a, b) => b.roi - a.roi);
        const ctx = document.getElementById('roiChart');
        if (charts.roi) charts.roi.destroy();

        charts.roi = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: sorted.map(c => c.name),
                datasets: [{
                    label: 'ROI (%)',
                    data: sorted.map(c => c.roi),
                    backgroundColor: '#9C27B0'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: (value) => parseFloat(value).toFixed(1) + '%'
                        }
                    }
                }
            }
        });
    }

    // --- CREATE SUPPLIER PERFORMANCE CHART ---
    function createSupplierPerformanceChart(labels, data) {
        const ctx = document.getElementById('supplierPerformanceChart');
        if (charts.supplierPerformance) charts.supplierPerformance.destroy();

        charts.supplierPerformance = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Revenue Generated',
                    data: data,
                    backgroundColor: '#00BCD4'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: (value) => '$' + parseFloat(value).toFixed(0)
                        }
                    }
                }
            }
        });
    }

    // --- UPDATE BEST/WORST PRODUCTS ---
    function updateBestProducts() {
        const metric = document.getElementById('best-products-metric').value;
        const productPerformance = calculateProductPerformance();
        const sorted = [...productPerformance].sort((a, b) => parseFloat(b[metric]) - parseFloat(a[metric])).slice(0, 10);
        renderPerformanceTable('best-products-table', sorted, metric, true);
    }

    function updateWorstProducts() {
        const metric = document.getElementById('worst-products-metric').value;
        const productPerformance = calculateProductPerformance();
        const sorted = [...productPerformance].sort((a, b) => parseFloat(a[metric]) - parseFloat(b[metric])).slice(0, 10);
        renderPerformanceTable('worst-products-table', sorted, metric, false);
    }

    function renderPerformanceTable(containerId, products, metric, isBest) {
        const container = document.getElementById(containerId);

        if (products.length === 0) {
            container.innerHTML = '<div class="empty-state"><span class="empty-icon">📊</span><p>No data available</p></div>';
            return;
        }

        const formatValue = (value) => {
            if (metric === 'revenue' || metric === 'profit' || metric === 'cost') return `$${parseFloat(value).toFixed(2)}`;
            if (metric === 'roi' || metric === 'profit_margin') return `${parseFloat(value).toFixed(1)}%`;
            return value;
        };

        container.innerHTML = products.map((product, index) => `
            <div class="performance-row ${isBest ? 'best' : 'worst'}">
                <div class="rank">${index + 1}</div>
                <div class="product-name">${product.name}</div>
                <div class="metric-value">${formatValue(product[metric])}</div>
            </div>
        `).join('');
    }

    // --- GENERATE RECOMMENDATIONS ---
    function generateRecommendations(productPerformance, stockStatus) {
        const recommendations = [];

        // Low stock recommendations
        if (stockStatus.low_stock > 0) {
            recommendations.push({
                type: 'warning',
                icon: '⚠️',
                title: 'Low Stock Alert',
                message: `${stockStatus.low_stock} products need restocking. Consider reordering soon.`,
                action: 'View Items'
            });
        }

        // Best sellers recommendation
        const bestSellers = [...productPerformance].sort((a, b) => b.revenue - a.revenue);
        if (bestSellers.length > 0 && bestSellers[0].revenue > 0) {
            recommendations.push({
                type: 'success',
                icon: '🌟',
                title: 'Focus on Best Sellers',
                message: `${bestSellers[0].name} is your top performer with $${bestSellers[0].revenue.toFixed(2)} in revenue. Ensure adequate stock levels.`,
                action: 'View Details'
            });
        }

        // High margin products
        const highMargin = [...productPerformance].sort((a, b) => b.roi - a.roi);
        if (highMargin.length > 0 && highMargin[0].roi > 30) {
            recommendations.push({
                type: 'success',
                icon: '💰',
                title: 'High Profit Opportunity',
                message: `${highMargin[0].name} has ${parseFloat(highMargin[0].roi).toFixed(1)}% ROI. Consider promoting it.`,
                action: 'Promote'
            });
        }

        // Slow movers
        const slowMovers = [...productPerformance].sort((a, b) => a.quantity - b.quantity);
        if (slowMovers.length > 0 && slowMovers[0].quantity < 5 && slowMovers[0].quantity > 0) {
            recommendations.push({
                type: 'info',
                icon: '📉',
                title: 'Slow Moving Items',
                message: `${slowMovers[0].name} has low sales (${slowMovers[0].quantity} units). Consider discounting to improve turnover.`,
                action: 'Create Promotion'
            });
        }

        // Out of stock
        if (stockStatus.out_of_stock > 0) {
            recommendations.push({
                type: 'warning',
                icon: '❌',
                title: 'Out of Stock Items',
                message: `${stockStatus.out_of_stock} products are out of stock. Restock to avoid lost sales.`,
                action: 'Restock Now'
            });
        }

        renderRecommendations(recommendations);
    }

    function renderRecommendations(recommendations) {
        const container = document.getElementById('recommendations-list');
        
        if (recommendations.length === 0) {
            container.innerHTML = '<div class="empty-state"><span class="empty-icon">💡</span><p>No recommendations at this time</p></div>';
            return;
        }

        container.innerHTML = recommendations.map(rec => `
            <div class="recommendation-card ${rec.type}">
                <div class="rec-icon">${rec.icon}</div>
                <div class="rec-content">
                    <h3 class="rec-title">${rec.title}</h3>
                    <p class="rec-message">${rec.message}</p>
                </div>
                <button class="btn btn-sm btn-secondary rec-action">${rec.action}</button>
            </div>
        `).join('');
    }

    // --- UPDATE QUICK STATS ---
    function updateQuickStats(stats) {
        document.getElementById('total-items-stat').textContent = stats.total_items_in_stock.toLocaleString();
        document.getElementById('inventory-value-stat').textContent = `$${parseFloat(stats.total_inventory_value).toFixed(2)}`;
        document.getElementById('avg-margin-stat').textContent = `${parseFloat(stats.average_profit_margin).toFixed(1)}%`;
        document.getElementById('items-sold-stat').textContent = stats.total_items_sold.toLocaleString();
    }

    // --- EXPORT REPORT ---
    function exportReport() {
        showAlert('Export functionality coming soon! Will generate PDF report.', 'info');
        // In production, use a library like jsPDF to generate PDF
    }

    // --- HELPERS ---
    function animateValue(elementId, start, end, duration, isCurrency = false) {
        const element = document.getElementById(elementId);
        if (!element) return;
        
        let startTimestamp = null;
        const step = (timestamp) => {
            if (!startTimestamp) startTimestamp = timestamp;
            const progress = Math.min((timestamp - startTimestamp) / duration, 1);
            const value = progress * (end - start) + start;
            
            if(isCurrency) {
                 element.textContent = `$${value.toFixed(2)}`;
            } else {
                 element.textContent = value.toFixed(0);
            }

            if (progress < 1) {
                window.requestAnimationFrame(step);
            } else {
                // Ensure final value is set correctly
                if(isCurrency) {
                    element.textContent = `$${end.toFixed(2)}`;
                } else {
                    element.textContent = end.toFixed(0);
                }
            }
        };
        window.requestAnimationFrame(step);
    }

    function showAlert(message, type) {
        const container = document.getElementById('alert-container');
        const alert = document.createElement('div');
        alert.className = `alert alert-${type}`;
        alert.innerHTML = `
            <span class="alert-icon">${type === 'success' ? '✓' : type === 'info' ? 'ℹ️' : '⚠️'}</span>
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
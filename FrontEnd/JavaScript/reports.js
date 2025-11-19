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
    let summaryMetrics = {};
    let salesTrendData = {};
    let allProductPerformance = [];
    let categoryPerformanceData = [];
    let supplierPerformanceData = {};
    let stockStatusData = {};
    let inventoryStats = {};
    
    // Chart instances
    let charts = {};

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
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) {
                throw new Error(`Failed to fetch ${endpoint}: ${response.statusText}`);
            }
            return await response.json();
        } catch (error) {
            console.error(`Error fetching ${endpoint}:`, error);
            showAlert(`Failed to load data for ${endpoint}`, 'error');
            throw error; // Re-throw to stop Promise.all
        }
    }

    // --- LOAD ALL DATA ---
    async function loadAllData() {
        showAlert('Loading report data...', 'success');
        try {
            const [
                summary,
                salesTrend,
                productPerf,
                categoryPerf,
                supplierPerf,
                stockStatus,
                invStats
            ] = await Promise.all([
                fetchFromApi('/reports/summary'),
                fetchFromApi('/reports/sales_trend'),
                fetchFromApi('/reports/performance_by_product'),
                fetchFromApi('/reports/category_performance'),
                fetchFromApi('/reports/supplier_performance'),
                fetchFromApi('/reports/stock_status'),
                fetchFromApi('/reports/inventory_stats')
            ]);
            
            // Store fetched data globally
            summaryMetrics = summary;
            salesTrendData = salesTrend;
            allProductPerformance = productPerf;
            categoryPerformanceData = categoryPerf;
            supplierPerformanceData = supplierPerf;
            stockStatusData = stockStatus;
            inventoryStats = invStats;

            // Generate initial report
            generateReport();
            showAlert('Reports loaded successfully!', 'success');

        } catch (error) {
            console.error('Failed to load all dashboard data:', error);
            showAlert('Could not load all report data. Please try again.', 'error');
        }
    }

    // --- GENERATE MOCK SALES DATA (REMOVED) ---
    // This function is no longer needed.

    // --- GENERATE REPORT ---
    function generateReport() {
        // Calculate metrics
        calculateKeyMetrics(summaryMetrics);
        
        // Generate all charts with LIVE data
        createSalesTrendChart(salesTrendData.labels, salesTrendData.revenue, salesTrendData.profit);
        createRevenueCategoryChart(categoryPerformanceData.map(c => c.name), categoryPerformanceData.map(c => c.revenue));
        createTopProductsChart(allProductPerformance);
        createProfitMarginChart(allProductPerformance);
        createStockStatusChart(stockStatusData);
        createROIChart(categoryPerformanceData);
        createSupplierPerformanceChart(supplierPerformanceData.labels, supplierPerformanceData.data);
        
        // Update tables
        updateBestProducts();
        updateWorstProducts();
        
        // Generate recommendations
        generateRecommendations();
        
        // Update quick stats
        updateQuickStats(inventoryStats);
        
        // Update notification count
        document.getElementById('notification-count').textContent = stockStatusData.low_stock || 0;
    }

    // --- CALCULATE KEY METRICS ---
    function calculateKeyMetrics(summary) {
        animateValue('total-revenue', 0, parseFloat(summary.total_revenue), 1500, true);
        animateValue('total-profit', 0, parseFloat(summary.total_profit), 1500, true);
        
        document.getElementById('revenue-change').textContent = '+12.5%'; // Mock change
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
        const sorted = [...allProductPerformance].sort((a, b) => parseFloat(b[metric]) - parseFloat(a[metric])).slice(0, 10);
        renderPerformanceTable('best-products-table', sorted, metric, true);
    }

    function updateWorstProducts() {
        const metric = document.getElementById('worst-products-metric').value;
        const sorted = [...allProductPerformance].sort((a, b) => parseFloat(a[metric]) - parseFloat(b[metric])).slice(0, 10);
        renderPerformanceTable('worst-products-table', sorted, metric, false);
    }

    // --- CALCULATE PRODUCT PERFORMANCE (REMOVED) ---
    // This is now done on the backend.

    function renderPerformanceTable(containerId, products, metric, isBest) {
        const container = document.getElementById(containerId);
        
        if (products.length === 0) {
            container.innerHTML = '<div class="empty-state"><span class="empty-icon">📊</span><p>No data available</p></div>';
            return;
        }

        const formatValue = (value) => {
            if (metric === 'revenue' || metric === 'profit') return `$${parseFloat(value).toFixed(2)}`;
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
    function generateRecommendations() {
        const recommendations = [];

        // Low stock recommendations
        if (stockStatusData.low_stock > 0) {
            recommendations.push({
                type: 'warning',
                icon: '⚠️',
                title: 'Low Stock Alert',
                message: `${stockStatusData.low_stock} products need restocking. Consider reordering soon.`,
                action: 'View Items'
            });
        }

        // Best sellers recommendation
        const bestSellers = [...allProductPerformance].sort((a, b) => b.revenue - a.revenue);
        if (bestSellers.length > 0) {
            recommendations.push({
                type: 'success',
                icon: '🌟',
                title: 'Focus on Best Sellers',
                message: `${bestSellers[0].name} is your top performer. Ensure adequate stock levels.`,
                action: 'View Details'
            });
        }

        // High margin products
        const highMargin = [...allProductPerformance].sort((a, b) => b.roi - a.roi);
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
        const slowMovers = [...allProductPerformance].sort((a, b) => a.quantity - b.quantity);
        if (slowMovers.length > 0 && slowMovers[0].quantity < 5) {
            recommendations.push({
                type: 'info',
                icon: '📉',
                title: 'Slow Moving Items',
                message: `Consider discounting ${slowMovers[0].name} to improve turnover.`,
                action: 'Create Promotion'
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
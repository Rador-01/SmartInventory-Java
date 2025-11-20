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
    let currentDateRange = {
        start: null,
        end: null
    };

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
        document.getElementById('time-period').addEventListener('change', handleTimePeriodChange);

        // Load initial report
        generateReport();
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

    // --- HANDLE TIME PERIOD CHANGE ---
    function handleTimePeriodChange() {
        const period = document.getElementById('time-period').value;
        const customDateRange = document.getElementById('custom-date-range');

        if (period === 'custom') {
            customDateRange.style.display = 'flex';
        } else {
            customDateRange.style.display = 'none';
            currentDateRange = getDateRangeFromPeriod(period);
        }
    }

    // --- GET DATE RANGE FROM PERIOD ---
    function getDateRangeFromPeriod(period) {
        const end = new Date();
        let start = new Date();

        switch (period) {
            case 'today':
                start = new Date();
                break;
            case 'week':
                start.setDate(start.getDate() - 7);
                break;
            case 'month':
                start.setMonth(start.getMonth() - 1);
                break;
            case 'year':
                start.setFullYear(start.getFullYear() - 1);
                break;
            default:
                start.setDate(start.getDate() - 7);
        }

        return {
            start: start.toISOString().split('T')[0],
            end: end.toISOString().split('T')[0]
        };
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

    // --- GENERATE REPORT ---
    async function generateReport() {
        const timePeriod = document.getElementById('time-period').value;

        // Get date range
        let dateParams = '';
        if (timePeriod === 'custom') {
            const startDate = document.getElementById('date-from').value;
            const endDate = document.getElementById('date-to').value;
            if (startDate && endDate) {
                dateParams = `?startDate=${startDate}&endDate=${endDate}`;
                currentDateRange = { start: startDate, end: endDate };
            }
        } else {
            const range = getDateRangeFromPeriod(timePeriod);
            dateParams = `?startDate=${range.start}&endDate=${range.end}`;
            currentDateRange = range;
        }

        showAlert('Loading report data...', 'success');

        try {
            // Fetch all data at once using the optimized full report endpoint
            const fullReport = await fetchFromApi(`/reports/full${dateParams}&days=30`);

            // Update all sections with the data
            updateKeyMetrics(fullReport.summary);
            createSalesTrendChart(fullReport.salesTrend);
            createRevenueCategoryChart(fullReport.categoryPerformance);
            createTopProductsChart(fullReport.productPerformance);
            createProfitMarginChart(fullReport.productPerformance);
            createStockStatusChart(fullReport.stockStatus);
            createROIChart(fullReport.categoryPerformance);
            createSupplierPerformanceChart(fullReport.supplierPerformance);

            // Update tables
            updateBestProducts(fullReport.productPerformance);
            updateWorstProducts(fullReport.productPerformance);

            // Generate recommendations
            renderRecommendations(fullReport.recommendations);

            // Update quick stats
            updateQuickStats(fullReport.inventoryStats);

            // Update notification count
            document.getElementById('notification-count').textContent = fullReport.stockStatus.lowStock || 0;

            showAlert('Reports loaded successfully!', 'success');

        } catch (error) {
            console.error('Failed to load report data:', error);
            showAlert('Could not load report data. Please try again.', 'error');
        }
    }

    // --- UPDATE KEY METRICS ---
    function updateKeyMetrics(summary) {
        animateValue('total-revenue', 0, parseFloat(summary.totalRevenue), 1500, true);
        animateValue('total-profit', 0, parseFloat(summary.totalProfit), 1500, true);

        document.getElementById('revenue-change').textContent = '+12.5%'; // Mock for now
        document.getElementById('profit-margin').textContent = `${parseFloat(summary.profitMarginPercent).toFixed(1)}% margin`;
        document.getElementById('roi-value').textContent = `${parseFloat(summary.roiPercent).toFixed(1)}%`;
        document.getElementById('roi-status').textContent = summary.roiPercent > 20 ? 'Excellent' : summary.roiPercent > 10 ? 'Good' : 'Fair';
        document.getElementById('turnover-rate').textContent = `${parseFloat(summary.turnoverRate).toFixed(1)}x`;
        document.getElementById('turnover-status').textContent = summary.turnoverRate > 2 ? 'Excellent' : summary.turnoverRate > 1 ? 'Good' : 'Slow';
    }

    // --- CREATE SALES TREND CHART ---
    function createSalesTrendChart(data) {
        const ctx = document.getElementById('salesTrendChart');
        if (charts.salesTrend) charts.salesTrend.destroy();

        const labels = data.labels.map(date => new Date(date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));

        charts.salesTrend = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: 'Revenue',
                        data: data.revenue,
                        borderColor: '#2196F3',
                        backgroundColor: 'rgba(33, 150, 243, 0.1)',
                        tension: 0.4,
                        fill: true
                    },
                    {
                        label: 'Profit',
                        data: data.profit,
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
                interaction: {
                    mode: 'index',
                    intersect: false,
                },
                plugins: {
                    legend: {
                        position: 'top',
                        labels: {
                            usePointStyle: true,
                            padding: 15
                        }
                    },
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
    function createRevenueCategoryChart(categoryData) {
        const ctx = document.getElementById('revenueCategoryChart');
        if (charts.revenueCategory) charts.revenueCategory.destroy();

        const labels = categoryData.map(c => c.name);
        const data = categoryData.map(c => c.revenue);

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
                        '#00BCD4',
                        '#FFC107',
                        '#E91E63'
                    ]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'right',
                        labels: {
                            padding: 15,
                            usePointStyle: true
                        }
                    },
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
        const sorted = [...products]
            .filter(p => p.profitMargin > 0)
            .sort((a, b) => b.profitMargin - a.profitMargin)
            .slice(0, 10);
        const ctx = document.getElementById('profitMarginChart');
        if (charts.profitMargin) charts.profitMargin.destroy();

        charts.profitMargin = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: sorted.map(p => p.name),
                datasets: [{
                    label: 'Profit Margin (%)',
                    data: sorted.map(p => p.profitMargin),
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
                    data: [statusData.inStock, statusData.lowStock, statusData.outOfStock],
                    backgroundColor: ['#4CAF50', '#FF9800', '#F44336']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            padding: 15,
                            usePointStyle: true
                        }
                    }
                }
            }
        });
    }

    // --- CREATE ROI CHART ---
    function createROIChart(categoryData) {
        const sorted = [...categoryData]
            .filter(c => c.roi > 0)
            .sort((a, b) => b.roi - a.roi);
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
    function createSupplierPerformanceChart(supplierData) {
        const ctx = document.getElementById('supplierPerformanceChart');
        if (charts.supplierPerformance) charts.supplierPerformance.destroy();

        const labels = supplierData.map(s => s.name);
        const data = supplierData.map(s => s.revenue);

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
    function updateBestProducts(productPerformance = null) {
        if (!productPerformance) return;

        const metric = document.getElementById('best-products-metric').value;
        const sorted = [...productPerformance].sort((a, b) => parseFloat(b[metric]) - parseFloat(a[metric])).slice(0, 10);
        renderPerformanceTable('best-products-table', sorted, metric, true);
    }

    function updateWorstProducts(productPerformance = null) {
        if (!productPerformance) return;

        const metric = document.getElementById('worst-products-metric').value;
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
            if (metric === 'roi' || metric === 'profitMargin') return `${parseFloat(value).toFixed(1)}%`;
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

    // --- RENDER RECOMMENDATIONS ---
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
        document.getElementById('total-items-stat').textContent = stats.totalItemsInStock.toLocaleString();
        document.getElementById('inventory-value-stat').textContent = `$${parseFloat(stats.totalInventoryValue).toFixed(2)}`;
        document.getElementById('avg-margin-stat').textContent = `${parseFloat(stats.averageProfitMargin).toFixed(1)}%`;
        document.getElementById('items-sold-stat').textContent = stats.totalItemsSold.toLocaleString();
    }

    // --- EXPORT REPORT ---
    function exportReport() {
        showAlert('Export functionality coming soon! Will generate PDF report.', 'info');
        // TODO: Implement PDF export using jsPDF
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

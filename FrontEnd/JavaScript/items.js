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
    let allStock = [];
    let allSuppliers = [];
    let allCategories = [];
    let allClients = [];

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
        setupToggleButtons();
        setupFormHandlers();
        setupSearchFilters();

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

    // --- TOGGLE BUTTONS ---
    function setupToggleButtons() {
        document.getElementById('toggle-add-product').addEventListener('click', () => {
            const section = document.getElementById('add-product-section');
            section.style.display = section.style.display === 'none' ? 'block' : 'none';
        });

        document.getElementById('toggle-add-stock').addEventListener('click', () => {
            const section = document.getElementById('add-stock-section');
            section.style.display = section.style.display === 'none' ? 'block' : 'none';
        });

        document.getElementById('toggle-add-supplier').addEventListener('click', () => {
            const section = document.getElementById('add-supplier-section');
            section.style.display = section.style.display === 'none' ? 'block' : 'none';
        });

        document.getElementById('toggle-add-category').addEventListener('click', () => {
            const section = document.getElementById('add-category-section');
            section.style.display = section.style.display === 'none' ? 'block' : 'none';
        });
    }

    // --- FORM HANDLERS ---
    function setupFormHandlers() {
        document.getElementById('add-product-form').addEventListener('submit', createProduct);
        document.getElementById('add-stock-form').addEventListener('submit', createStock);
        document.getElementById('add-supplier-form').addEventListener('submit', createSupplier);
        document.getElementById('add-category-form').addEventListener('submit', createCategory);
    }

    // --- SEARCH AND FILTERS ---
    function setupSearchFilters() {
        // Product search and filter
        document.getElementById('product-search').addEventListener('input', (e) => {
            const searchTerm = e.target.value.toLowerCase();
            const categoryId = document.getElementById('product-category-filter').value;
            filterProducts(searchTerm, categoryId);
        });

        document.getElementById('product-category-filter').addEventListener('change', (e) => {
            const searchTerm = document.getElementById('product-search').value.toLowerCase();
            const categoryId = e.target.value;
            filterProducts(searchTerm, categoryId);
        });

        // Stock search and filter
        document.getElementById('stock-search').addEventListener('input', (e) => {
            const searchTerm = e.target.value.toLowerCase();
            const filter = document.getElementById('stock-filter').value;
            filterStock(searchTerm, filter);
        });

        document.getElementById('stock-filter').addEventListener('change', (e) => {
            const searchTerm = document.getElementById('stock-search').value.toLowerCase();
            const filter = e.target.value;
            filterStock(searchTerm, filter);
        });

        // Supplier search
        document.getElementById('supplier-search').addEventListener('input', (e) => {
            const searchTerm = e.target.value.toLowerCase();
            filterSuppliers(searchTerm);
        });

        // Category search
        document.getElementById('category-search').addEventListener('input', (e) => {
            const searchTerm = e.target.value.toLowerCase();
            filterCategories(searchTerm);
        });
    }

    // --- FILTER FUNCTIONS ---
    function filterProducts(searchTerm, categoryId) {
        let filtered = allProducts;

        if (searchTerm) {
            filtered = filtered.filter(p =>
                (p.name || '').toLowerCase().includes(searchTerm) ||
                (p.brand || '').toLowerCase().includes(searchTerm) ||
                (p.sku || '').toLowerCase().includes(searchTerm)
            );
        }

        if (categoryId) {
            filtered = filtered.filter(p => p.category?.id == categoryId);
        }

        renderProducts(filtered);
    }

    function filterStock(searchTerm, filter) {
        let filtered = allStock;

        if (searchTerm) {
            filtered = filtered.filter(s =>
                (s.product_name || '').toLowerCase().includes(searchTerm)
            );
        }

        if (filter === 'low') {
            filtered = filtered.filter(s => s.quantity > 0 && s.quantity < 10);
        } else if (filter === 'out') {
            filtered = filtered.filter(s => s.quantity === 0);
        }

        renderStock(filtered);
    }

    function filterSuppliers(searchTerm) {
        const filtered = allSuppliers.filter(s =>
            (s.name || '').toLowerCase().includes(searchTerm) ||
            (s.contactPerson || '').toLowerCase().includes(searchTerm)
        );
        renderSuppliers(filtered);
    }

    function filterCategories(searchTerm) {
        const filtered = allCategories.filter(c =>
            (c.name || '').toLowerCase().includes(searchTerm)
        );
        renderCategories(filtered);
    }

    // --- LOAD ALL DATA ---
    async function loadAllData() {
        try {
            const [products, suppliers, categories] = await Promise.all([
                fetchFromApi('/products'),
                fetchFromApi('/suppliers'),
                fetchFromApi('/categories')
            ]);

            allProducts = products;
            allSuppliers = suppliers;
            allCategories = categories;

            // Create stock data from products
            allStock = allProducts.map(p => ({
                id: p.id,
                product_name: p.name,
                product_id: p.id,
                quantity: p.currentStock || 0,
                selling_price: p.sellingPrice || 0,
                supplier_name: p.supplier?.name || 'N/A',
                location: 'N/A'
            }));

            // Update UI
            updateMetrics();
            populateDropdowns();
            renderProducts(allProducts);
            renderStock(allStock);
            renderSuppliers(allSuppliers);
            renderCategories(allCategories);

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

    // --- UPDATE METRICS ---
    function updateMetrics() {
        animateValue('total-products', 0, allProducts.length, 1000);
        animateValue('total-suppliers', 0, allSuppliers.length, 1000);
        animateValue('total-categories', 0, allCategories.length, 1000);
        animateValue('total-stock-items', 0, allStock.length, 1000);

        // Update notification count (low stock items)
        const lowStockCount = allProducts.filter(p => {
            const stock = p.currentStock || 0;
            return stock > 0 && stock < 10;
        }).length;
        document.getElementById('notification-count').textContent = lowStockCount;
    }

    // --- POPULATE DROPDOWNS ---
    function populateDropdowns() {
        // Product category dropdown
        const productCategorySelect = document.getElementById('product-category');
        const productCategoryFilter = document.getElementById('product-category-filter');
        productCategorySelect.innerHTML = '<option value="">Select a category...</option>';
        productCategoryFilter.innerHTML = '<option value="">All Categories</option>';
        allCategories.forEach(cat => {
            productCategorySelect.innerHTML += `<option value="${cat.id}">${cat.name}</option>`;
            productCategoryFilter.innerHTML += `<option value="${cat.id}">${cat.name}</option>`;
        });

        // Stock product dropdown
        const stockProductSelect = document.getElementById('stock-product');
        stockProductSelect.innerHTML = '<option value="">Select a product...</option>';
        allProducts.forEach(prod => {
            stockProductSelect.innerHTML += `<option value="${prod.id}">${prod.name}</option>`;
        });

        // Stock supplier dropdown
        const stockSupplierSelect = document.getElementById('stock-supplier');
        stockSupplierSelect.innerHTML = '<option value="">Select a supplier...</option>';
        allSuppliers.forEach(sup => {
            stockSupplierSelect.innerHTML += `<option value="${sup.id}">${sup.name}</option>`;
        });
    }

    // --- CREATE PRODUCT ---
    async function createProduct(e) {
        e.preventDefault();

        const categoryId = document.getElementById('product-category').value;

        const body = {
            name: document.getElementById('product-name').value,
            brand: document.getElementById('product-brand').value || null,
            description: document.getElementById('product-description').value || null,
            sku: `SKU-${Date.now()}`,
            costPrice: 0,
            sellingPrice: 0,
            currentStock: 0,
            category: categoryId ? { id: parseInt(categoryId) } : null
        };

        try {
            const response = await fetch(`${API_BASE_URL}/products`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(body)
            });

            const result = await response.json();
            displayResponse('product-response', result, response.status);

            if (response.ok) {
                showAlert('Product created successfully!', 'success');
                e.target.reset();
                await loadAllData();
            } else {
                showAlert(result.error || 'Failed to create product', 'error');
            }
        } catch (error) {
            displayResponse('product-response', { message: `Network error: ${error.message}` }, 0);
            showAlert('Network error occurred', 'error');
        }
    }

    // --- CREATE STOCK ---
    async function createStock(e) {
        e.preventDefault();

        const productId = parseInt(document.getElementById('stock-product').value);
        const supplierId = parseInt(document.getElementById('stock-supplier').value);
        const quantity = parseInt(document.getElementById('stock-quantity').value);
        const costPrice = parseFloat(document.getElementById('stock-cost-price').value) || 0;
        const sellingPrice = parseFloat(document.getElementById('stock-selling-price').value);

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
            displayResponse('stock-response', result, stockResponse.status);

            if (stockResponse.ok) {
                showAlert('Stock added successfully!', 'success');
                e.target.reset();
                await loadAllData();
            } else {
                showAlert(result.error || 'Failed to add stock', 'error');
            }

        } catch (error) {
            displayResponse('stock-response', { message: `Network error: ${error.message}` }, 0);
            showAlert('Network error occurred', 'error');
        }
    }

    // --- CREATE SUPPLIER ---
    async function createSupplier(e) {
        e.preventDefault();

        const body = {
            name: document.getElementById('supplier-name').value,
            contactPerson: document.getElementById('supplier-contact-person').value || null,
            phone: document.getElementById('supplier-phone').value || null,
            email: document.getElementById('supplier-email').value || null,
            address: document.getElementById('supplier-address').value || null,
            additionalFees: parseFloat(document.getElementById('supplier-fees').value) || 0
        };

        try {
            const response = await fetch(`${API_BASE_URL}/suppliers`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(body)
            });

            const result = await response.json();
            displayResponse('supplier-response', result, response.status);

            if (response.ok) {
                showAlert('Supplier created successfully!', 'success');
                e.target.reset();
                await loadAllData();
            } else {
                showAlert(result.error || 'Failed to create supplier', 'error');
            }
        } catch (error) {
            displayResponse('supplier-response', { message: `Network error: ${error.message}` }, 0);
            showAlert('Network error occurred', 'error');
        }
    }

    // --- CREATE CATEGORY ---
    async function createCategory(e) {
        e.preventDefault();

        const body = {
            name: document.getElementById('category-name').value,
            description: document.getElementById('category-desc').value || null
        };

        try {
            const response = await fetch(`${API_BASE_URL}/categories`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(body)
            });

            const result = await response.json();
            displayResponse('category-response', result, response.status);

            if (response.ok) {
                showAlert('Category created successfully!', 'success');
                e.target.reset();
                await loadAllData();
            } else {
                showAlert(result.error || 'Failed to create category', 'error');
            }
        } catch (error) {
            displayResponse('category-response', { message: `Network error: ${error.message}` }, 0);
            showAlert('Network error occurred', 'error');
        }
    }

    // --- RENDER FUNCTIONS ---
    function renderProducts(products) {
        const container = document.getElementById('products-list');
        if (products.length === 0) {
            container.innerHTML = '<div class="empty-state"><span class="empty-icon">📦</span><p>No products found</p></div>';
            return;
        }
        container.innerHTML = products.map((p, index) => `
            <div class="item-list-row" style="animation: slideInLeft 0.3s ease ${index * 0.05}s forwards; opacity: 0;">
                <div class="item-info">
                    <div class="item-name"><strong>${p.name}</strong> ${p.brand ? `(${p.brand})` : ''}</div>
                    <div class="item-details">
                        ID: ${p.id} | SKU: ${p.sku} | Category: ${p.category?.name || 'N/A'} |
                        Price: $${p.sellingPrice || 'N/A'} | Stock: ${p.currentStock || 0}
                    </div>
                </div>
                <div class="item-actions">
                    <button class="btn btn-secondary btn-sm view-btn" onclick="viewProduct(${p.id})">View</button>
                    <button class="btn btn-danger btn-sm delete-btn" onclick="deleteProduct(${p.id})">Delete</button>
                </div>
            </div>
        `).join('');
    }

    function renderStock(stock) {
        const container = document.getElementById('stock-list');
        if (stock.length === 0) {
            container.innerHTML = '<div class="empty-state"><span class="empty-icon">📦</span><p>No stock found</p></div>';
            return;
        }
        container.innerHTML = stock.map((s, index) => `
            <div class="item-list-row" style="animation: slideInLeft 0.3s ease ${index * 0.05}s forwards; opacity: 0;">
                <div class="item-info">
                    <div class="item-name"><strong>${s.product_name}</strong></div>
                    <div class="item-details">
                        Quantity: ${s.quantity} | Price: $${s.selling_price} | Supplier: ${s.supplier_name}
                    </div>
                </div>
                <div class="item-actions">
                    <span class="badge ${s.quantity === 0 ? 'badge-danger' : s.quantity < 10 ? 'badge-warning' : 'badge-success'}">
                        ${s.quantity === 0 ? 'Out of Stock' : s.quantity < 10 ? 'Low Stock' : 'In Stock'}
                    </span>
                </div>
            </div>
        `).join('');
    }

    function renderSuppliers(suppliers) {
        const container = document.getElementById('suppliers-list');
        if (suppliers.length === 0) {
            container.innerHTML = '<div class="empty-state"><span class="empty-icon">🏢</span><p>No suppliers found</p></div>';
            return;
        }
        container.innerHTML = suppliers.map((s, index) => `
            <div class="item-list-row" style="animation: slideInLeft 0.3s ease ${index * 0.05}s forwards; opacity: 0;">
                <div class="item-info">
                    <div class="item-name"><strong>${s.name}</strong></div>
                    <div class="item-details">
                        Contact: ${s.contactPerson || 'N/A'} | Phone: ${s.phone || 'N/A'} | Email: ${s.email || 'N/A'}
                    </div>
                </div>
                <div class="item-actions">
                    <button class="btn btn-secondary btn-sm view-btn" onclick="viewSupplier(${s.id})">View</button>
                    <button class="btn btn-danger btn-sm delete-btn" onclick="deleteSupplier(${s.id})">Delete</button>
                </div>
            </div>
        `).join('');
    }

    function renderCategories(categories) {
        const container = document.getElementById('categories-list');
        if (categories.length === 0) {
            container.innerHTML = '<div class="empty-state"><span class="empty-icon">📂</span><p>No categories found</p></div>';
            return;
        }
        container.innerHTML = categories.map((c, index) => `
            <div class="item-list-row" style="animation: slideInLeft 0.3s ease ${index * 0.05}s forwards; opacity: 0;">
                <div class="item-info">
                    <div class="item-name"><strong>${c.name}</strong></div>
                    <div class="item-details">${c.description || 'No description'}</div>
                </div>
                <div class="item-actions">
                    <button class="btn btn-secondary btn-sm view-btn" onclick="viewCategory(${c.id})">View</button>
                    <button class="btn btn-danger btn-sm delete-btn" onclick="deleteCategory(${c.id})">Delete</button>
                </div>
            </div>
        `).join('');
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

    // --- GLOBAL FUNCTIONS FOR ONCLICK ---
    window.viewProduct = async (id) => {
        alert(`View product ${id} - Feature coming soon`);
    };

    window.deleteProduct = async (id) => {
        if (!confirm('Are you sure you want to delete this product?')) return;

        try {
            const response = await fetch(`${API_BASE_URL}/products/${id}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                showAlert('Product deleted successfully!', 'success');
                await loadAllData();
            } else {
                showAlert('Failed to delete product', 'error');
            }
        } catch (error) {
            showAlert('Network error occurred', 'error');
        }
    };

    window.viewSupplier = async (id) => {
        alert(`View supplier ${id} - Feature coming soon`);
    };

    window.deleteSupplier = async (id) => {
        if (!confirm('Are you sure you want to delete this supplier?')) return;

        try {
            const response = await fetch(`${API_BASE_URL}/suppliers/${id}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                showAlert('Supplier deleted successfully!', 'success');
                await loadAllData();
            } else {
                showAlert('Failed to delete supplier', 'error');
            }
        } catch (error) {
            showAlert('Network error occurred', 'error');
        }
    };

    window.viewCategory = async (id) => {
        alert(`View category ${id} - Feature coming soon`);
    };

    window.deleteCategory = async (id) => {
        if (!confirm('Are you sure you want to delete this category?')) return;

        try {
            const response = await fetch(`${API_BASE_URL}/categories/${id}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                showAlert('Category deleted successfully!', 'success');
                await loadAllData();
            } else {
                showAlert('Failed to delete category', 'error');
            }
        } catch (error) {
            showAlert('Network error occurred', 'error');
        }
    };

    // --- RUN THE APP ---
    initPage();
});

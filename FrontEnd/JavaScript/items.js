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
    let allProducts = [];
    let allSuppliers = [];
    let allCategories = [];
    let allStock = [];

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

        // Toggle sections
        document.getElementById('toggle-add-product')?.addEventListener('click', () => toggleSection('add-product-section'));
        document.getElementById('toggle-add-stock')?.addEventListener('click', () => toggleSection('add-stock-section'));
        document.getElementById('toggle-add-supplier')?.addEventListener('click', () => toggleSection('add-supplier-section'));
        document.getElementById('toggle-add-category')?.addEventListener('click', () => toggleSection('add-category-section'));

        // Form Submissions
        document.getElementById('add-product-form').addEventListener('submit', createProduct);
        document.getElementById('add-stock-form').addEventListener('submit', createStock);
        document.getElementById('add-supplier-form').addEventListener('submit', createSupplier);
        document.getElementById('add-category-form').addEventListener('submit', createCategory);

        // Search & Filter
        document.getElementById('product-search')?.addEventListener('input', filterProducts);
        document.getElementById('product-category-filter')?.addEventListener('change', filterProducts);
        document.getElementById('stock-search')?.addEventListener('input', filterStock);
        document.getElementById('stock-filter')?.addEventListener('change', filterStock);
        document.getElementById('supplier-search')?.addEventListener('input', filterSuppliers);
        document.getElementById('category-search')?.addEventListener('input', filterCategories);

        // Delete Button Listeners (using event delegation)
        document.getElementById('products-list').addEventListener('click', (e) => handleDelete(e, 'product'));
        document.getElementById('stock-list').addEventListener('click', (e) => handleDelete(e, 'stock'));
        document.getElementById('suppliers-list').addEventListener('click', (e) => handleDelete(e, 'supplier'));
        document.getElementById('categories-list').addEventListener('click', (e) => handleDelete(e, 'category'));

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

    // --- TOGGLE SECTION ---
    function toggleSection(sectionId) {
        const section = document.getElementById(sectionId);
        section.style.display = section.style.display === 'none' ? 'block' : 'none';
    }

    // --- LOAD ALL DATA ---
    async function loadAllData() {
        await Promise.all([
            fetchProducts(),
            fetchSuppliers(),
            fetchCategories(),
            fetchStock()
        ]);
        updateMetrics();
        populateDropdowns();
    }

    // --- UPDATE METRICS ---
    function updateMetrics() {
        animateValue('total-products', 0, allProducts.length, 1000);
        animateValue('total-suppliers', 0, allSuppliers.length, 1000);
        animateValue('total-categories', 0, allCategories.length, 1000);
        animateValue('total-stock-items', 0, allStock.length, 1000);

        // Update notification count (low stock items)
        const lowStockCount = allStock.filter(s => s.quantity < 10).length;
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

    // --- FETCH FUNCTIONS ---
    async function fetchProducts() {
        try {
            const response = await fetch(`${API_BASE_URL}/products`);
            if (!response.ok) throw new Error('Failed to fetch products');
            allProducts = await response.json();
            renderProducts(allProducts);
        } catch (error) {
            console.error('Error fetching products:', error);
            showAlert('Failed to load products', 'error');
            document.getElementById('products-list').innerHTML = '<p class="text-danger">Error loading products</p>';
        }
    }

    async function fetchSuppliers() {
        try {
            const response = await fetch(`${API_BASE_URL}/suppliers`);
            if (!response.ok) throw new Error('Failed to fetch suppliers');
            allSuppliers = await response.json();
            renderSuppliers(allSuppliers);
        } catch (error) {
            console.error('Error fetching suppliers:', error);
            showAlert('Failed to load suppliers', 'error');
            document.getElementById('suppliers-list').innerHTML = '<p class="text-danger">Error loading suppliers</p>';
        }
    }

    async function fetchCategories() {
        try {
            const response = await fetch(`${API_BASE_URL}/categories`);
            if (!response.ok) throw new Error('Failed to fetch categories');
            allCategories = await response.json();
            renderCategories(allCategories);
        } catch (error) {
            console.error('Error fetching categories:', error);
            showAlert('Failed to load categories', 'error');
            document.getElementById('categories-list').innerHTML = '<p class="text-danger">Error loading categories</p>';
        }
    }

    async function fetchStock() {
        try {
            const response = await fetch(`${API_BASE_URL}/stocks`);
            if (!response.ok) throw new Error('Failed to fetch stock');
            allStock = await response.json();
            renderStock(allStock);
        } catch (error) {
            console.error('Error fetching stock:', error);
            showAlert('Failed to load stock', 'error');
            document.getElementById('stock-list').innerHTML = '<p class="text-danger">Error loading stock</p>';
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
                        ID: ${p.id} | Category: ${p.category_name || 'N/A'} | Warranty: ${p.warranty_months || 0} months
                    </div>
                </div>
                <div class="item-actions">
                    <button class="btn btn-secondary btn-sm view-btn" data-id="${p.id}" data-type="product">View</button>
                    <button class="btn btn-danger btn-sm delete-btn" data-id="${p.id}" data-type="product">Delete</button>
                </div>
            </div>
        `).join('');
    }

    function renderStock(stocks) {
        const container = document.getElementById('stock-list');
        if (stocks.length === 0) {
            container.innerHTML = '<div class="empty-state"><span class="empty-icon">📊</span><p>No stock items found</p></div>';
            return;
        }
        container.innerHTML = stocks.map((s, index) => {
            const isLowStock = s.quantity < 10;
            const isOutOfStock = s.quantity === 0;
            const statusClass = isOutOfStock ? 'status-out' : isLowStock ? 'status-low' : 'status-good';
            const statusText = isOutOfStock ? '❌ Out' : isLowStock ? '⚠️ Low' : '✅ Good';
            
            return `
            <div class="item-list-row ${statusClass}" style="animation: slideInLeft 0.3s ease ${index * 0.05}s forwards; opacity: 0;">
                <div class="item-info">
                    <div class="item-name"><strong>${s.product_name || 'Unknown'}</strong> ${statusText}</div>
                    <div class="item-details">
                        Qty: ${s.quantity} | Price: $${s.selling_price} | Supplier: ${s.supplier_name || 'N/A'} | Location: ${s.location || 'N/A'}
                    </div>
                </div>
                <div class="item-actions">
                    <button class="btn btn-secondary btn-sm edit-btn" data-id="${s.id}" data-type="stock">Edit</button>
                    <button class="btn btn-danger btn-sm delete-btn" data-id="${s.id}" data-type="stock">Delete</button>
                </div>
            </div>
        `}).join('');
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
                        Contact: ${s.contact_person || 'N/A'} | Phone: ${s.phone || 'N/A'} | Email: ${s.email || 'N/A'}
                    </div>
                </div>
                <div class="item-actions">
                    <button class="btn btn-secondary btn-sm view-btn" data-id="${s.id}" data-type="supplier">View</button>
                    <button class="btn btn-danger btn-sm delete-btn" data-id="${s.id}" data-type="supplier">Delete</button>
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
                    <button class="btn btn-danger btn-sm delete-btn" data-id="${c.id}" data-type="category">Delete</button>
                </div>
            </div>
        `).join('');
    }

    // --- CREATE FUNCTIONS ---
    async function createProduct(e) {
        e.preventDefault();
        const body = {
            name: document.getElementById('product-name').value,
            brand: document.getElementById('product-brand').value,
            description: document.getElementById('product-description').value,
            warranty_months: parseInt(document.getElementById('product-warranty').value) || null,
            category_id: parseInt(document.getElementById('product-category').value)
        };
        await createData('/products', body, 'product-response', fetchProducts);
        e.target.reset();
    }

    async function createStock(e) {
        e.preventDefault();
        const body = {
            product_id: parseInt(document.getElementById('stock-product').value),
            supplier_id: parseInt(document.getElementById('stock-supplier').value),
            quantity: parseInt(document.getElementById('stock-quantity').value),
            cost_price: parseFloat(document.getElementById('stock-cost-price').value) || null,
            selling_price: parseFloat(document.getElementById('stock-selling-price').value),
            location: document.getElementById('stock-location').value || null,
            expiration_date: document.getElementById('stock-expiration').value || null
        };
        await createData('/stocks', body, 'stock-response', fetchStock);
        e.target.reset();
    }

    async function createSupplier(e) {
        e.preventDefault();
        const body = {
            name: document.getElementById('supplier-name').value,
            contact_person: document.getElementById('supplier-contact-person').value,
            phone: document.getElementById('supplier-phone').value,
            email: document.getElementById('supplier-email').value,
            address: document.getElementById('supplier-address').value,
            additional_fees: parseFloat(document.getElementById('supplier-fees').value) || null
        };
        await createData('/suppliers', body, 'supplier-response', fetchSuppliers);
        e.target.reset();
    }

    async function createCategory(e) {
        e.preventDefault();
        const body = {
            name: document.getElementById('category-name').value,
            description: document.getElementById('category-desc').value
        };
        await createData('/categories', body, 'category-response', fetchCategories);
        e.target.reset();
    }

    // --- GENERIC CREATE ---
    async function createData(endpoint, body, responseElement, refreshFunc) {
        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(body)
            });

            const result = await response.json(); // 'result' IS the new item
            displayResponse(responseElement, result, response.status);

            if (response.ok) {
                showAlert('Item created successfully!', 'success');

                // --- START OF THE NEW FIX ---
                // Manually add the new item to the correct local cache
                // This avoids re-fetching and prevents the double-render bug.
                
                if (refreshFunc === fetchProducts) {
                    allProducts.push(result);
                    renderProducts(allProducts);
                } else if (refreshFunc === fetchStock) {
                    allStock.push(result);
                    renderStock(allStock);
                } else if (refreshFunc === fetchSuppliers) {
                    allSuppliers.push(result);
                    renderSuppliers(allSuppliers);
                } else if (refreshFunc === fetchCategories) {
                    allCategories.push(result);
                    renderCategories(allCategories);
                }
                
                // Now, manually update the other parts of the page that depend on this new data
                updateMetrics();
                populateDropdowns();
                // We NO LONGER call await loadAllData() here.
                // --- END OF THE NEW FIX ---

            } else {
                showAlert(result.message || 'Failed to create item', 'error');
            }
        } catch (error) {
            displayResponse(responseElement, { message: `Network error: ${error.message}` }, 0);
            showAlert('Network error occurred', 'error');
        }
    }

    // --- DELETE ---
    async function handleDelete(e, type) {
        if (!e.target.classList.contains('delete-btn')) return;
        
        if (!confirm('Are you sure you want to delete this item?')) return;
        
        const id = e.target.dataset.id;
        const endpoints = {
            product: `/products/${id}`,
            stock: `/stocks/${id}`,
            supplier: `/suppliers/${id}`,
            category: `/categories/${id}`
        };
        
        try {
            const response = await fetch(`${API_BASE_URL}${endpoints[type]}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.status === 204 || response.ok) {
                showAlert('Item deleted successfully!', 'success');
                await loadAllData();
            } else {
                const result = await response.json();
                showAlert(result.message || 'Failed to delete item', 'error');
            }
        } catch (error) {
            showAlert('Network error occurred', 'error');
        }
    }

    // --- FILTER FUNCTIONS ---
    function filterProducts() {
        const searchTerm = document.getElementById('product-search').value.toLowerCase();
        const categoryFilter = document.getElementById('product-category-filter').value;
        
        const filtered = allProducts.filter(p => {
            const matchesSearch = p.name.toLowerCase().includes(searchTerm) || 
                                  (p.brand && p.brand.toLowerCase().includes(searchTerm));
            const matchesCategory = !categoryFilter || p.category_id == categoryFilter;
            return matchesSearch && matchesCategory;
        });
        
        renderProducts(filtered);
    }

    function filterStock() {
        const searchTerm = document.getElementById('stock-search').value.toLowerCase();
        const filterType = document.getElementById('stock-filter').value;
        
        let filtered = allStock.filter(s => 
            (s.product_name && s.product_name.toLowerCase().includes(searchTerm)) ||
            (s.location && s.location.toLowerCase().includes(searchTerm))
        );
        
        if (filterType === 'low') {
            filtered = filtered.filter(s => s.quantity > 0 && s.quantity < 10);
        } else if (filterType === 'out') {
            filtered = filtered.filter(s => s.quantity === 0);
        }
        
        renderStock(filtered);
    }

    function filterSuppliers() {
        const searchTerm = document.getElementById('supplier-search').value.toLowerCase();
        const filtered = allSuppliers.filter(s =>
            s.name.toLowerCase().includes(searchTerm) ||
            (s.contact_person && s.contact_person.toLowerCase().includes(searchTerm)) ||
            (s.email && s.email.toLowerCase().includes(searchTerm))
        );
        renderSuppliers(filtered);
    }

    function filterCategories() {
        const searchTerm = document.getElementById('category-search').value.toLowerCase();
        const filtered = allCategories.filter(c =>
            c.name.toLowerCase().includes(searchTerm) ||
            (c.description && c.description.toLowerCase().includes(searchTerm))
        );
        renderCategories(filtered);
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
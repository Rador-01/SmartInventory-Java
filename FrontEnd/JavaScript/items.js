// KEY CHANGES FOR ITEMS.JS TO WORK WITH SPRING BOOT BACKEND

// 1. UPDATE CREATE PRODUCT FUNCTION
async function createProduct(e) {
    e.preventDefault();
    
    // Get category ID
    const categoryId = document.getElementById('product-category').value;
    
    const body = {
        name: document.getElementById('product-name').value,
        brand: document.getElementById('product-brand').value || null,
        description: document.getElementById('product-description').value || null,
        sku: `SKU-${Date.now()}`, // Generate unique SKU
        costPrice: null, // Will be set when adding stock
        sellingPrice: null, // Will be set when adding stock
        category: categoryId ? { id: parseInt(categoryId) } : null
    };
    
    await createData('/products', body, 'product-response', fetchProducts);
    e.target.reset();
}

// 2. UPDATE CREATE STOCK FUNCTION
async function createStock(e) {
    e.preventDefault();
    
    const body = {
        product: { id: parseInt(document.getElementById('stock-product').value) },
        supplier: { id: parseInt(document.getElementById('stock-supplier').value) },
        quantity: parseInt(document.getElementById('stock-quantity').value),
        costPrice: parseFloat(document.getElementById('stock-cost-price').value) || null,
        sellingPrice: parseFloat(document.getElementById('stock-selling-price').value)
    };
    
    // For Spring Boot, we need to:
    // 1. First update the product with prices
    // 2. Then add stock movement
    
    try {
        // Update product prices
        await fetch(`${API_BASE_URL}/products/${body.product.id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                costPrice: body.costPrice,
                sellingPrice: body.sellingPrice,
                supplier: body.supplier
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
                productId: body.product.id,
                quantity: body.quantity,
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

// 3. UPDATE RENDER FUNCTIONS TO HANDLE SPRING BOOT DATA STRUCTURE
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
                <button class="btn btn-secondary btn-sm view-btn" data-id="${p.id}" data-type="product">View</button>
                <button class="btn btn-danger btn-sm delete-btn" data-id="${p.id}" data-type="product">Delete</button>
            </div>
        </div>
    `).join('');
}

// 4. UPDATE RENDER STOCK TO GET STOCK MOVEMENTS
async function fetchStock() {
    try {
        // In Spring Boot, we fetch products with their stock levels
        const response = await fetch(`${API_BASE_URL}/products`);
        if (!response.ok) throw new Error('Failed to fetch products');
        allProducts = await response.json();
        
        // Filter to show only products with stock info
        allStock = allProducts.map(p => ({
            id: p.id,
            product_name: p.name,
            product_id: p.id,
            quantity: p.currentStock || 0,
            selling_price: p.sellingPrice || 0,
            supplier_name: p.supplier?.name || 'N/A',
            location: 'N/A' // Not tracked in current backend
        }));
        
        renderStock(allStock);
    } catch (error) {
        console.error('Error fetching stock:', error);
        showAlert('Failed to load stock', 'error');
        document.getElementById('stock-list').innerHTML = '<p class="text-danger">Error loading stock</p>';
    }
}

// 5. POPULATE DROPDOWNS CORRECTLY
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

// 6. UPDATE METRICS TO HANDLE SPRING BOOT DATA
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
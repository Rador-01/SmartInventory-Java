// KEY CHANGES FOR TRANSACTIONS.JS TO WORK WITH SPRING BOOT BACKEND

// 1. UPDATE PROCESS SALE FUNCTION
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

// 2. UPDATE POPULATE SALE STOCK OPTIONS
function populateSaleStockOptions(select) {
    select.innerHTML = '<option value="">Select stock item...</option>';
    // In Spring Boot, we use products directly (not separate stock items)
    allProducts.filter(p => (p.currentStock || 0) > 0).forEach(product => {
        const stock = product.currentStock || 0;
        const price = product.sellingPrice || 0;
        select.innerHTML += `<option value="${product.id}" data-price="${price}" data-qty="${stock}">${product.name} - $${price} (Available: ${stock})</option>`;
    });
}

// 3. UPDATE FETCH TRANSACTION HISTORY
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

// 4. UPDATE PROCESS RETURN
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
        // In Spring Boot, we add stock back if restocking
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
        }

    } catch (error) {
        displayResponse('return-response', { message: `Network error: ${error.message}` }, 0);
        showAlert('Network error occurred', 'error');
    }
}

// 5. UPDATE PROCESS REMOVAL
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

// 6. UPDATE UPDATE METRICS
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
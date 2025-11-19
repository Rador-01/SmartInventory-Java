package com.smartinventory.controller;

import com.smartinventory.model.SaleItem;
import com.smartinventory.service.SaleItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SaleItemController - Handles individual sale item endpoints
 *
 * Note: Usually sale items are created with sales, but this controller
 * provides additional endpoints for querying and managing individual items.
 *
 * Endpoints:
 * GET    /api/sale-items                - Get all sale items
 * GET    /api/sale-items/{id}           - Get sale item by ID
 * GET    /api/sale-items/sale/{saleId}  - Get items by sale
 * GET    /api/sale-items/product/{productId} - Get items by product
 * PUT    /api/sale-items/{id}           - Update sale item
 * DELETE /api/sale-items/{id}           - Delete sale item
 * GET    /api/sale-items/product/{id}/sold - Total quantity sold
 * GET    /api/sale-items/product/{id}/revenue - Total revenue
 * GET    /api/sale-items/top-selling    - Top selling products
 * GET    /api/sale-items/with-discount  - Items with discounts
 */
@RestController
@RequestMapping("/api/sale-items")
@CrossOrigin(origins = "*")
public class SaleItemController {

    private final SaleItemService saleItemService;

    @Autowired
    public SaleItemController(SaleItemService saleItemService) {
        this.saleItemService = saleItemService;
    }

    /**
     * GET /api/sale-items
     * Get all sale items
     */
    @GetMapping
    public ResponseEntity<List<SaleItem>> getAllSaleItems() {
        return ResponseEntity.ok(saleItemService.getAllSaleItems());
    }

    /**
     * GET /api/sale-items/{id}
     * Get sale item by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSaleItemById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(saleItemService.getSaleItemById(id));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * GET /api/sale-items/sale/{saleId}
     * Get all items for a specific sale
     */
    @GetMapping("/sale/{saleId}")
    public ResponseEntity<List<SaleItem>> getSaleItemsBySale(@PathVariable Long saleId) {
        return ResponseEntity.ok(saleItemService.getSaleItemsBySaleId(saleId));
    }

    /**
     * GET /api/sale-items/product/{productId}
     * Get all sale items for a specific product
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<SaleItem>> getSaleItemsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(saleItemService.getSaleItemsByProductId(productId));
    }

    /**
     * PUT /api/sale-items/{id}
     * Update sale item (quantity, price, discount)
     *
     * Request Body:
     * {
     *   "quantity": 3,
     *   "unitPrice": 95.0,
     *   "discount": 10.0
     * }
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSaleItem(@PathVariable Long id, @RequestBody SaleItem saleItem) {
        try {
            return ResponseEntity.ok(saleItemService.updateSaleItem(id, saleItem));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * DELETE /api/sale-items/{id}
     * Delete sale item
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSaleItem(@PathVariable Long id) {
        try {
            saleItemService.deleteSaleItem(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Sale item deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * GET /api/sale-items/product/{productId}/sold
     * Get total quantity sold for a product
     */
    @GetMapping("/product/{productId}/sold")
    public ResponseEntity<?> getTotalQuantitySold(@PathVariable Long productId) {
        Integer totalSold = saleItemService.calculateTotalQuantitySold(productId);
        Map<String, Object> response = new HashMap<>();
        response.put("productId", productId);
        response.put("totalQuantitySold", totalSold);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/sale-items/product/{productId}/revenue
     * Get total revenue for a product
     */
    @GetMapping("/product/{productId}/revenue")
    public ResponseEntity<?> getTotalRevenue(@PathVariable Long productId) {
        Double totalRevenue = saleItemService.calculateTotalRevenue(productId);
        Map<String, Object> response = new HashMap<>();
        response.put("productId", productId);
        response.put("totalRevenue", totalRevenue);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/sale-items/top-selling
     * Get top selling products
     * Returns list of [Product, TotalQuantity] pairs
     */
    @GetMapping("/top-selling")
    public ResponseEntity<List<Object[]>> getTopSellingProducts() {
        return ResponseEntity.ok(saleItemService.getTopSellingProducts());
    }

    /**
     * GET /api/sale-items/with-discount
     * Get all sale items that have discounts applied
     */
    @GetMapping("/with-discount")
    public ResponseEntity<List<SaleItem>> getItemsWithDiscount() {
        return ResponseEntity.ok(saleItemService.getItemsWithDiscount());
    }

    /**
     * GET /api/sale-items/total-discounts
     * Get total discounts given across all sales
     */
    @GetMapping("/total-discounts")
    public ResponseEntity<?> getTotalDiscounts() {
        Double totalDiscounts = saleItemService.calculateTotalDiscounts();
        Map<String, Double> response = new HashMap<>();
        response.put("totalDiscounts", totalDiscounts);
        return ResponseEntity.ok(response);
    }
}

/**
 * USAGE EXAMPLES:
 *
 * 1. Get items for a sale:
 * GET http://localhost:5001/api/sale-items/sale/1
 *
 * 2. Get total quantity sold for a product:
 * GET http://localhost:5001/api/sale-items/product/5/sold
 * Response: { "productId": 5, "totalQuantitySold": 127 }
 *
 * 3. Get total revenue for a product:
 * GET http://localhost:5001/api/sale-items/product/5/revenue
 * Response: { "productId": 5, "totalRevenue": 12700.50 }
 *
 * 4. Get top selling products:
 * GET http://localhost:5001/api/sale-items/top-selling
 *
 * 5. Get items with discounts:
 * GET http://localhost:5001/api/sale-items/with-discount
 */
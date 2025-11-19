package com.smartinventory.controller;

import com.smartinventory.model.Stock;
import com.smartinventory.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StockController - Handles stock movement endpoints
 *
 * Endpoints:
 * GET  /api/stock                    - Get all stock movements
 * GET  /api/stock/{id}               - Get stock movement by ID
 * POST /api/stock/add                - Add stock (incoming)
 * POST /api/stock/remove             - Remove stock (outgoing)
 * GET  /api/stock/product/{id}       - Get movements for product
 * GET  /api/stock/recent             - Get recent movements
 * GET  /api/stock/product/{id}/current - Get current stock level
 */
@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "*")
public class StockController {

    private final StockService stockService;

    @Autowired
    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public ResponseEntity<List<Stock>> getAllStockMovements() {
        return ResponseEntity.ok(stockService.getAllStockMovements());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStockById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(stockService.getStockById(id));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * POST /api/stock/add
     * Add stock to a product
     *
     * Request Body:
     * {
     *   "productId": 1,
     *   "quantity": 50,
     *   "reason": "Purchase from supplier",
     *   "reference": "PO-001"
     * }
     */
    @PostMapping("/add")
    public ResponseEntity<?> addStock(@RequestBody StockRequest request) {
        try {
            Stock stock = stockService.addStock(
                    request.getProductId(),
                    request.getQuantity(),
                    request.getReason(),
                    request.getReference()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(stock);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * POST /api/stock/remove
     * Remove stock from a product
     *
     * Request Body:
     * {
     *   "productId": 1,
     *   "quantity": 5,
     *   "reason": "Sold to customer",
     *   "reference": "SALE-001"
     * }
     */
    @PostMapping("/remove")
    public ResponseEntity<?> removeStock(@RequestBody StockRequest request) {
        try {
            Stock stock = stockService.removeStock(
                    request.getProductId(),
                    request.getQuantity(),
                    request.getReason(),
                    request.getReference()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(stock);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Stock>> getStockMovementsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockService.getStockMovementsByProduct(productId));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Stock>> getRecentStockMovements() {
        return ResponseEntity.ok(stockService.getRecentStockMovements());
    }

    @GetMapping("/product/{productId}/current")
    public ResponseEntity<?> getCurrentStock(@PathVariable Long productId) {
        try {
            Integer stock = stockService.calculateCurrentStock(productId);
            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("currentStock", stock);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/additions")
    public ResponseEntity<List<Stock>> getStockAdditions() {
        return ResponseEntity.ok(stockService.getStockAdditions());
    }

    @GetMapping("/removals")
    public ResponseEntity<List<Stock>> getStockRemovals() {
        return ResponseEntity.ok(stockService.getStockRemovals());
    }

    /**
     * Request DTO for stock operations
     */
    public static class StockRequest {
        private Long productId;
        private Integer quantity;
        private String reason;
        private String reference;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
    }
}
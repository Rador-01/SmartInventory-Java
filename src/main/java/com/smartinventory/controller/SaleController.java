package com.smartinventory.controller;

import com.smartinventory.model.Sale;
import com.smartinventory.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SaleController - Handles sales transaction endpoints
 *
 * Endpoints:
 * GET    /api/sales                 - Get all sales
 * GET    /api/sales/{id}            - Get sale by ID
 * GET    /api/sales/reference/{ref} - Get sale by reference
 * POST   /api/sales                 - Create new sale
 * PUT    /api/sales/{id}/status     - Update sale status
 * DELETE /api/sales/{id}            - Delete/cancel sale
 * GET    /api/sales/client/{id}     - Get sales by client
 * GET    /api/sales/status/{status} - Get sales by status
 * GET    /api/sales/pending         - Get pending sales
 * GET    /api/sales/recent          - Get recent sales
 */
@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*")
public class SaleController {

    private final SaleService saleService;

    @Autowired
    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public ResponseEntity<List<Sale>> getAllSales() {
        return ResponseEntity.ok(saleService.getAllSales());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSaleById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(saleService.getSaleById(id));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<?> getSaleByReference(@PathVariable String reference) {
        try {
            return ResponseEntity.ok(saleService.getSaleByReference(reference));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * POST /api/sales
     * Create new sale
     *
     * Request Body:
     * {
     *   "client": { "id": 1 },
     *   "saleReference": "SALE-001",
     *   "status": "PENDING",
     *   "paymentMethod": "CASH",
     *   "notes": "Customer notes",
     *   "items": [
     *     {
     *       "product": { "id": 1 },
     *       "quantity": 2,
     *       "unitPrice": 100.0,
     *       "discount": 0.0
     *     }
     *   ]
     * }
     */
    @PostMapping
    public ResponseEntity<?> createSale(@RequestBody Sale sale) {
        try {
            Sale created = saleService.createSale(sale);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * PUT /api/sales/{id}/status
     * Update sale status (PENDING -> PAID)
     *
     * Request Body:
     * {
     *   "status": "PAID"
     * }
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateSaleStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {
        try {
            String status = request.get("status");
            Sale updated = saleService.updateSaleStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSale(@PathVariable Long id) {
        try {
            saleService.deleteSale(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Sale deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Sale>> getSalesByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(saleService.getSalesByClient(clientId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Sale>> getSalesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(saleService.getSalesByStatus(status));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Sale>> getPendingSales() {
        return ResponseEntity.ok(saleService.getPendingSales());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Sale>> getRecentSales() {
        return ResponseEntity.ok(saleService.getRecentSales());
    }
}
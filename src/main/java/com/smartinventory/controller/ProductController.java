package com.smartinventory.controller;

import com.smartinventory.model.Product;
import com.smartinventory.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProductController - Handles product endpoints
 *
 * Endpoints:
 * GET    /api/products          - Get all products
 * GET    /api/products/{id}     - Get product by ID
 * GET    /api/products/sku/{sku} - Get product by SKU
 * POST   /api/products          - Create new product
 * PUT    /api/products/{id}     - Update product
 * DELETE /api/products/{id}     - Delete product
 * GET    /api/products/search?q={term} - Search products
 * GET    /api/products/low-stock - Get low stock products
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /api/products
     * Get all products
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * GET /api/products/{id}
     * Get product by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id);
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * GET /api/products/sku/{sku}
     * Get product by SKU
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<?> getProductBySku(@PathVariable String sku) {
        try {
            Product product = productService.getProductBySku(sku);
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * POST /api/products
     * Create new product
     *
     * Request Body:
     * {
     *   "name": "Laptop",
     *   "description": "Gaming laptop",
     *   "brand": "Dell",
     *   "sku": "SKU001",
     *   "costPrice": 800.0,
     *   "sellingPrice": 1200.0,
     *   "category": { "id": 1 },
     *   "supplier": { "id": 1 }
     * }
     */
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        try {
            Product created = productService.createProduct(product);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * PUT /api/products/{id}
     * Update product
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        try {
            Product updated = productService.updateProduct(id, product);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * DELETE /api/products/{id}
     * Delete product
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Product deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * GET /api/products/search?q={term}
     * Search products
     */
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String q) {
        List<Product> products = productService.searchProducts(q);
        return ResponseEntity.ok(products);
    }

    /**
     * GET /api/products/category/{categoryId}
     * Get products by category
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable Long categoryId) {
        List<Product> products = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(products);
    }

    /**
     * GET /api/products/supplier/{supplierId}
     * Get products by supplier
     */
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<Product>> getProductsBySupplier(@PathVariable Long supplierId) {
        List<Product> products = productService.getProductsBySupplier(supplierId);
        return ResponseEntity.ok(products);
    }

    /**
     * GET /api/products/low-stock?threshold=10
     * Get low stock products
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold
    ) {
        List<Product> products = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(products);
    }

    /**
     * GET /api/products/{id}/stock
     * Get current stock for a product
     */
    @GetMapping("/{id}/stock")
    public ResponseEntity<?> getCurrentStock(@PathVariable Long id) {
        try {
            int stock = productService.getCurrentStock(id);
            Map<String, Object> response = new HashMap<>();
            response.put("productId", id);
            response.put("currentStock", stock);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}

/**
 * TESTING EXAMPLES:
 *
 * 1. Get all products:
 * GET http://localhost:5001/api/products
 *
 * 2. Create product:
 * POST http://localhost:5001/api/products
 * Body: {
 *   "name": "Laptop",
 *   "brand": "Dell",
 *   "sku": "LAP001",
 *   "costPrice": 800,
 *   "sellingPrice": 1200
 * }
 *
 * 3. Search products:
 * GET http://localhost:5001/api/products/search?q=laptop
 *
 * 4. Get low stock products:
 * GET http://localhost:5001/api/products/low-stock?threshold=5
 */
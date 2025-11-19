package com.smartinventory.service;

import com.smartinventory.model.Product;
import com.smartinventory.model.Category;
import com.smartinventory.model.Supplier;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.CategoryRepository;
import com.smartinventory.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ProductService - Product management operations
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    @Autowired
    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          SupplierRepository supplierRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
    }

    // ============================================
    // CRUD OPERATIONS
    // ============================================

    /**
     * Get all products
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Get product by ID
     */
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    /**
     * Get product by SKU
     */
    public Product getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Product not found with SKU: " + sku));
    }

    /**
     * Create new product
     */
    public Product createProduct(Product product) {
        // Check if SKU already exists
        if (productRepository.existsBySku(product.getSku())) {
            throw new RuntimeException("Product with SKU " + product.getSku() + " already exists");
        }

        // Validate category if provided
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            Category category = categoryRepository.findById(product.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }

        // Validate supplier if provided
        if (product.getSupplier() != null && product.getSupplier().getId() != null) {
            Supplier supplier = supplierRepository.findById(product.getSupplier().getId())
                    .orElseThrow(() -> new RuntimeException("Supplier not found"));
            product.setSupplier(supplier);
        }

        return productRepository.save(product);
    }

    /**
     * Update product
     */
    public Product updateProduct(Long id, Product productDetails) {
        Product product = getProductById(id);

        // Update fields
        if (productDetails.getName() != null) {
            product.setName(productDetails.getName());
        }

        if (productDetails.getDescription() != null) {
            product.setDescription(productDetails.getDescription());
        }

        if (productDetails.getBrand() != null) {
            product.setBrand(productDetails.getBrand());
        }

        if (productDetails.getSku() != null && !productDetails.getSku().equals(product.getSku())) {
            if (productRepository.existsBySku(productDetails.getSku())) {
                throw new RuntimeException("Product with SKU " + productDetails.getSku() + " already exists");
            }
            product.setSku(productDetails.getSku());
        }

        if (productDetails.getCostPrice() != null) {
            product.setCostPrice(productDetails.getCostPrice());
        }

        if (productDetails.getSellingPrice() != null) {
            product.setSellingPrice(productDetails.getSellingPrice());
        }

        // Update category if provided
        if (productDetails.getCategory() != null && productDetails.getCategory().getId() != null) {
            Category category = categoryRepository.findById(productDetails.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }

        // Update supplier if provided
        if (productDetails.getSupplier() != null && productDetails.getSupplier().getId() != null) {
            Supplier supplier = supplierRepository.findById(productDetails.getSupplier().getId())
                    .orElseThrow(() -> new RuntimeException("Supplier not found"));
            product.setSupplier(supplier);
        }

        return productRepository.save(product);
    }

    /**
     * Delete product
     */
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }

    // ============================================
    // QUERY OPERATIONS
    // ============================================

    /**
     * Search products by name
     */
    public List<Product> searchProducts(String searchTerm) {
        return productRepository.searchProducts(searchTerm);
    }

    /**
     * Get products by category
     */
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    /**
     * Get products by supplier
     */
    public List<Product> getProductsBySupplier(Long supplierId) {
        return productRepository.findBySupplierId(supplierId);
    }

    /**
     * Get products by brand
     */
    public List<Product> getProductsByBrand(String brand) {
        return productRepository.findByBrand(brand);
    }

    /**
     * Get products by price range
     */
    public List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice) {
        return productRepository.findBySellingPriceBetween(minPrice, maxPrice);
    }

    /**
     * Get low stock products
     */
    public List<Product> getLowStockProducts(int threshold) {
        return productRepository.findLowStockProducts(threshold);
    }

    /**
     * Get products with no stock
     */
    public List<Product> getProductsWithNoStock() {
        return productRepository.findProductsWithNoStock();
    }

    /**
     * Count total products
     */
    public long countProducts() {
        return productRepository.count();
    }

    /**
     * Check if SKU exists
     */
    public boolean skuExists(String sku) {
        return productRepository.existsBySku(sku);
    }

    /**
     * Get current stock for a product
     */
    public int getCurrentStock(Long productId) {
        Product product = getProductById(productId);
        return product.getCurrentStock();
    }

    /**
     * Calculate total inventory value
     */
    public double calculateTotalInventoryValue() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .mapToDouble(p -> {
                    int stock = p.getCurrentStock();
                    Double costPrice = p.getCostPrice();
                    return stock * (costPrice != null ? costPrice : 0);
                })
                .sum();
    }
}
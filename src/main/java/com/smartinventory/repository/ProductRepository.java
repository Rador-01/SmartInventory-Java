package com.smartinventory.repository;

import com.smartinventory.model.Product;
import com.smartinventory.model.Category;
import com.smartinventory.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ProductRepository - Database access for Product entity
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find product by SKU (unique product code)
     * Query: SELECT * FROM product WHERE sku = ?
     */
    Optional<Product> findBySku(String sku);

    /**
     * Check if SKU exists
     * Query: SELECT COUNT(*) > 0 FROM product WHERE sku = ?
     */
    boolean existsBySku(String sku);

    /**
     * Find products by name containing (search)
     * Query: SELECT * FROM product WHERE name LIKE %?%
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Find products by brand
     * Query: SELECT * FROM product WHERE brand = ?
     */
    List<Product> findByBrand(String brand);

    /**
     * Find products by category
     * Query: SELECT * FROM product WHERE category_id = ?
     */
    List<Product> findByCategory(Category category);

    /**
     * Find products by category ID
     * Query: SELECT * FROM product WHERE category_id = ?
     */
    List<Product> findByCategoryId(Long categoryId);

    /**
     * Find products by supplier
     * Query: SELECT * FROM product WHERE supplier_id = ?
     */
    List<Product> findBySupplier(Supplier supplier);

    /**
     * Find products by supplier ID
     * Query: SELECT * FROM product WHERE supplier_id = ?
     */
    List<Product> findBySupplierId(Long supplierId);

    /**
     * Find products by price range
     * Query: SELECT * FROM product WHERE selling_price BETWEEN ? AND ?
     */
    List<Product> findBySellingPriceBetween(Double minPrice, Double maxPrice);

    /**
     * Find products with selling price greater than
     * Query: SELECT * FROM product WHERE selling_price > ?
     */
    List<Product> findBySellingPriceGreaterThan(Double price);

    /**
     * Find products with selling price less than
     * Query: SELECT * FROM product WHERE selling_price < ?
     */
    List<Product> findBySellingPriceLessThan(Double price);

    /**
     * Custom query to find low stock products
     * This uses a native SQL query because calculating stock requires joining Stock table
     */
    @Query("SELECT p FROM Product p WHERE " +
            "(SELECT SUM(s.quantity) FROM Stock s WHERE s.product = p) < :threshold")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    /**
     * Custom query to find products with no stock movements
     */
    @Query("SELECT p FROM Product p WHERE p.stocks IS EMPTY")
    List<Product> findProductsWithNoStock();

    /**
     * Search products by multiple criteria
     */
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Product> searchProducts(@Param("searchTerm") String searchTerm);
}
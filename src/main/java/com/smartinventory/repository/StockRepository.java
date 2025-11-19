package com.smartinventory.repository;

import com.smartinventory.model.Stock;
import com.smartinventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * StockRepository - Database access for Stock entity
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    /**
     * Find all stock movements for a product
     * Query: SELECT * FROM stock WHERE product_id = ?
     */
    List<Stock> findByProduct(Product product);

    /**
     * Find all stock movements for a product ID
     * Query: SELECT * FROM stock WHERE product_id = ?
     */
    List<Stock> findByProductId(Long productId);

    /**
     * Find stock movements by type (IN or OUT)
     * Query: SELECT * FROM stock WHERE movement_type = ?
     */
    List<Stock> findByMovementType(String movementType);

    /**
     * Find stock movements by reference (e.g., "PURCHASE-001")
     * Query: SELECT * FROM stock WHERE reference = ?
     */
    List<Stock> findByReference(String reference);

    /**
     * Find stock movements by date range
     * Query: SELECT * FROM stock WHERE created_at BETWEEN ? AND ?
     */
    List<Stock> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find stock movements for a product by date range
     */
    List<Stock> findByProductIdAndCreatedAtBetween(
            Long productId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Calculate total stock for a product
     * Returns the sum of all quantities (positive and negative)
     */
    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.product.id = :productId")
    Integer calculateTotalStock(@Param("productId") Long productId);

    /**
     * Get recent stock movements (last N records)
     */
    List<Stock> findTop10ByOrderByCreatedAtDesc();

    /**
     * Get recent stock movements for a specific product
     */
    List<Stock> findTop10ByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Find stock additions only (IN movements)
     */
    @Query("SELECT s FROM Stock s WHERE s.movementType = 'IN' OR s.quantity > 0")
    List<Stock> findStockAdditions();

    /**
     * Find stock removals only (OUT movements)
     */
    @Query("SELECT s FROM Stock s WHERE s.movementType = 'OUT' OR s.quantity < 0")
    List<Stock> findStockRemovals();
}
package com.smartinventory.repository;

import com.smartinventory.model.SaleItem;
import com.smartinventory.model.Sale;
import com.smartinventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SaleItemRepository - Database access for SaleItem entity
 */
@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    /**
     * Find all items in a sale
     * Query: SELECT * FROM sale_item WHERE sale_id = ?
     */
    List<SaleItem> findBySale(Sale sale);

    /**
     * Find all items in a sale by sale ID
     * Query: SELECT * FROM sale_item WHERE sale_id = ?
     */
    List<SaleItem> findBySaleId(Long saleId);

    /**
     * Find all sale items for a product
     * Query: SELECT * FROM sale_item WHERE product_id = ?
     */
    List<SaleItem> findByProduct(Product product);

    /**
     * Find all sale items for a product ID
     * Query: SELECT * FROM sale_item WHERE product_id = ?
     */
    List<SaleItem> findByProductId(Long productId);

    /**
     * Calculate total quantity sold for a product
     */
    @Query("SELECT COALESCE(SUM(si.quantity), 0) FROM SaleItem si " +
            "WHERE si.product.id = :productId")
    Integer calculateTotalQuantitySold(@Param("productId") Long productId);

    /**
     * Calculate total revenue for a product
     */
    @Query("SELECT COALESCE(SUM(si.subtotal), 0) FROM SaleItem si " +
            "WHERE si.product.id = :productId")
    Double calculateTotalRevenueByProduct(@Param("productId") Long productId);

    /**
     * Find top selling products
     */
    @Query("SELECT si.product, SUM(si.quantity) as totalQty FROM SaleItem si " +
            "GROUP BY si.product " +
            "ORDER BY totalQty DESC")
    List<Object[]> findTopSellingProducts();

    /**
     * Find items with discount
     */
    @Query("SELECT si FROM SaleItem si WHERE si.discount > 0")
    List<SaleItem> findItemsWithDiscount();

    /**
     * Calculate total discount given
     */
    @Query("SELECT COALESCE(SUM(si.discount), 0) FROM SaleItem si")
    Double calculateTotalDiscounts();
}
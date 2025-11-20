package com.smartinventory.repository;

import com.smartinventory.model.Sale;
import com.smartinventory.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SaleRepository - Database access for Sale entity
 */
@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    /**
     * Find sale by reference number
     * Query: SELECT * FROM sale WHERE sale_reference = ?
     */
    Optional<Sale> findBySaleReference(String saleReference);

    /**
     * Check if sale reference exists
     * Query: SELECT COUNT(*) > 0 FROM sale WHERE sale_reference = ?
     */
    boolean existsBySaleReference(String saleReference);

    /**
     * Find sales by client
     * Query: SELECT * FROM sale WHERE client_id = ?
     */
    List<Sale> findByClient(Client client);

    /**
     * Find sales by client ID
     * Query: SELECT * FROM sale WHERE client_id = ?
     */
    List<Sale> findByClientId(Long clientId);

    /**
     * Find sales by status (PENDING, PAID, CANCELLED)
     * Query: SELECT * FROM sale WHERE status = ?
     */
    List<Sale> findByStatus(String status);

    /**
     * Find sales by payment method
     * Query: SELECT * FROM sale WHERE payment_method = ?
     */
    List<Sale> findByPaymentMethod(String paymentMethod);

    /**
     * Find sales by date range
     * Query: SELECT * FROM sale WHERE sale_date BETWEEN ? AND ?
     */
    List<Sale> findBySaleDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find sales created between dates
     * Query: SELECT * FROM sale WHERE created_at BETWEEN ? AND ?
     */
    List<Sale> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find recent sales (last 10)
     * Query: SELECT * FROM sale ORDER BY sale_date DESC LIMIT 10
     */
    List<Sale> findTop10ByOrderBySaleDateDesc();

    /**
     * Find pending sales
     */
    @Query("SELECT s FROM Sale s WHERE s.status = 'PENDING' ORDER BY s.saleDate DESC")
    List<Sale> findPendingSales();

    /**
     * Find paid sales
     */
    @Query("SELECT s FROM Sale s WHERE s.status = 'PAID' ORDER BY s.saleDate DESC")
    List<Sale> findPaidSales();

    /**
     * Calculate total sales for a period
     */
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s " +
            "WHERE s.status = 'PAID' AND s.saleDate BETWEEN :startDate AND :endDate")
    Double calculateTotalSales(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Calculate total sales for a client
     */
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s " +
            "WHERE s.client.id = :clientId AND s.status = 'PAID'")
    Double calculateTotalSalesByClient(@Param("clientId") Long clientId);

    /**
     * Count sales by status
     */
    Long countByStatus(String status);

    /**
     * Find today's sales
     */
    @Query("SELECT s FROM Sale s WHERE " +
            "FUNCTION('DATE', s.saleDate) = FUNCTION('DATE', CURRENT_TIMESTAMP)")
    List<Sale> findTodaySales();

    /**
     * Find sales by month and year
     */
    @Query("SELECT s FROM Sale s WHERE " +
            "FUNCTION('strftime', '%Y', s.saleDate) = CAST(:year AS string) AND " +
            "FUNCTION('strftime', '%m', s.saleDate) = CASE WHEN :month < 10 THEN CONCAT('0', CAST(:month AS string)) ELSE CAST(:month AS string) END")
    List<Sale> findSalesByMonthAndYear(
            @Param("year") int year,
            @Param("month") int month
    );
}
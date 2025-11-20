package com.smartinventory.controller;

import com.smartinventory.dto.report.*;
import com.smartinventory.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReportController - Handles report and analytics endpoints
 *
 * Endpoints:
 * GET /api/reports/summary              - Get summary metrics
 * GET /api/reports/sales-trend          - Get sales trend data
 * GET /api/reports/product-performance  - Get product performance
 * GET /api/reports/category-performance - Get category performance
 * GET /api/reports/supplier-performance - Get supplier performance
 * GET /api/reports/stock-status         - Get stock status
 * GET /api/reports/inventory-stats      - Get inventory statistics
 * GET /api/reports/recommendations      - Get smart recommendations
 * GET /api/reports/full                 - Get all report data at once
 */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * GET /api/reports/summary
     * Get summary metrics for a date range
     *
     * @param startDate Optional start date (defaults to 30 days ago)
     * @param endDate Optional end date (defaults to now)
     * @return Summary metrics DTO
     */
    @GetMapping("/summary")
    public ResponseEntity<SummaryMetricsDTO> getSummaryMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDateTime start = (startDate != null)
                ? startDate.atStartOfDay()
                : LocalDateTime.now().minusDays(30);

        LocalDateTime end = (endDate != null)
                ? endDate.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        SummaryMetricsDTO metrics = reportService.getSummaryMetrics(start, end);
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/reports/sales-trend
     * Get sales trend data for the last N days
     *
     * @param days Number of days (defaults to 30)
     * @return Sales trend DTO
     */
    @GetMapping("/sales-trend")
    public ResponseEntity<SalesTrendDTO> getSalesTrend(
            @RequestParam(defaultValue = "30") int days
    ) {
        SalesTrendDTO trend = reportService.getSalesTrend(days);
        return ResponseEntity.ok(trend);
    }

    /**
     * GET /api/reports/product-performance
     * Get product performance metrics
     *
     * @param startDate Optional start date
     * @param endDate Optional end date
     * @return List of product performance DTOs
     */
    @GetMapping("/product-performance")
    public ResponseEntity<List<ProductPerformanceDTO>> getProductPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDateTime start = (startDate != null)
                ? startDate.atStartOfDay()
                : LocalDateTime.now().minusDays(30);

        LocalDateTime end = (endDate != null)
                ? endDate.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        List<ProductPerformanceDTO> performance = reportService.getProductPerformance(start, end);
        return ResponseEntity.ok(performance);
    }

    /**
     * GET /api/reports/category-performance
     * Get category performance metrics
     *
     * @param startDate Optional start date
     * @param endDate Optional end date
     * @return List of category performance DTOs
     */
    @GetMapping("/category-performance")
    public ResponseEntity<List<CategoryPerformanceDTO>> getCategoryPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDateTime start = (startDate != null)
                ? startDate.atStartOfDay()
                : LocalDateTime.now().minusDays(30);

        LocalDateTime end = (endDate != null)
                ? endDate.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        List<CategoryPerformanceDTO> performance = reportService.getCategoryPerformance(start, end);
        return ResponseEntity.ok(performance);
    }

    /**
     * GET /api/reports/supplier-performance
     * Get supplier performance metrics
     *
     * @param startDate Optional start date
     * @param endDate Optional end date
     * @return List of supplier performance DTOs
     */
    @GetMapping("/supplier-performance")
    public ResponseEntity<List<SupplierPerformanceDTO>> getSupplierPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDateTime start = (startDate != null)
                ? startDate.atStartOfDay()
                : LocalDateTime.now().minusDays(30);

        LocalDateTime end = (endDate != null)
                ? endDate.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        List<SupplierPerformanceDTO> performance = reportService.getSupplierPerformance(start, end);
        return ResponseEntity.ok(performance);
    }

    /**
     * GET /api/reports/stock-status
     * Get stock status distribution
     *
     * @return Stock status DTO
     */
    @GetMapping("/stock-status")
    public ResponseEntity<StockStatusDTO> getStockStatus() {
        StockStatusDTO status = reportService.getStockStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * GET /api/reports/inventory-stats
     * Get inventory statistics
     *
     * @param startDate Optional start date (for items sold calculation)
     * @param endDate Optional end date
     * @return Inventory stats DTO
     */
    @GetMapping("/inventory-stats")
    public ResponseEntity<InventoryStatsDTO> getInventoryStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDateTime start = (startDate != null)
                ? startDate.atStartOfDay()
                : LocalDateTime.now().minusDays(30);

        LocalDateTime end = (endDate != null)
                ? endDate.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        InventoryStatsDTO stats = reportService.getInventoryStats(start, end);
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/reports/recommendations
     * Get smart recommendations based on data
     *
     * @return List of recommendation DTOs
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendationDTO>> getRecommendations() {
        List<RecommendationDTO> recommendations = reportService.getRecommendations();
        return ResponseEntity.ok(recommendations);
    }

    /**
     * GET /api/reports/full
     * Get all report data at once (optimized single request)
     *
     * @param startDate Optional start date
     * @param endDate Optional end date
     * @param days Number of days for trend (defaults to 30)
     * @return Map containing all report data
     */
    @GetMapping("/full")
    public ResponseEntity<Map<String, Object>> getFullReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "30") int days
    ) {
        LocalDateTime start = (startDate != null)
                ? startDate.atStartOfDay()
                : LocalDateTime.now().minusDays(30);

        LocalDateTime end = (endDate != null)
                ? endDate.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        Map<String, Object> fullReport = new HashMap<>();

        // Get all report data
        fullReport.put("summary", reportService.getSummaryMetrics(start, end));
        fullReport.put("salesTrend", reportService.getSalesTrend(days));
        fullReport.put("productPerformance", reportService.getProductPerformance(start, end));
        fullReport.put("categoryPerformance", reportService.getCategoryPerformance(start, end));
        fullReport.put("supplierPerformance", reportService.getSupplierPerformance(start, end));
        fullReport.put("stockStatus", reportService.getStockStatus());
        fullReport.put("inventoryStats", reportService.getInventoryStats(start, end));
        fullReport.put("recommendations", reportService.getRecommendations());

        return ResponseEntity.ok(fullReport);
    }
}

/**
 * TESTING EXAMPLES:
 *
 * 1. Get summary metrics:
 * GET http://localhost:5001/api/reports/summary
 *
 * 2. Get summary with date range:
 * GET http://localhost:5001/api/reports/summary?startDate=2025-01-01&endDate=2025-11-20
 *
 * 3. Get sales trend for last 7 days:
 * GET http://localhost:5001/api/reports/sales-trend?days=7
 *
 * 4. Get product performance:
 * GET http://localhost:5001/api/reports/product-performance
 *
 * 5. Get full report (all data at once):
 * GET http://localhost:5001/api/reports/full
 *
 * 6. Get full report with custom range:
 * GET http://localhost:5001/api/reports/full?startDate=2025-01-01&endDate=2025-11-20&days=60
 */

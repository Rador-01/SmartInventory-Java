package com.smartinventory.controller;

import com.smartinventory.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * ReportController - Handles report and analytics endpoints
 *
 * SIMPLIFIED VERSION - Only 3 essential endpoints:
 * GET /api/reports/summary           - Get summary metrics (revenue, profit, sales count)
 * GET /api/reports/product-performance - Get product performance (what's selling)
 * GET /api/reports/stock-status      - Get stock status (in-stock, low-stock, out-of-stock)
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
     * @return Summary metrics: totalRevenue, totalCost, totalProfit, salesCount, productCount
     *
     * Example: GET /api/reports/summary
     * Example: GET /api/reports/summary?startDate=2025-01-01&endDate=2025-11-20
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummaryMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // Set default date range if not provided
        LocalDateTime start = (startDate != null)
                ? startDate.atStartOfDay()
                : LocalDateTime.now().minusDays(30);

        LocalDateTime end = (endDate != null)
                ? endDate.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        Map<String, Object> metrics = reportService.getSummaryMetrics(start, end);
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/reports/product-performance
     * Get product performance metrics
     *
     * @param startDate Optional start date (defaults to 30 days ago)
     * @param endDate Optional end date (defaults to now)
     * @return List of products with: id, name, quantity, revenue, cost, profit
     *
     * Example: GET /api/reports/product-performance
     * Example: GET /api/reports/product-performance?startDate=2025-01-01&endDate=2025-11-20
     */
    @GetMapping("/product-performance")
    public ResponseEntity<List<Map<String, Object>>> getProductPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // Set default date range if not provided
        LocalDateTime start = (startDate != null)
                ? startDate.atStartOfDay()
                : LocalDateTime.now().minusDays(30);

        LocalDateTime end = (endDate != null)
                ? endDate.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        List<Map<String, Object>> performance = reportService.getProductPerformance(start, end);
        return ResponseEntity.ok(performance);
    }

    /**
     * GET /api/reports/stock-status
     * Get stock status distribution
     *
     * @return Stock counts: inStock (>=10), lowStock (1-9), outOfStock (0)
     *
     * Example: GET /api/reports/stock-status
     */
    @GetMapping("/stock-status")
    public ResponseEntity<Map<String, Integer>> getStockStatus() {
        Map<String, Integer> status = reportService.getStockStatus();
        return ResponseEntity.ok(status);
    }
}

/**
 * WHAT WAS SIMPLIFIED:
 *
 * REMOVED (Too complex for learning):
 * - /sales-trend endpoint
 * - /category-performance endpoint
 * - /supplier-performance endpoint
 * - /inventory-stats endpoint
 * - /recommendations endpoint
 * - /full report endpoint
 * - All DTO classes (now using Map<String, Object>)
 *
 * KEPT (Essential for understanding):
 * - /summary - Overall business metrics
 * - /product-performance - What's selling
 * - /stock-status - Inventory levels
 *
 * WHY THIS IS BETTER FOR LEARNING:
 * - Only 3 endpoints instead of 8
 * - Returns simple Maps instead of complex DTOs
 * - Easier to understand what data is returned
 * - Still demonstrates REST API patterns
 * - Still shows how to handle query parameters
 * - Still shows date range handling
 *
 * TESTING EXAMPLES:
 *
 * 1. Get summary metrics:
 * GET http://localhost:5001/api/reports/summary
 *
 * Response:
 * {
 *   "totalRevenue": 5000.0,
 *   "totalCost": 3000.0,
 *   "totalProfit": 2000.0,
 *   "salesCount": 10,
 *   "productCount": 50
 * }
 *
 * 2. Get product performance:
 * GET http://localhost:5001/api/reports/product-performance
 *
 * Response:
 * [
 *   {
 *     "id": 1,
 *     "name": "Laptop",
 *     "quantity": 5,
 *     "revenue": 6000.0,
 *     "cost": 4000.0,
 *     "profit": 2000.0
 *   },
 *   ...
 * ]
 *
 * 3. Get stock status:
 * GET http://localhost:5001/api/reports/stock-status
 *
 * Response:
 * {
 *   "inStock": 30,
 *   "lowStock": 5,
 *   "outOfStock": 2
 * }
 */

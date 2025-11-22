package com.smartinventory.service;

import com.smartinventory.model.*;
import com.smartinventory.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ReportService - Handles report generation and analytics
 *
 * SIMPLIFIED VERSION - Only 3 essential reports for learning:
 * 1. Summary Metrics - Total revenue, profit, sales count
 * 2. Product Performance - Revenue and quantity sold per product
 * 3. Stock Status - Count of in-stock, low-stock, and out-of-stock items
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    @Autowired
    public ReportService(SaleRepository saleRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
    }

    /**
     * Get summary metrics for a date range
     *
     * Returns: total revenue, total profit, number of sales
     */
    public Map<String, Object> getSummaryMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        // Get all paid sales in date range
        List<Sale> sales = getSalesInRange(startDate, endDate);
        List<Sale> paidSales = new ArrayList<>();

        for (Sale sale : sales) {
            if ("PAID".equalsIgnoreCase(sale.getStatus())) {
                paidSales.add(sale);
            }
        }

        // Calculate total revenue
        double totalRevenue = 0;
        for (Sale sale : paidSales) {
            totalRevenue += sale.getTotalAmount();
        }

        // Calculate total cost
        double totalCost = 0;
        for (Sale sale : paidSales) {
            for (SaleItem item : sale.getItems()) {
                Product product = item.getProduct();
                if (product.getCostPrice() != null) {
                    totalCost += product.getCostPrice() * item.getQuantity();
                }
            }
        }

        // Calculate profit
        double totalProfit = totalRevenue - totalCost;

        // Create response
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRevenue", totalRevenue);
        summary.put("totalCost", totalCost);
        summary.put("totalProfit", totalProfit);
        summary.put("salesCount", paidSales.size());
        summary.put("productCount", productRepository.count());

        return summary;
    }

    /**
     * Get product performance metrics
     *
     * Returns: list of products with their revenue and quantity sold
     */
    public List<Map<String, Object>> getProductPerformance(LocalDateTime startDate, LocalDateTime endDate) {
        // Get all paid sales in date range
        List<Sale> sales = getSalesInRange(startDate, endDate);
        List<Sale> paidSales = new ArrayList<>();

        for (Sale sale : sales) {
            if ("PAID".equalsIgnoreCase(sale.getStatus())) {
                paidSales.add(sale);
            }
        }

        // Track product performance
        Map<Long, Map<String, Object>> performanceMap = new HashMap<>();

        // Initialize all products
        List<Product> allProducts = productRepository.findAll();
        for (Product product : allProducts) {
            Map<String, Object> performance = new HashMap<>();
            performance.put("id", product.getId());
            performance.put("name", product.getName());
            performance.put("quantity", 0);
            performance.put("revenue", 0.0);
            performance.put("cost", 0.0);
            performanceMap.put(product.getId(), performance);
        }

        // Calculate from sales
        for (Sale sale : paidSales) {
            for (SaleItem item : sale.getItems()) {
                Long productId = item.getProduct().getId();
                Map<String, Object> performance = performanceMap.get(productId);

                if (performance != null) {
                    // Update quantity
                    int currentQty = (int) performance.get("quantity");
                    performance.put("quantity", currentQty + item.getQuantity());

                    // Update revenue
                    double currentRevenue = (double) performance.get("revenue");
                    performance.put("revenue", currentRevenue + item.getSubtotal());

                    // Update cost
                    Product product = item.getProduct();
                    if (product.getCostPrice() != null) {
                        double currentCost = (double) performance.get("cost");
                        double itemCost = product.getCostPrice() * item.getQuantity();
                        performance.put("cost", currentCost + itemCost);
                    }
                }
            }
        }

        // Calculate profit for each product
        for (Map<String, Object> performance : performanceMap.values()) {
            double revenue = (double) performance.get("revenue");
            double cost = (double) performance.get("cost");
            performance.put("profit", revenue - cost);
        }

        return new ArrayList<>(performanceMap.values());
    }

    /**
     * Get stock status distribution
     *
     * Returns: count of products in-stock, low-stock, and out-of-stock
     */
    public Map<String, Integer> getStockStatus() {
        List<Product> allProducts = productRepository.findAll();

        int inStock = 0;
        int lowStock = 0;
        int outOfStock = 0;

        for (Product product : allProducts) {
            int stock = product.getCurrentStock();
            if (stock >= 10) {
                inStock++;
            } else if (stock > 0) {
                lowStock++;
            } else {
                outOfStock++;
            }
        }

        Map<String, Integer> status = new HashMap<>();
        status.put("inStock", inStock);
        status.put("lowStock", lowStock);
        status.put("outOfStock", outOfStock);

        return status;
    }

    /**
     * Helper method to get sales in a date range
     */
    private List<Sale> getSalesInRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Sale> allSales = saleRepository.findAll();
        List<Sale> salesInRange = new ArrayList<>();

        for (Sale sale : allSales) {
            LocalDateTime saleDate = sale.getSaleDate();
            // Check if sale date is within range
            if (!saleDate.isBefore(startDate) && !saleDate.isAfter(endDate)) {
                salesInRange.add(sale);
            }
        }

        return salesInRange;
    }
}

/**
 * WHAT WAS SIMPLIFIED:
 *
 * REMOVED (Too complex for learning):
 * - SalesTrendDTO and sales trend calculation
 * - CategoryPerformanceDTO and category analytics
 * - SupplierPerformanceDTO and supplier analytics
 * - InventoryStatsDTO and inventory statistics
 * - RecommendationDTO and smart recommendations
 * - Complex BigDecimal calculations and ROI formulas
 * - Java Streams (replaced with simple loops for clarity)
 *
 * KEPT (Essential for understanding):
 * - Summary metrics (revenue, profit, sales count)
 * - Product performance (what's selling)
 * - Stock status (inventory levels)
 *
 * WHY THIS IS BETTER FOR LEARNING:
 * - Uses simple loops instead of Streams API
 * - Uses HashMap instead of custom DTOs
 * - Uses double instead of BigDecimal
 * - Easier to read and understand
 * - Still demonstrates MVC pattern
 * - Still shows how to aggregate data from database
 */

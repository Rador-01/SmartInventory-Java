package com.smartinventory.service;

import com.smartinventory.dto.report.*;
import com.smartinventory.model.*;
import com.smartinventory.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportService - Handles all report generation and analytics
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    @Autowired
    public ReportService(SaleRepository saleRepository,
                        ProductRepository productRepository,
                        CategoryRepository categoryRepository,
                        SupplierRepository supplierRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
    }

    /**
     * Get summary metrics for a date range
     */
    public SummaryMetricsDTO getSummaryMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        List<Sale> sales = getSalesInRange(startDate, endDate);
        List<Sale> paidSales = sales.stream()
                .filter(s -> "PAID".equalsIgnoreCase(s.getStatus()))
                .collect(Collectors.toList());

        // Calculate total revenue
        BigDecimal totalRevenue = paidSales.stream()
                .map(s -> BigDecimal.valueOf(s.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total cost
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Sale sale : paidSales) {
            for (SaleItem item : sale.getItems()) {
                Product product = item.getProduct();
                if (product.getCostPrice() != null) {
                    BigDecimal itemCost = BigDecimal.valueOf(product.getCostPrice())
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                    totalCost = totalCost.add(itemCost);
                }
            }
        }

        // Calculate profit
        BigDecimal totalProfit = totalRevenue.subtract(totalCost);

        // Calculate profit margin percentage
        BigDecimal profitMarginPercent = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            profitMarginPercent = totalProfit
                    .divide(totalRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Calculate ROI percentage
        BigDecimal roiPercent = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            roiPercent = totalProfit
                    .divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Calculate turnover rate (simple: sales / products)
        List<Product> allProducts = productRepository.findAll();
        BigDecimal turnoverRate = BigDecimal.ZERO;
        if (!allProducts.isEmpty()) {
            turnoverRate = BigDecimal.valueOf(paidSales.size())
                    .divide(BigDecimal.valueOf(allProducts.size()), 4, RoundingMode.HALF_UP);
        }

        return new SummaryMetricsDTO(
                totalRevenue,
                totalProfit,
                profitMarginPercent,
                roiPercent,
                turnoverRate,
                paidSales.size(),
                allProducts.size()
        );
    }

    /**
     * Get sales trend data for the last N days
     */
    public SalesTrendDTO getSalesTrend(int days) {
        List<LocalDate> labels = new ArrayList<>();
        List<BigDecimal> revenue = new ArrayList<>();
        List<BigDecimal> profit = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            labels.add(date);

            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

            List<Sale> daySales = getSalesInRange(startOfDay, endOfDay).stream()
                    .filter(s -> "PAID".equalsIgnoreCase(s.getStatus()))
                    .collect(Collectors.toList());

            // Calculate day revenue
            BigDecimal dayRevenue = daySales.stream()
                    .map(s -> BigDecimal.valueOf(s.getTotalAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            revenue.add(dayRevenue);

            // Calculate day cost
            BigDecimal dayCost = BigDecimal.ZERO;
            for (Sale sale : daySales) {
                for (SaleItem item : sale.getItems()) {
                    Product product = item.getProduct();
                    if (product.getCostPrice() != null) {
                        BigDecimal itemCost = BigDecimal.valueOf(product.getCostPrice())
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                        dayCost = dayCost.add(itemCost);
                    }
                }
            }

            // Calculate day profit
            BigDecimal dayProfit = dayRevenue.subtract(dayCost);
            profit.add(dayProfit);
        }

        return new SalesTrendDTO(labels, revenue, profit);
    }

    /**
     * Get product performance metrics
     */
    public List<ProductPerformanceDTO> getProductPerformance(LocalDateTime startDate, LocalDateTime endDate) {
        List<Sale> paidSales = getSalesInRange(startDate, endDate).stream()
                .filter(s -> "PAID".equalsIgnoreCase(s.getStatus()))
                .collect(Collectors.toList());

        Map<Long, ProductPerformanceDTO> performanceMap = new HashMap<>();

        // Initialize all products
        List<Product> allProducts = productRepository.findAll();
        for (Product product : allProducts) {
            performanceMap.put(product.getId(), new ProductPerformanceDTO(
                    product.getId(),
                    product.getName(),
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            ));
        }

        // Calculate from sales
        for (Sale sale : paidSales) {
            for (SaleItem item : sale.getItems()) {
                Long productId = item.getProduct().getId();
                ProductPerformanceDTO dto = performanceMap.get(productId);

                if (dto != null) {
                    dto.setQuantity(dto.getQuantity() + item.getQuantity());
                    dto.setRevenue(dto.getRevenue().add(BigDecimal.valueOf(item.getSubtotal())));

                    Product product = item.getProduct();
                    if (product.getCostPrice() != null) {
                        BigDecimal itemCost = BigDecimal.valueOf(product.getCostPrice())
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                        dto.setCost(dto.getCost().add(itemCost));
                    }
                }
            }
        }

        // Calculate profit, margin, and ROI
        for (ProductPerformanceDTO dto : performanceMap.values()) {
            BigDecimal profit = dto.getRevenue().subtract(dto.getCost());
            dto.setProfit(profit);

            // Calculate profit margin
            if (dto.getRevenue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal margin = profit
                        .divide(dto.getRevenue(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                dto.setProfitMargin(margin);
            }

            // Calculate ROI
            if (dto.getCost().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal roi = profit
                        .divide(dto.getCost(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                dto.setRoi(roi);
            }
        }

        return new ArrayList<>(performanceMap.values());
    }

    /**
     * Get category performance metrics
     */
    public List<CategoryPerformanceDTO> getCategoryPerformance(LocalDateTime startDate, LocalDateTime endDate) {
        List<Sale> paidSales = getSalesInRange(startDate, endDate).stream()
                .filter(s -> "PAID".equalsIgnoreCase(s.getStatus()))
                .collect(Collectors.toList());

        Map<Long, CategoryPerformanceDTO> performanceMap = new HashMap<>();

        // Initialize categories
        List<Category> allCategories = categoryRepository.findAll();
        for (Category category : allCategories) {
            performanceMap.put(category.getId(), new CategoryPerformanceDTO(
                    category.getId(),
                    category.getName(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0
            ));
        }

        // Calculate from sales
        for (Sale sale : paidSales) {
            for (SaleItem item : sale.getItems()) {
                Product product = item.getProduct();
                if (product.getCategory() != null) {
                    Long categoryId = product.getCategory().getId();
                    CategoryPerformanceDTO dto = performanceMap.get(categoryId);

                    if (dto != null) {
                        dto.setRevenue(dto.getRevenue().add(BigDecimal.valueOf(item.getSubtotal())));
                        dto.setQuantity(dto.getQuantity() + item.getQuantity());

                        if (product.getCostPrice() != null) {
                            BigDecimal itemCost = BigDecimal.valueOf(product.getCostPrice())
                                    .multiply(BigDecimal.valueOf(item.getQuantity()));
                            dto.setCost(dto.getCost().add(itemCost));
                        }
                    }
                }
            }
        }

        // Calculate profit and ROI
        for (CategoryPerformanceDTO dto : performanceMap.values()) {
            BigDecimal profit = dto.getRevenue().subtract(dto.getCost());
            dto.setProfit(profit);

            if (dto.getCost().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal roi = profit
                        .divide(dto.getCost(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                dto.setRoi(roi);
            }
        }

        // Filter out categories with no revenue
        return performanceMap.values().stream()
                .filter(dto -> dto.getRevenue().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    /**
     * Get supplier performance metrics
     */
    public List<SupplierPerformanceDTO> getSupplierPerformance(LocalDateTime startDate, LocalDateTime endDate) {
        List<Sale> paidSales = getSalesInRange(startDate, endDate).stream()
                .filter(s -> "PAID".equalsIgnoreCase(s.getStatus()))
                .collect(Collectors.toList());

        Map<Long, SupplierPerformanceDTO> performanceMap = new HashMap<>();

        // Initialize suppliers
        List<Supplier> allSuppliers = supplierRepository.findAll();
        for (Supplier supplier : allSuppliers) {
            performanceMap.put(supplier.getId(), new SupplierPerformanceDTO(
                    supplier.getId(),
                    supplier.getName(),
                    BigDecimal.ZERO,
                    0
            ));
        }

        // Calculate from sales
        for (Sale sale : paidSales) {
            for (SaleItem item : sale.getItems()) {
                Product product = item.getProduct();
                if (product.getSupplier() != null) {
                    Long supplierId = product.getSupplier().getId();
                    SupplierPerformanceDTO dto = performanceMap.get(supplierId);

                    if (dto != null) {
                        dto.setRevenue(dto.getRevenue().add(BigDecimal.valueOf(item.getSubtotal())));
                        dto.setProductCount(dto.getProductCount() + 1);
                    }
                }
            }
        }

        // Filter and sort
        return performanceMap.values().stream()
                .filter(dto -> dto.getRevenue().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(SupplierPerformanceDTO::getRevenue).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get stock status distribution
     */
    public StockStatusDTO getStockStatus() {
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

        return new StockStatusDTO(inStock, lowStock, outOfStock);
    }

    /**
     * Get inventory statistics
     */
    public InventoryStatsDTO getInventoryStats(LocalDateTime startDate, LocalDateTime endDate) {
        List<Product> allProducts = productRepository.findAll();

        // Total items in stock
        int totalItems = allProducts.stream()
                .mapToInt(Product::getCurrentStock)
                .sum();

        // Total inventory value
        BigDecimal totalValue = allProducts.stream()
                .map(p -> {
                    int stock = p.getCurrentStock();
                    Double cost = p.getCostPrice();
                    if (cost != null) {
                        return BigDecimal.valueOf(stock).multiply(BigDecimal.valueOf(cost));
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Average profit margin
        List<Product> productsWithPrices = allProducts.stream()
                .filter(p -> p.getCostPrice() != null && p.getSellingPrice() != null)
                .collect(Collectors.toList());

        BigDecimal avgMargin = BigDecimal.ZERO;
        if (!productsWithPrices.isEmpty()) {
            BigDecimal totalMargin = productsWithPrices.stream()
                    .map(p -> {
                        BigDecimal cost = BigDecimal.valueOf(p.getCostPrice());
                        BigDecimal selling = BigDecimal.valueOf(p.getSellingPrice());
                        if (cost.compareTo(BigDecimal.ZERO) > 0) {
                            return selling.subtract(cost)
                                    .divide(cost, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100));
                        }
                        return BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            avgMargin = totalMargin.divide(
                    BigDecimal.valueOf(productsWithPrices.size()),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        // Total items sold
        List<Sale> paidSales = getSalesInRange(startDate, endDate).stream()
                .filter(s -> "PAID".equalsIgnoreCase(s.getStatus()))
                .collect(Collectors.toList());

        int totalItemsSold = paidSales.stream()
                .flatMap(s -> s.getItems().stream())
                .mapToInt(SaleItem::getQuantity)
                .sum();

        return new InventoryStatsDTO(totalItems, totalValue, avgMargin, totalItemsSold);
    }

    /**
     * Generate smart recommendations based on data
     */
    public List<RecommendationDTO> getRecommendations() {
        List<RecommendationDTO> recommendations = new ArrayList<>();

        StockStatusDTO stockStatus = getStockStatus();
        List<Product> allProducts = productRepository.findAll();

        // Low stock alert
        if (stockStatus.getLowStock() > 0) {
            recommendations.add(new RecommendationDTO(
                    "warning",
                    "⚠️",
                    "Low Stock Alert",
                    stockStatus.getLowStock() + " products need restocking. Consider reordering soon.",
                    "View Items"
            ));
        }

        // Out of stock alert
        if (stockStatus.getOutOfStock() > 0) {
            recommendations.add(new RecommendationDTO(
                    "warning",
                    "❌",
                    "Out of Stock Items",
                    stockStatus.getOutOfStock() + " products are out of stock. Restock to avoid lost sales.",
                    "Restock Now"
            ));
        }

        // Get product performance for recommendations
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(30);
        List<ProductPerformanceDTO> performance = getProductPerformance(startDate, endDate);

        // Best seller recommendation
        ProductPerformanceDTO bestSeller = performance.stream()
                .max(Comparator.comparing(ProductPerformanceDTO::getRevenue))
                .orElse(null);

        if (bestSeller != null && bestSeller.getRevenue().compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add(new RecommendationDTO(
                    "success",
                    "🌟",
                    "Focus on Best Sellers",
                    bestSeller.getName() + " is your top performer with $" +
                            bestSeller.getRevenue().setScale(2, RoundingMode.HALF_UP) +
                            " in revenue. Ensure adequate stock levels.",
                    "View Details"
            ));
        }

        // High ROI recommendation
        ProductPerformanceDTO highRoi = performance.stream()
                .filter(p -> p.getRoi().compareTo(BigDecimal.valueOf(30)) > 0)
                .max(Comparator.comparing(ProductPerformanceDTO::getRoi))
                .orElse(null);

        if (highRoi != null) {
            recommendations.add(new RecommendationDTO(
                    "success",
                    "💰",
                    "High Profit Opportunity",
                    highRoi.getName() + " has " +
                            highRoi.getRoi().setScale(1, RoundingMode.HALF_UP) +
                            "% ROI. Consider promoting it.",
                    "Promote"
            ));
        }

        // Slow moving items
        ProductPerformanceDTO slowMover = performance.stream()
                .filter(p -> p.getQuantity() > 0 && p.getQuantity() < 5)
                .min(Comparator.comparing(ProductPerformanceDTO::getQuantity))
                .orElse(null);

        if (slowMover != null) {
            recommendations.add(new RecommendationDTO(
                    "info",
                    "📉",
                    "Slow Moving Items",
                    slowMover.getName() + " has low sales (" + slowMover.getQuantity() +
                            " units). Consider discounting to improve turnover.",
                    "Create Promotion"
            ));
        }

        return recommendations;
    }

    /**
     * Helper method to get sales in a date range
     */
    private List<Sale> getSalesInRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Sale> allSales = saleRepository.findAll();
        return allSales.stream()
                .filter(s -> {
                    LocalDateTime saleDate = s.getSaleDate();
                    return !saleDate.isBefore(startDate) && !saleDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }
}

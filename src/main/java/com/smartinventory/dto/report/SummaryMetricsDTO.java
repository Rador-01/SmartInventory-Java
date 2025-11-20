package com.smartinventory.dto.report;

import java.math.BigDecimal;

/**
 * DTO for overall summary metrics
 */
public class SummaryMetricsDTO {
    private BigDecimal totalRevenue;
    private BigDecimal totalProfit;
    private BigDecimal profitMarginPercent;
    private BigDecimal roiPercent;
    private BigDecimal turnoverRate;
    private Integer totalSales;
    private Integer totalProducts;

    public SummaryMetricsDTO() {
    }

    public SummaryMetricsDTO(BigDecimal totalRevenue, BigDecimal totalProfit,
                            BigDecimal profitMarginPercent, BigDecimal roiPercent,
                            BigDecimal turnoverRate, Integer totalSales, Integer totalProducts) {
        this.totalRevenue = totalRevenue;
        this.totalProfit = totalProfit;
        this.profitMarginPercent = profitMarginPercent;
        this.roiPercent = roiPercent;
        this.turnoverRate = turnoverRate;
        this.totalSales = totalSales;
        this.totalProducts = totalProducts;
    }

    // Getters and Setters
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(BigDecimal totalProfit) {
        this.totalProfit = totalProfit;
    }

    public BigDecimal getProfitMarginPercent() {
        return profitMarginPercent;
    }

    public void setProfitMarginPercent(BigDecimal profitMarginPercent) {
        this.profitMarginPercent = profitMarginPercent;
    }

    public BigDecimal getRoiPercent() {
        return roiPercent;
    }

    public void setRoiPercent(BigDecimal roiPercent) {
        this.roiPercent = roiPercent;
    }

    public BigDecimal getTurnoverRate() {
        return turnoverRate;
    }

    public void setTurnoverRate(BigDecimal turnoverRate) {
        this.turnoverRate = turnoverRate;
    }

    public Integer getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(Integer totalSales) {
        this.totalSales = totalSales;
    }

    public Integer getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(Integer totalProducts) {
        this.totalProducts = totalProducts;
    }
}

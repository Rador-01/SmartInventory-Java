package com.smartinventory.dto.report;

import java.math.BigDecimal;

/**
 * DTO for product performance metrics
 */
public class ProductPerformanceDTO {
    private Long id;
    private String name;
    private Integer quantity;
    private BigDecimal revenue;
    private BigDecimal cost;
    private BigDecimal profit;
    private BigDecimal profitMargin;
    private BigDecimal roi;

    public ProductPerformanceDTO() {
    }

    public ProductPerformanceDTO(Long id, String name, Integer quantity,
                                BigDecimal revenue, BigDecimal cost,
                                BigDecimal profit, BigDecimal profitMargin, BigDecimal roi) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.revenue = revenue;
        this.cost = cost;
        this.profit = profit;
        this.profitMargin = profitMargin;
        this.roi = roi;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }

    public BigDecimal getProfitMargin() {
        return profitMargin;
    }

    public void setProfitMargin(BigDecimal profitMargin) {
        this.profitMargin = profitMargin;
    }

    public BigDecimal getRoi() {
        return roi;
    }

    public void setRoi(BigDecimal roi) {
        this.roi = roi;
    }
}

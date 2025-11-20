package com.smartinventory.dto.report;

import java.math.BigDecimal;

/**
 * DTO for category performance metrics
 */
public class CategoryPerformanceDTO {
    private Long id;
    private String name;
    private BigDecimal revenue;
    private BigDecimal cost;
    private BigDecimal profit;
    private BigDecimal roi;
    private Integer quantity;

    public CategoryPerformanceDTO() {
    }

    public CategoryPerformanceDTO(Long id, String name, BigDecimal revenue,
                                 BigDecimal cost, BigDecimal profit,
                                 BigDecimal roi, Integer quantity) {
        this.id = id;
        this.name = name;
        this.revenue = revenue;
        this.cost = cost;
        this.profit = profit;
        this.roi = roi;
        this.quantity = quantity;
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

    public BigDecimal getRoi() {
        return roi;
    }

    public void setRoi(BigDecimal roi) {
        this.roi = roi;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

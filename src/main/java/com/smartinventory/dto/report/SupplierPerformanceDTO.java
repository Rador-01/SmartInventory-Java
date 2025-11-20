package com.smartinventory.dto.report;

import java.math.BigDecimal;

/**
 * DTO for supplier performance metrics
 */
public class SupplierPerformanceDTO {
    private Long id;
    private String name;
    private BigDecimal revenue;
    private Integer productCount;

    public SupplierPerformanceDTO() {
    }

    public SupplierPerformanceDTO(Long id, String name, BigDecimal revenue, Integer productCount) {
        this.id = id;
        this.name = name;
        this.revenue = revenue;
        this.productCount = productCount;
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

    public Integer getProductCount() {
        return productCount;
    }

    public void setProductCount(Integer productCount) {
        this.productCount = productCount;
    }
}

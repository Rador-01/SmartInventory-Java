package com.smartinventory.dto.report;

import java.math.BigDecimal;

/**
 * DTO for inventory statistics
 */
public class InventoryStatsDTO {
    private Integer totalItemsInStock;
    private BigDecimal totalInventoryValue;
    private BigDecimal averageProfitMargin;
    private Integer totalItemsSold;

    public InventoryStatsDTO() {
    }

    public InventoryStatsDTO(Integer totalItemsInStock, BigDecimal totalInventoryValue,
                            BigDecimal averageProfitMargin, Integer totalItemsSold) {
        this.totalItemsInStock = totalItemsInStock;
        this.totalInventoryValue = totalInventoryValue;
        this.averageProfitMargin = averageProfitMargin;
        this.totalItemsSold = totalItemsSold;
    }

    // Getters and Setters
    public Integer getTotalItemsInStock() {
        return totalItemsInStock;
    }

    public void setTotalItemsInStock(Integer totalItemsInStock) {
        this.totalItemsInStock = totalItemsInStock;
    }

    public BigDecimal getTotalInventoryValue() {
        return totalInventoryValue;
    }

    public void setTotalInventoryValue(BigDecimal totalInventoryValue) {
        this.totalInventoryValue = totalInventoryValue;
    }

    public BigDecimal getAverageProfitMargin() {
        return averageProfitMargin;
    }

    public void setAverageProfitMargin(BigDecimal averageProfitMargin) {
        this.averageProfitMargin = averageProfitMargin;
    }

    public Integer getTotalItemsSold() {
        return totalItemsSold;
    }

    public void setTotalItemsSold(Integer totalItemsSold) {
        this.totalItemsSold = totalItemsSold;
    }
}

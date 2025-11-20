package com.smartinventory.dto.report;

/**
 * DTO for stock status distribution
 */
public class StockStatusDTO {
    private Integer inStock;
    private Integer lowStock;
    private Integer outOfStock;

    public StockStatusDTO() {
    }

    public StockStatusDTO(Integer inStock, Integer lowStock, Integer outOfStock) {
        this.inStock = inStock;
        this.lowStock = lowStock;
        this.outOfStock = outOfStock;
    }

    // Getters and Setters
    public Integer getInStock() {
        return inStock;
    }

    public void setInStock(Integer inStock) {
        this.inStock = inStock;
    }

    public Integer getLowStock() {
        return lowStock;
    }

    public void setLowStock(Integer lowStock) {
        this.lowStock = lowStock;
    }

    public Integer getOutOfStock() {
        return outOfStock;
    }

    public void setOutOfStock(Integer outOfStock) {
        this.outOfStock = outOfStock;
    }
}

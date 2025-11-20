package com.smartinventory.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for sales trend data over time
 */
public class SalesTrendDTO {
    private List<LocalDate> labels;
    private List<BigDecimal> revenue;
    private List<BigDecimal> profit;

    public SalesTrendDTO() {
    }

    public SalesTrendDTO(List<LocalDate> labels, List<BigDecimal> revenue, List<BigDecimal> profit) {
        this.labels = labels;
        this.revenue = revenue;
        this.profit = profit;
    }

    // Getters and Setters
    public List<LocalDate> getLabels() {
        return labels;
    }

    public void setLabels(List<LocalDate> labels) {
        this.labels = labels;
    }

    public List<BigDecimal> getRevenue() {
        return revenue;
    }

    public void setRevenue(List<BigDecimal> revenue) {
        this.revenue = revenue;
    }

    public List<BigDecimal> getProfit() {
        return profit;
    }

    public void setProfit(List<BigDecimal> profit) {
        this.profit = profit;
    }
}

package com.smartinventory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * SaleItem Entity - Individual items within a sale
 *
 * This represents one product line in a sale.
 * For example: "2x Laptop @ $1000 each = $2000"
 */
@Entity
@Table(name = "sale_item")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY SALE ITEMS BELONG TO ONE SALE
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    // MANY SALE ITEMS BELONG TO ONE PRODUCT
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Quantity of this product sold
    @Column(nullable = false)
    private Integer quantity;

    // Unit price at the time of sale (captured because prices can change)
    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    // Subtotal = quantity * unitPrice (calculated automatically)
    @Column(nullable = false)
    private Double subtotal;

    // Discount applied to this item (optional)
    @Column
    private Double discount = 0.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ============================================
    // CONSTRUCTORS
    // ============================================

    public SaleItem() {
    }

    public SaleItem(Product product, Integer quantity, Double unitPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }

    public SaleItem(Product product, Integer quantity, Double unitPrice, Double discount) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discount = discount;
        calculateSubtotal();
    }

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        calculateSubtotal();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateSubtotal();
    }

    // ============================================
    // BUSINESS LOGIC METHODS
    // ============================================

    /**
     * Calculate subtotal: (quantity * unitPrice) - discount
     */
    public void calculateSubtotal() {
        if (quantity != null && unitPrice != null) {
            double total = quantity * unitPrice;
            if (discount != null && discount > 0) {
                total -= discount;
            }
            this.subtotal = total;
        }
    }

    /**
     * Get the discount percentage applied
     */
    public Double getDiscountPercentage() {
        if (discount == null || discount == 0 || unitPrice == null || unitPrice == 0) {
            return 0.0;
        }
        double originalPrice = quantity * unitPrice;
        return (discount / originalPrice) * 100;
    }

    // ============================================
    // GETTERS AND SETTERS
    // ============================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        calculateSubtotal();
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
        calculateSubtotal();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SaleItem{" +
                "id=" + id +
                ", productId=" + (product != null ? product.getId() : null) +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", discount=" + discount +
                ", subtotal=" + subtotal +
                ", createdAt=" + createdAt +
                '}';
    }
}

/**
 * USAGE EXAMPLE:
 *
 * // Create a sale item
 * Product laptop = productRepository.findById(1L).get();
 * SaleItem item = new SaleItem(laptop, 2, 1000.0);  // 2 laptops @ $1000 each
 *
 * // With discount
 * SaleItem itemWithDiscount = new SaleItem(laptop, 2, 1000.0, 100.0);  // $100 discount
 *
 * // Add to sale
 * sale.addItem(item);
 *
 * // Subtotal is calculated automatically
 * System.out.println(item.getSubtotal());  // 2000.0 or 1900.0 (with discount)
 */
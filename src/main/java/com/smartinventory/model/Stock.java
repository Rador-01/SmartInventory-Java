package com.smartinventory.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stock Entity - Stock movements (additions/removals)
 *
 * This tracks all inventory changes:
 * - Positive quantity = stock added (purchase, return)
 * - Negative quantity = stock removed (sale, damage)
 */
@Entity
@Table(name = "stock")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY STOCK RECORDS BELONG TO ONE PRODUCT
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonManagedReference("product-stocks")
    private Product product;

    // Quantity can be positive (added) or negative (removed)
    @Column(nullable = false)
    private Integer quantity;

    // Type of movement: "IN" (added) or "OUT" (removed)
    @Column(name = "movement_type", nullable = false, length = 10)
    private String movementType;

    // Reason for the movement
    @Column(length = 500)
    private String reason;

    // Reference to related document (e.g., "PURCHASE-001", "SALE-123")
    @Column(length = 100)
    private String reference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ============================================
    // CONSTRUCTORS
    // ============================================

    public Stock() {
    }

    public Stock(Product product, Integer quantity, String movementType, String reason) {
        this.product = product;
        this.quantity = quantity;
        this.movementType = movementType;
        this.reason = reason;
    }

    public Stock(Product product, Integer quantity, String movementType,
                 String reason, String reference) {
        this.product = product;
        this.quantity = quantity;
        this.movementType = movementType;
        this.reason = reason;
        this.reference = reference;
    }

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Check if this is a stock addition
     */
    public boolean isAddition() {
        return "IN".equalsIgnoreCase(movementType) || quantity > 0;
    }

    /**
     * Check if this is a stock removal
     */
    public boolean isRemoval() {
        return "OUT".equalsIgnoreCase(movementType) || quantity < 0;
    }

    @Override
    public String toString() {
        return "Stock{" +
                "id=" + id +
                ", productId=" + (product != null ? product.getId() : null) +
                ", quantity=" + quantity +
                ", movementType='" + movementType + '\'' +
                ", reason='" + reason + '\'' +
                ", reference='" + reference + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

/**
 * USAGE EXAMPLES:
 *
 * // Add stock (purchase)
 * Stock purchase = new Stock(product, 50, "IN", "Purchase from supplier", "PO-001");
 * stockRepository.save(purchase);
 *
 * // Remove stock (sale)
 * Stock sale = new Stock(product, -5, "OUT", "Sold to customer", "SALE-123");
 * stockRepository.save(sale);
 *
 * // Damaged goods
 * Stock damage = new Stock(product, -3, "OUT", "Damaged items");
 * stockRepository.save(damage);
 */
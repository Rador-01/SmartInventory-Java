package com.smartinventory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sale Entity - Sales transactions
 *
 * Represents a sale order containing multiple items.
 */
@Entity
@Table(name = "sale")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY SALES BELONG TO ONE CLIENT
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    // Sale reference number (e.g., "SALE-001")
    @Column(name = "sale_reference", unique = true, length = 50)
    private String saleReference;

    // Total amount of the sale
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount = 0.0;

    // Payment status: PENDING, PAID, CANCELLED
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    // Payment method: CASH, CARD, TRANSFER, etc.
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    // Additional notes
    @Column(length = 1000)
    private String notes;

    // ONE SALE HAS MANY SALE ITEMS
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @Column(name = "sale_date", nullable = false)
    private LocalDateTime saleDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============================================
    // CONSTRUCTORS
    // ============================================

    public Sale() {
    }

    public Sale(Client client, String saleReference, String paymentMethod) {
        this.client = client;
        this.saleReference = saleReference;
        this.paymentMethod = paymentMethod;
        this.saleDate = LocalDateTime.now();
    }

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.saleDate == null) {
            this.saleDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ============================================
    // BUSINESS LOGIC METHODS
    // ============================================

    /**
     * Calculate total amount from all sale items
     * Automatically updates totalAmount field
     */
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .mapToDouble(SaleItem::getSubtotal)
                .sum();
    }

    /**
     * Add an item to this sale
     */
    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
        calculateTotalAmount();
    }

    /**
     * Remove an item from this sale
     */
    public void removeItem(SaleItem item) {
        items.remove(item);
        item.setSale(null);
        calculateTotalAmount();
    }

    /**
     * Mark sale as paid
     */
    public void markAsPaid() {
        this.status = "PAID";
    }

    /**
     * Mark sale as cancelled
     */
    public void markAsCancelled() {
        this.status = "CANCELLED";
    }

    /**
     * Check if sale is paid
     */
    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(this.status);
    }

    /**
     * Check if sale is pending
     */
    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(this.status);
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

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getSaleReference() {
        return saleReference;
    }

    public void setSaleReference(String saleReference) {
        this.saleReference = saleReference;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<SaleItem> getItems() {
        return items;
    }

    public void setItems(List<SaleItem> items) {
        this.items = items;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Sale{" +
                "id=" + id +
                ", saleReference='" + saleReference + '\'' +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", itemCount=" + items.size() +
                ", saleDate=" + saleDate +
                '}';
    }
}

/**
 * USAGE EXAMPLE:
 *
 * // Create a sale
 * Sale sale = new Sale(client, "SALE-001", "CASH");
 *
 * // Add items
 * SaleItem item1 = new SaleItem(product1, 2, 100.0);
 * SaleItem item2 = new SaleItem(product2, 1, 50.0);
 * sale.addItem(item1);
 * sale.addItem(item2);
 *
 * // Total is calculated automatically
 * System.out.println(sale.getTotalAmount());  // 250.0
 *
 * // Mark as paid
 * sale.markAsPaid();
 *
 * saleRepository.save(sale);
 */
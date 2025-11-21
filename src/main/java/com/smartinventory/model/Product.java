package com.smartinventory.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Product Entity - Products in the inventory
 *
 * This is the main entity for products.
 * It has relationships with Category, Supplier, Stock, and SaleItem.
 */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String brand;

    // SKU = Stock Keeping Unit (unique product code)
    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    // Prices stored as Double (can be null initially)
    @Column(name = "cost_price")
    private Double costPrice;

    @Column(name = "selling_price")
    private Double sellingPrice;

    // MANY PRODUCTS BELONG TO ONE CATEGORY
    // This creates the relationship: Product → Category
    // @JoinColumn specifies the foreign key column name
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // MANY PRODUCTS BELONG TO ONE SUPPLIER
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    // ONE PRODUCT HAS MANY STOCK RECORDS (stock movements)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference("product-stocks")
    private List<Stock> stocks = new ArrayList<>();

    // ONE PRODUCT CAN BE IN MANY SALE ITEMS
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @JsonBackReference("product-saleitems")
    private List<SaleItem> saleItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============================================
    // CONSTRUCTORS
    // ============================================

    public Product() {
    }

    public Product(String name, String description, String brand, String sku,
                   Double costPrice, Double sellingPrice) {
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.sku = sku;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
    }

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ============================================
    // BUSINESS LOGIC METHODS
    // ============================================

    /**
     * Calculate current stock quantity from all stock movements
     * @return total quantity in stock
     */
    public int getCurrentStock() {
        return stocks.stream()
                .mapToInt(Stock::getQuantity)
                .sum();
    }

    /**
     * Calculate profit margin percentage
     * @return profit margin as percentage
     */
    public Double getProfitMargin() {
        if (costPrice == null || sellingPrice == null || costPrice == 0) {
            return 0.0;
        }
        return ((sellingPrice - costPrice) / costPrice) * 100;
    }

    /**
     * Check if product is low in stock (less than 10 units)
     * @return true if stock is low
     */
    public boolean isLowStock() {
        return getCurrentStock() < 10;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(Double costPrice) {
        this.costPrice = costPrice;
    }

    public Double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(Double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }

    public List<SaleItem> getSaleItems() {
        return saleItems;
    }

    public void setSaleItems(List<SaleItem> saleItems) {
        this.saleItems = saleItems;
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

    // ============================================
    // UTILITY METHODS
    // ============================================

    public void addStock(Stock stock) {
        stocks.add(stock);
        stock.setProduct(this);
    }

    public void removeStock(Stock stock) {
        stocks.remove(stock);
        stock.setProduct(null);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", brand='" + brand + '\'' +
                ", sku='" + sku + '\'' +
                ", costPrice=" + costPrice +
                ", sellingPrice=" + sellingPrice +
                ", currentStock=" + getCurrentStock() +
                ", createdAt=" + createdAt +
                '}';
    }
}

/**
 * RELATIONSHIP SUMMARY:
 *
 * Product ← Many-to-One → Category
 * Product ← Many-to-One → Supplier
 * Product ← One-to-Many → Stock
 * Product ← One-to-Many → SaleItem
 *
 * Example usage:
 * Product laptop = new Product("Laptop", "Gaming laptop", "Dell", "SKU001", 800.0, 1200.0);
 * laptop.setCategory(electronicsCategory);
 * laptop.setSupplier(dellSupplier);
 * productRepository.save(laptop);
 */
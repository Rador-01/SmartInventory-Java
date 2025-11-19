package com.smartinventory.service;

import com.smartinventory.model.SaleItem;
import com.smartinventory.model.Product;
import com.smartinventory.repository.SaleItemRepository;
import com.smartinventory.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SaleItemService - Manages sale items
 */
@Service
public class SaleItemService {

    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;

    @Autowired
    public SaleItemService(SaleItemRepository saleItemRepository,
                           ProductRepository productRepository) {
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
    }

    /**
     * Get all sale items
     */
    public List<SaleItem> getAllSaleItems() {
        return saleItemRepository.findAll();
    }

    /**
     * Get sale item by ID
     */
    public SaleItem getSaleItemById(Long id) {
        return saleItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale item not found with id: " + id));
    }

    /**
     * Get sale items by sale ID
     */
    public List<SaleItem> getSaleItemsBySaleId(Long saleId) {
        return saleItemRepository.findBySaleId(saleId);
    }

    /**
     * Get sale items by product ID
     */
    public List<SaleItem> getSaleItemsByProductId(Long productId) {
        return saleItemRepository.findByProductId(productId);
    }

    /**
     * Calculate total quantity sold for a product
     */
    public Integer calculateTotalQuantitySold(Long productId) {
        return saleItemRepository.calculateTotalQuantitySold(productId);
    }

    /**
     * Calculate total revenue for a product
     */
    public Double calculateTotalRevenue(Long productId) {
        return saleItemRepository.calculateTotalRevenueByProduct(productId);
    }

    /**
     * Get top selling products
     * Returns list of [Product, Quantity] pairs
     */
    public List<Object[]> getTopSellingProducts() {
        return saleItemRepository.findTopSellingProducts();
    }

    /**
     * Get items with discount
     */
    public List<SaleItem> getItemsWithDiscount() {
        return saleItemRepository.findItemsWithDiscount();
    }

    /**
     * Calculate total discounts given
     */
    public Double calculateTotalDiscounts() {
        return saleItemRepository.calculateTotalDiscounts();
    }

    /**
     * Create sale item
     * Note: Usually sale items are created with the sale, not separately
     */
    public SaleItem createSaleItem(SaleItem saleItem) {
        // Validate product
        if (saleItem.getProduct() != null && saleItem.getProduct().getId() != null) {
            Product product = productRepository.findById(saleItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            saleItem.setProduct(product);

            // Set unit price from product if not provided
            if (saleItem.getUnitPrice() == null) {
                saleItem.setUnitPrice(product.getSellingPrice());
            }
        }

        // Calculate subtotal
        saleItem.calculateSubtotal();

        return saleItemRepository.save(saleItem);
    }

    /**
     * Update sale item
     */
    public SaleItem updateSaleItem(Long id, SaleItem saleItemDetails) {
        SaleItem saleItem = getSaleItemById(id);

        if (saleItemDetails.getQuantity() != null) {
            saleItem.setQuantity(saleItemDetails.getQuantity());
        }

        if (saleItemDetails.getUnitPrice() != null) {
            saleItem.setUnitPrice(saleItemDetails.getUnitPrice());
        }

        if (saleItemDetails.getDiscount() != null) {
            saleItem.setDiscount(saleItemDetails.getDiscount());
        }

        // Recalculate subtotal
        saleItem.calculateSubtotal();

        return saleItemRepository.save(saleItem);
    }

    /**
     * Delete sale item
     */
    public void deleteSaleItem(Long id) {
        SaleItem saleItem = getSaleItemById(id);
        saleItemRepository.delete(saleItem);
    }
}
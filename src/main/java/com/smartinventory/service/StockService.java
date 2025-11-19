package com.smartinventory.service;

import com.smartinventory.model.Stock;
import com.smartinventory.model.Product;
import com.smartinventory.repository.StockRepository;
import com.smartinventory.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;

    @Autowired
    public StockService(StockRepository stockRepository, ProductRepository productRepository) {
        this.stockRepository = stockRepository;
        this.productRepository = productRepository;
    }

    /**
     * Get all stock movements
     */
    public List<Stock> getAllStockMovements() {
        return stockRepository.findAll();
    }

    /**
     * Get stock movement by ID
     */
    public Stock getStockById(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock movement not found with id: " + id));
    }

    /**
     * Add stock (incoming stock)
     */
    public Stock addStock(Long productId, Integer quantity, String reason, String reference) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be positive for stock addition");
        }

        Stock stock = new Stock(product, quantity, "IN", reason, reference);
        return stockRepository.save(stock);
    }

    /**
     * Remove stock (outgoing stock)
     */
    public Stock removeStock(Long productId, Integer quantity, String reason, String reference) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be positive for stock removal");
        }

        // Check if enough stock available
        int currentStock = stockRepository.calculateTotalStock(productId);
        if (currentStock < quantity) {
            throw new RuntimeException("Insufficient stock. Available: " + currentStock + ", Requested: " + quantity);
        }

        Stock stock = new Stock(product, -quantity, "OUT", reason, reference);
        return stockRepository.save(stock);
    }

    /**
     * Get stock movements for a product
     */
    public List<Stock> getStockMovementsByProduct(Long productId) {
        return stockRepository.findByProductId(productId);
    }

    /**
     * Get recent stock movements
     */
    public List<Stock> getRecentStockMovements() {
        return stockRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * Get stock movements by date range
     */
    public List<Stock> getStockMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return stockRepository.findByCreatedAtBetween(startDate, endDate);
    }

    /**
     * Calculate current stock for a product
     */
    public Integer calculateCurrentStock(Long productId) {
        return stockRepository.calculateTotalStock(productId);
    }

    /**
     * Get stock additions
     */
    public List<Stock> getStockAdditions() {
        return stockRepository.findStockAdditions();
    }

    /**
     * Get stock removals
     */
    public List<Stock> getStockRemovals() {
        return stockRepository.findStockRemovals();
    }
}
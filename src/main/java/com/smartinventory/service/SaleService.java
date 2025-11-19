package com.smartinventory.service;

import com.smartinventory.model.Sale;
import com.smartinventory.model.SaleItem;
import com.smartinventory.model.Client;
import com.smartinventory.model.Product;
import com.smartinventory.repository.SaleRepository;
import com.smartinventory.repository.ClientRepository;
import com.smartinventory.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;

    @Autowired
    public SaleService(SaleRepository saleRepository,
                       ClientRepository clientRepository,
                       ProductRepository productRepository,
                       StockService stockService) {
        this.saleRepository = saleRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.stockService = stockService;
    }

    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found with id: " + id));
    }

    public Sale getSaleByReference(String reference) {
        return saleRepository.findBySaleReference(reference)
                .orElseThrow(() -> new RuntimeException("Sale not found with reference: " + reference));
    }

    /**
     * Create a new sale
     * @Transactional ensures all operations succeed or all fail (rollback)
     */
    @Transactional
    public Sale createSale(Sale sale) {
        // Validate client if provided
        if (sale.getClient() != null && sale.getClient().getId() != null) {
            Client client = clientRepository.findById(sale.getClient().getId())
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            sale.setClient(client);
        }

        // Check if reference already exists
        if (sale.getSaleReference() != null && saleRepository.existsBySaleReference(sale.getSaleReference())) {
            throw new RuntimeException("Sale reference already exists");
        }

        // Generate reference if not provided
        if (sale.getSaleReference() == null) {
            sale.setSaleReference(generateSaleReference());
        }

        // Process sale items
        if (sale.getItems() != null && !sale.getItems().isEmpty()) {
            for (SaleItem item : sale.getItems()) {
                // Validate product
                Product product = productRepository.findById(item.getProduct().getId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                item.setProduct(product);

                // Set unit price from product if not provided
                if (item.getUnitPrice() == null) {
                    item.setUnitPrice(product.getSellingPrice());
                }

                // Calculate subtotal
                item.calculateSubtotal();

                // Link to sale
                item.setSale(sale);

                // Remove stock if sale is paid
                if ("PAID".equalsIgnoreCase(sale.getStatus())) {
                    stockService.removeStock(
                            product.getId(),
                            item.getQuantity(),
                            "Sale: " + sale.getSaleReference(),
                            sale.getSaleReference()
                    );
                }
            }
        }

        // Calculate total amount
        sale.calculateTotalAmount();

        return saleRepository.save(sale);
    }

    /**
     * Update sale status
     */
    @Transactional
    public Sale updateSaleStatus(Long id, String status) {
        Sale sale = getSaleById(id);
        String oldStatus = sale.getStatus();

        sale.setStatus(status);

        // If changing to PAID, remove stock
        if ("PAID".equalsIgnoreCase(status) && !"PAID".equalsIgnoreCase(oldStatus)) {
            for (SaleItem item : sale.getItems()) {
                stockService.removeStock(
                        item.getProduct().getId(),
                        item.getQuantity(),
                        "Sale: " + sale.getSaleReference(),
                        sale.getSaleReference()
                );
            }
        }

        return saleRepository.save(sale);
    }

    /**
     * Delete sale (cancel)
     */
    @Transactional
    public void deleteSale(Long id) {
        Sale sale = getSaleById(id);

        // If sale was paid, add stock back
        if ("PAID".equalsIgnoreCase(sale.getStatus())) {
            for (SaleItem item : sale.getItems()) {
                stockService.addStock(
                        item.getProduct().getId(),
                        item.getQuantity(),
                        "Sale cancelled: " + sale.getSaleReference(),
                        sale.getSaleReference()
                );
            }
        }

        saleRepository.delete(sale);
    }

    public List<Sale> getSalesByClient(Long clientId) {
        return saleRepository.findByClientId(clientId);
    }

    public List<Sale> getSalesByStatus(String status) {
        return saleRepository.findByStatus(status);
    }

    public List<Sale> getSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return saleRepository.findBySaleDateBetween(startDate, endDate);
    }

    public List<Sale> getPendingSales() {
        return saleRepository.findPendingSales();
    }

    public List<Sale> getRecentSales() {
        return saleRepository.findTop10ByOrderBySaleDateDesc();
    }

    public Double calculateTotalSales(LocalDateTime startDate, LocalDateTime endDate) {
        return saleRepository.calculateTotalSales(startDate, endDate);
    }

    /**
     * Generate unique sale reference
     */
    private String generateSaleReference() {
        String prefix = "SALE-";
        long count = saleRepository.count() + 1;
        return prefix + String.format("%06d", count);
    }
}
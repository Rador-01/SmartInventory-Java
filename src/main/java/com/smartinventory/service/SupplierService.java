package com.smartinventory.service;

import com.smartinventory.model.Supplier;
import com.smartinventory.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Autowired
    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
    }

    public Supplier createSupplier(Supplier supplier) {
        if (supplierRepository.existsByName(supplier.getName())) {
            throw new RuntimeException("Supplier with name " + supplier.getName() + " already exists");
        }
        return supplierRepository.save(supplier);
    }

    public Supplier updateSupplier(Long id, Supplier supplierDetails) {
        Supplier supplier = getSupplierById(id);

        if (supplierDetails.getName() != null && !supplierDetails.getName().equals(supplier.getName())) {
            if (supplierRepository.existsByName(supplierDetails.getName())) {
                throw new RuntimeException("Supplier with name " + supplierDetails.getName() + " already exists");
            }
            supplier.setName(supplierDetails.getName());
        }

        if (supplierDetails.getContactPerson() != null) {
            supplier.setContactPerson(supplierDetails.getContactPerson());
        }

        if (supplierDetails.getPhone() != null) {
            supplier.setPhone(supplierDetails.getPhone());
        }

        if (supplierDetails.getEmail() != null) {
            supplier.setEmail(supplierDetails.getEmail());
        }

        if (supplierDetails.getAddress() != null) {
            supplier.setAddress(supplierDetails.getAddress());
        }

        return supplierRepository.save(supplier);
    }

    public void deleteSupplier(Long id) {
        Supplier supplier = getSupplierById(id);
        supplierRepository.delete(supplier);
    }

    public List<Supplier> searchSuppliers(String searchTerm) {
        return supplierRepository.findByNameContainingIgnoreCase(searchTerm);
    }
}
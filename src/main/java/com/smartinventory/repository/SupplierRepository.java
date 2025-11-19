package com.smartinventory.repository;

import com.smartinventory.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * SupplierRepository - Database access for Supplier entity
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /**
     * Find supplier by name
     * Query: SELECT * FROM supplier WHERE name = ?
     */
    Optional<Supplier> findByName(String name);

    /**
     * Check if supplier name exists
     * Query: SELECT COUNT(*) > 0 FROM supplier WHERE name = ?
     */
    boolean existsByName(String name);

    /**
     * Find supplier by email
     * Query: SELECT * FROM supplier WHERE email = ?
     */
    Optional<Supplier> findByEmail(String email);

    /**
     * Find suppliers by name containing (search)
     * Query: SELECT * FROM supplier WHERE name LIKE %?%
     */
    java.util.List<Supplier> findByNameContainingIgnoreCase(String name);

    /**
     * Find suppliers by phone
     * Query: SELECT * FROM supplier WHERE phone = ?
     */
    java.util.List<Supplier> findByPhone(String phone);
}
package com.smartinventory.repository;

import com.smartinventory.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ClientRepository - Database access for Client entity
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    /**
     * Find client by email
     * Query: SELECT * FROM client WHERE email = ?
     */
    Optional<Client> findByEmail(String email);

    /**
     * Find client by phone
     * Query: SELECT * FROM client WHERE phone = ?
     */
    Optional<Client> findByPhone(String phone);

    /**
     * Check if email exists
     * Query: SELECT COUNT(*) > 0 FROM client WHERE email = ?
     */
    boolean existsByEmail(String email);

    /**
     * Check if phone exists
     * Query: SELECT COUNT(*) > 0 FROM client WHERE phone = ?
     */
    boolean existsByPhone(String phone);

    /**
     * Find clients by name containing (search)
     * Query: SELECT * FROM client WHERE name LIKE %?%
     */
    List<Client> findByNameContainingIgnoreCase(String name);

    /**
     * Find clients by company
     * Query: SELECT * FROM client WHERE company = ?
     */
    List<Client> findByCompany(String company);

    /**
     * Find clients by company containing (search)
     * Query: SELECT * FROM client WHERE company LIKE %?%
     */
    List<Client> findByCompanyContainingIgnoreCase(String company);

    /**
     * Search clients by name or company
     */
    @Query("SELECT c FROM Client c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.company) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Client> searchClients(String searchTerm);

    /**
     * Find top clients by total purchases
     * This is a complex query that joins with Sales table
     */
    @Query("SELECT c FROM Client c LEFT JOIN c.sales s " +
            "GROUP BY c.id " +
            "ORDER BY SUM(s.totalAmount) DESC")
    List<Client> findTopClientsByPurchases();
}
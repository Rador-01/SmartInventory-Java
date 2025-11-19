package com.smartinventory.repository;

import com.smartinventory.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CategoryRepository - Database access for Category entity
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find category by name
     * Query: SELECT * FROM category WHERE name = ?
     */
    Optional<Category> findByName(String name);

    /**
     * Check if category name exists
     * Query: SELECT COUNT(*) > 0 FROM category WHERE name = ?
     */
    boolean existsByName(String name);

    /**
     * Find categories by name containing (search)
     * Query: SELECT * FROM category WHERE name LIKE %?%
     */
    java.util.List<Category> findByNameContainingIgnoreCase(String name);
}
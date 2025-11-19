package com.smartinventory.repository;

import com.smartinventory.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository - Database access for User entity
 *
 * This is just an INTERFACE - no code needed!
 *
 * By extending JpaRepository<User, Long>, Spring Boot automatically creates:
 * - findAll() - Get all users
 * - findById(id) - Get user by ID
 * - save(user) - Save or update user
 * - delete(user) - Delete user
 * - deleteById(id) - Delete user by ID
 * - count() - Count total users
 * - existsById(id) - Check if user exists
 *
 * MAGIC: We just declare methods by name, Spring creates the implementation!
 */
@Repository  // Tells Spring: "This is a repository"
public interface UserRepository extends JpaRepository<User, Long> {

    // CUSTOM QUERIES
    // Spring creates these methods automatically based on the method name!

    /**
     * Find user by username
     * Spring automatically creates the query: SELECT * FROM user WHERE username = ?
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     * Spring automatically creates: SELECT * FROM user WHERE email = ?
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists
     * Spring automatically creates: SELECT COUNT(*) > 0 FROM user WHERE username = ?
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     * Spring automatically creates: SELECT COUNT(*) > 0 FROM user WHERE email = ?
     */
    boolean existsByEmail(String email);

    /**
     * Find user by username or email
     * Spring automatically creates: SELECT * FROM user WHERE username = ? OR email = ?
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Find all users by role
     * Spring automatically creates: SELECT * FROM user WHERE role = ?
     */
    java.util.List<User> findByRole(String role);
}

/**
 * HOW TO USE THIS REPOSITORY:
 *
 * In your service or controller, just inject this repository:
 *
 * @Autowired
 * private UserRepository userRepository;
 *
 * Then use it:
 *
 * // Get all users
 * List<User> users = userRepository.findAll();
 *
 * // Find by ID
 * Optional<User> user = userRepository.findById(1L);
 *
 * // Find by username
 * Optional<User> user = userRepository.findByUsername("john");
 *
 * // Save/update user
 * User newUser = new User("john", "john@email.com", "password", "USER");
 * userRepository.save(newUser);  // Automatically saves to database!
 *
 * // Delete user
 * userRepository.deleteById(1L);
 *
 * // Check if exists
 * boolean exists = userRepository.existsByUsername("john");
 *
 * NO SQL NEEDED! Spring creates all the queries automatically!
 */
package com.smartinventory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * User Entity - Represents users in the system
 *
 * This is a normal Java class with JPA annotations.
 * JPA will automatically create a 'user' table in the database.
 *
 * Annotations explained:
 * @Entity - Tells JPA: "This is a database table"
 * @Table - Specifies the table name
 * @Id - Marks the primary key field
 * @GeneratedValue - Auto-increment the ID
 * @Column - Configures column properties (unique, nullable, length, etc.)
 */
@Entity
@Table(name = "user")  // Table name in database
public class User {

    // PRIMARY KEY
    @Id  // This is the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment
    private Long id;

    // USERNAME - must be unique
    @Column(nullable = false, unique = true, length = 80)
    private String username;

    // EMAIL - must be unique
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    // PASSWORD - hashed password stored here
    @Column(nullable = false, length = 255)
    private String password;

    // ROLE - user role (e.g., "ADMIN", "USER")
    @Column(nullable = false, length = 50)
    private String role = "USER";  // Default value

    // TIMESTAMPS
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============================================
    // CONSTRUCTORS
    // ============================================

    /**
     * Default constructor (required by JPA)
     * JPA needs this to create objects from database rows
     */
    public User() {
    }

    /**
     * Constructor with all fields (for creating new users)
     */
    public User(String username, String email, String password, String role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    /**
     * Called automatically before saving to database
     * Sets createdAt timestamp
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Called automatically before updating in database
     * Updates the updatedAt timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ============================================
    // GETTERS AND SETTERS
    // ============================================
    // These are normal Java getters and setters
    // JPA uses them to read/write data

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

/**
 * HOW THIS WORKS:
 *
 * 1. When Spring Boot starts, it sees @Entity annotation
 * 2. It creates a table named "user" with these columns:
 *    - id (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
 *    - username (VARCHAR(80), UNIQUE, NOT NULL)
 *    - email (VARCHAR(120), UNIQUE, NOT NULL)
 *    - password (VARCHAR(255), NOT NULL)
 *    - role (VARCHAR(50), NOT NULL, DEFAULT 'USER')
 *    - created_at (DATETIME, NOT NULL)
 *    - updated_at (DATETIME)
 *
 * 3. You can use this class like a normal Java object:
 *    User user = new User("john", "john@email.com", "hashed_password", "ADMIN");
 *    userRepository.save(user);  // Saves to database automatically
 */
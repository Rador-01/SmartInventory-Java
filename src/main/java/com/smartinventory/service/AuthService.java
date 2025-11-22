package com.smartinventory.service;

import com.smartinventory.model.User;
import com.smartinventory.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * AuthService - Handles authentication and user registration
 *
 * This is a normal Java class with @Service annotation.
 * The @Service tells Spring: "This is a service component"
 *
 * Services contain business logic and use repositories to access data.
 */
@Service
public class AuthService {

    // DEPENDENCY INJECTION
    // Spring automatically provides these objects
    // No need for "new" keyword!

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Constructor injection (recommended way)
     * Spring automatically calls this and provides the dependencies
     */
    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ============================================
    // AUTHENTICATION METHODS
    // ============================================

    /**
     * Register a new user
     *
     * @param username - Username
     * @param email - Email address
     * @param password - Plain password (will be hashed)
     * @param role - User role (USER, ADMIN)
     * @return Created user
     * @throws RuntimeException if username or email already exists
     */
    public User register(String username, String email, String password, String role) {
        // 1. Check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        // 2. Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        // 3. Hash the password (NEVER store plain passwords!)
        String hashedPassword = passwordEncoder.encode(password);

        // 4. Create new user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(hashedPassword);
        user.setRole(role != null ? role : "USER");

        // 5. Save to database
        return userRepository.save(user);
    }

    /**
     * Login user
     *
     * @param username - Username or email
     * @param password - Plain password
     * @return JWT token if successful
     * @throws RuntimeException if credentials are invalid
     */
    public String login(String username, String password) {
        // 1. Find user by username or email
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(username, username);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid credentials");
        }

        User user = userOpt.get();

        // 2. Check password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // 3. Generate and return JWT token
        return jwtService.generateToken(user);
    }

    /**
     * Validate JWT token
     *
     * @param token - JWT token
     * @return User if token is valid
     * @throws RuntimeException if token is invalid
     */
    public User validateToken(String token) {
        // 1. Extract username from token
        String username = jwtService.extractUsername(token);

        // 2. Find user
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        // 3. Validate token
        if (!jwtService.isTokenValid(token, user)) {
            throw new RuntimeException("Invalid token");
        }

        return user;
    }

    /**
     * Change user password
     *
     * @param userId - User ID
     * @param oldPassword - Current password
     * @param newPassword - New password
     * @throws RuntimeException if old password is incorrect
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        // 1. Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Incorrect password");
        }

        // 3. Hash and save new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Get current user by username
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Get current user by ID
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}

/**
 * EXPLANATION:
 *
 * 1. @Service annotation tells Spring this is a service component
 * 2. Dependencies (UserRepository, PasswordEncoder, JwtService) are injected automatically
 * 3. All methods are normal Java methods containing business logic
 * 4. We use repositories to access the database
 * 5. We throw exceptions when things go wrong (controllers will handle them)
 *
 * USAGE (in controller):
 *
 * @Autowired
 * private AuthService authService;
 *
 * // Register user
 * User user = authService.register("john", "john@email.com", "password123", "USER");
 *
 * // Login user
 * String token = authService.login("john", "password123");
 *
 * // Validate token
 * User user = authService.validateToken(token);
 */
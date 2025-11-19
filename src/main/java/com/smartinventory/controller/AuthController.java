package com.smartinventory.controller;

import com.smartinventory.model.User;
import com.smartinventory.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthController - Handles authentication endpoints
 *
 * Endpoints:
 * POST /api/auth/register - Register new user
 * POST /api/auth/login - Login user
 * GET /api/auth/me - Get current user info
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")  // Allow all origins for auth endpoints
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     * Register a new user
     *
     * Request Body:
     * {
     *   "username": "john",
     *   "email": "john@email.com",
     *   "password": "password123",
     *   "role": "USER"
     * }
     *
     * Response:
     * {
     *   "message": "User registered successfully",
     *   "user": { ... },
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Register user
            User user = authService.register(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getRole()
            );

            // Generate token
            String token = authService.login(request.getUsername(), request.getPassword());

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("user", user);
            response.put("token", token);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * POST /api/auth/login
     * Login user
     *
     * Request Body:
     * {
     *   "username": "john",
     *   "password": "password123"
     * }
     *
     * Response:
     * {
     *   "message": "Login successful",
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "user": { ... }
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Login and get token
            String token = authService.login(request.getUsername(), request.getPassword());

            // Get user info
            User user = authService.getUserByUsername(request.getUsername());

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("user", user);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * GET /api/auth/me
     * Get current user info from token
     *
     * Headers:
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * Response:
     * {
     *   "id": 1,
     *   "username": "john",
     *   "email": "john@email.com",
     *   "role": "USER"
     * }
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract token from "Bearer <token>"
            String token = authHeader.substring(7);

            // Validate token and get user
            User user = authService.validateToken(token);

            return ResponseEntity.ok(user);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid or expired token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    // ============================================
    // REQUEST DTOs (Data Transfer Objects)
    // ============================================

    /**
     * Login request body
     */
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * Register request body
     */
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}

/**
 * HOW TO TEST THIS API:
 *
 * 1. Register a new user:
 * POST http://localhost:5001/api/auth/register
 * Body: {
 *   "username": "admin",
 *   "email": "admin@test.com",
 *   "password": "admin123",
 *   "role": "ADMIN"
 * }
 *
 * 2. Login:
 * POST http://localhost:5001/api/auth/login
 * Body: {
 *   "username": "admin",
 *   "password": "admin123"
 * }
 *
 * Response will include a token like:
 * {
 *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "user": { ... }
 * }
 *
 * 3. Get current user info:
 * GET http://localhost:5001/api/auth/me
 * Headers: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 */
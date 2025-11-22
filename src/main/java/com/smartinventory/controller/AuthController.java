package com.smartinventory.controller;

import com.smartinventory.model.User;
import com.smartinventory.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
 * POST /api/auth/logout - Logout current user
 * POST /api/auth/logout-all - Logout from all devices
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
    @PostMapping(value = "/register", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        try {
            // Register user
            User user = authService.register(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getRole()
            );

            // Generate token
            String token = authService.login(request.getUsername(), request.getPassword(), httpRequest);

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
    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            // Login and get token
            String token = authService.login(request.getUsername(), request.getPassword(), httpRequest);

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
     * POST /api/auth/logout
     * Logout current user (revoke token)
     *
     * Headers:
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * Response:
     * {
     *   "message": "Logout successful"
     * }
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Validate Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid or missing Authorization header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            // Extract token
            String token = authHeader.substring(7);

            // Logout (revoke token)
            boolean success = authService.logout(token);

            if (success) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Logout successful");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Token not found or already revoked");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/auth/logout-all
     * Logout from all devices (revoke all user tokens)
     *
     * Headers:
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * Response:
     * {
     *   "message": "Logged out from all devices"
     * }
     */
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAllDevices(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Validate Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid or missing Authorization header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            // Extract token and validate
            String token = authHeader.substring(7);
            User user = authService.validateToken(token);

            // Logout from all devices
            authService.logoutAllDevices(user.getId());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Logged out from all devices");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Validate Authorization header
            if (authHeader == null || authHeader.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authorization header is missing");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            if (!authHeader.startsWith("Bearer ")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authorization header must start with 'Bearer '");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            if (authHeader.length() <= 7) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authorization token is missing");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

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
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 80, message = "Username must be between 3 and 80 characters")
        private String username;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
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
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 80, message = "Username must be between 3 and 80 characters")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 120, message = "Email must not exceed 120 characters")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters")
        private String password;

        @Size(max = 50, message = "Role must not exceed 50 characters")
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
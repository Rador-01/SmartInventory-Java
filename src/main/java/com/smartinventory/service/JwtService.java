package com.smartinventory.service;

import com.smartinventory.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtService - Handles JWT token operations
 *
 * This service creates, validates, and extracts information from JWT tokens.
 * Updated for JJWT 0.12.x
 */
@Service
public class JwtService {

    // Read secret key from application.properties
    @Value("${jwt.secret}")
    private String secretKey;

    // Read expiration time from application.properties (in milliseconds)
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // ============================================
    // TOKEN GENERATION
    // ============================================

    /**
     * Generate JWT token for a user
     *
     * @param user - User object
     * @return JWT token string
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole());

        return createToken(claims, user.getUsername());
    }

    /**
     * Create JWT token with claims
     *
     * @param claims - Additional data to include in token
     * @param subject - Username (subject of the token)
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ============================================
    // TOKEN VALIDATION
    // ============================================

    /**
     * Validate token against user
     *
     * @param token - JWT token
     * @param user - User object
     * @return true if token is valid
     */
    public boolean isTokenValid(String token, User user) {
        final String username = extractUsername(token);
        return (username.equals(user.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Check if token is expired
     *
     * @param token - JWT token
     * @return true if expired
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ============================================
    // EXTRACT INFORMATION FROM TOKEN
    // ============================================

    /**
     * Extract username from token
     *
     * @param token - JWT token
     * @return Username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user ID from token
     *
     * @param token - JWT token
     * @return User ID
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Extract user role from token
     *
     * @param token - JWT token
     * @return User role
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Extract expiration date from token
     *
     * @param token - JWT token
     * @return Expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from token
     *
     * @param token - JWT token
     * @param claimsResolver - Function to extract specific claim
     * @return Claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     *
     * @param token - JWT token
     * @return All claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Get signing key for JWT
     *
     * @return Signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

/**
 * HOW JWT WORKS:
 *
 * 1. User logs in with username/password
 * 2. Server validates credentials
 * 3. Server generates JWT token containing user info
 * 4. Server sends token to client
 * 5. Client stores token (usually in localStorage)
 * 6. Client sends token in every request (Authorization header)
 * 7. Server validates token and extracts user info
 *
 * JWT Token Structure:
 * eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoiam9obiJ9.signature
 *
 * Part 1: Header (algorithm and token type)
 * Part 2: Payload (user data - encoded, NOT encrypted!)
 * Part 3: Signature (verifies token hasn't been tampered with)
 *
 * USAGE:
 *
 * // Generate token
 * String token = jwtService.generateToken(user);
 *
 * // Validate token
 * boolean valid = jwtService.isTokenValid(token, user);
 *
 * // Extract username
 * String username = jwtService.extractUsername(token);
 *
 * // Extract user ID
 * Long userId = jwtService.extractUserId(token);
 */
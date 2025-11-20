package com.smartinventory.service;

import com.smartinventory.model.Auth;
import com.smartinventory.model.User;
import com.smartinventory.repository.AuthRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * TokenBlacklistService - Manages JWT token blacklisting for logout
 *
 * This service:
 * 1. Stores tokens in database when issued
 * 2. Marks tokens as revoked on logout
 * 3. Checks if tokens are blacklisted during authentication
 * 4. Cleans up expired tokens
 */
@Service
public class TokenBlacklistService {

    private final AuthRepository authRepository;
    private final JwtService jwtService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Autowired
    public TokenBlacklistService(AuthRepository authRepository, JwtService jwtService) {
        this.authRepository = authRepository;
        this.jwtService = jwtService;
    }

    /**
     * Store token when user logs in
     *
     * @param user - User who logged in
     * @param token - JWT token
     * @param request - HTTP request to extract user agent and IP
     */
    public void storeToken(User user, String token, HttpServletRequest request) {
        // Calculate expiration time
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtExpiration / 1000);

        // Extract user agent and IP address
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);

        // Create Auth entity
        Auth auth = new Auth(user, token, expiresAt, userAgent, ipAddress);
        auth.setTokenType("Bearer");

        // Save to database
        authRepository.save(auth);
    }

    /**
     * Revoke a specific token (logout)
     *
     * @param token - JWT token to revoke
     * @return true if token was revoked, false if not found
     */
    @Transactional
    public boolean revokeToken(String token) {
        Optional<Auth> authOpt = authRepository.findByToken(token);

        if (authOpt.isPresent()) {
            Auth auth = authOpt.get();
            auth.revoke();
            authRepository.save(auth);
            return true;
        }

        return false;
    }

    /**
     * Revoke all tokens for a user (logout from all devices)
     *
     * @param userId - User ID
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        authRepository.findActiveTokensByUserId(userId).forEach(auth -> {
            auth.revoke();
            authRepository.save(auth);
        });
    }

    /**
     * Check if token is blacklisted (revoked)
     *
     * @param token - JWT token
     * @return true if token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        Optional<Auth> authOpt = authRepository.findByToken(token);

        if (authOpt.isEmpty()) {
            // Token not found in database - not blacklisted
            // This can happen if token was issued before we implemented token storage
            return false;
        }

        Auth auth = authOpt.get();
        return auth.getRevoked() || auth.isExpired();
    }

    /**
     * Check if token is valid (exists and not revoked)
     *
     * @param token - JWT token
     * @return true if token is valid
     */
    public boolean isTokenValid(String token) {
        return authRepository.isTokenValid(token);
    }

    /**
     * Clean up expired tokens (should be run periodically)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        authRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    /**
     * Get client IP address from request
     *
     * @param request - HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Get active session count for a user
     *
     * @param userId - User ID
     * @return Number of active sessions
     */
    public int getActiveSessionCount(Long userId) {
        return authRepository.findActiveTokensByUserId(userId).size();
    }
}

/**
 * HOW TOKEN BLACKLISTING WORKS:
 *
 * 1. USER LOGS IN:
 *    - Server generates JWT token
 *    - Token is stored in Auth table with revoked=false
 *    - Token is returned to client
 *
 * 2. USER MAKES REQUESTS:
 *    - Client sends token in Authorization header
 *    - JwtAuthenticationFilter validates token signature and expiration
 *    - TokenBlacklistService checks if token is revoked
 *    - If revoked, request is rejected
 *
 * 3. USER LOGS OUT:
 *    - Client sends logout request with token
 *    - TokenBlacklistService marks token as revoked=true
 *    - Token becomes invalid immediately
 *
 * 4. CLEANUP:
 *    - Expired tokens are periodically deleted from database
 *    - Keeps database size manageable
 *
 * BENEFITS:
 * - Immediate logout (token revoked instantly)
 * - Logout from all devices (revoke all user tokens)
 * - Session tracking (see who's logged in)
 * - Audit trail (track login history)
 *
 * DRAWBACKS:
 * - Database query on every request (performance impact)
 * - More complex than pure stateless JWT
 * - Database storage required
 *
 * ALTERNATIVES:
 * - Use short-lived tokens (5-15 minutes)
 * - Implement refresh tokens
 * - Use Redis for blacklist (faster than database)
 */

package com.smartinventory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Auth Entity - Stores authentication tokens (Optional)
 *
 * This is optional - typically JWT tokens are stateless and don't need storage.
 * But you can use this to track login sessions, blacklist tokens, etc.
 *
 * Note: In most Spring Boot apps, we DON'T store JWT tokens in DB.
 * They are stateless and self-contained.
 * I'm including this to match your Python model, but we might not use it.
 */
@Entity
@Table(name = "auth")
public class Auth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY AUTH TOKENS BELONG TO ONE USER
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The JWT token
    @Column(nullable = false, length = 1000)
    private String token;

    // Token type: "Bearer"
    @Column(name = "token_type", length = 20)
    private String tokenType = "Bearer";

    // When the token expires
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Is this token revoked/blacklisted?
    @Column(nullable = false)
    private Boolean revoked = false;

    // Device/client information (optional)
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ============================================
    // CONSTRUCTORS
    // ============================================

    public Auth() {
    }

    public Auth(User user, String token, LocalDateTime expiresAt) {
        this.user = user;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public Auth(User user, String token, LocalDateTime expiresAt,
                String userAgent, String ipAddress) {
        this.user = user;
        this.token = token;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ============================================
    // BUSINESS LOGIC METHODS
    // ============================================

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if token is valid (not expired and not revoked)
     */
    public boolean isValid() {
        return !isExpired() && !revoked;
    }

    /**
     * Revoke this token (blacklist it)
     */
    public void revoke() {
        this.revoked = true;
    }

    // ============================================
    // GETTERS AND SETTERS
    // ============================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Auth{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", tokenType='" + tokenType + '\'' +
                ", expiresAt=" + expiresAt +
                ", revoked=" + revoked +
                ", createdAt=" + createdAt +
                '}';
    }
}

/**
 * NOTE ABOUT JWT AND THIS CLASS:
 *
 * In traditional session-based auth, tokens are stored in DB.
 * In JWT-based auth (what we're using), tokens are usually NOT stored.
 *
 * However, you might want to store tokens if you need:
 * - Token blacklisting (logout feature)
 * - Session tracking (see who's logged in)
 * - Audit trail (track login history)
 * - Refresh tokens
 *
 * For this project, we'll implement JWT WITHOUT storing tokens in DB first.
 * We can add token storage later if needed.
 *
 * This class is here to match your Python model structure,
 * but we might not use it initially.
 */
package com.smartinventory.repository;

import com.smartinventory.model.Auth;
import com.smartinventory.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AuthRepository - Database access for Auth entity
 *
 * Note: This is optional for JWT-based auth.
 * We might not use this initially since JWTs are stateless.
 */
@Repository
public interface AuthRepository extends JpaRepository<Auth, Long> {

    /**
     * Find auth token by token string
     * Query: SELECT * FROM auth WHERE token = ?
     */
    Optional<Auth> findByToken(String token);

    /**
     * Find all tokens for a user
     * Query: SELECT * FROM auth WHERE user_id = ?
     */
    List<Auth> findByUser(User user);

    /**
     * Find all tokens for a user ID
     * Query: SELECT * FROM auth WHERE user_id = ?
     */
    List<Auth> findByUserId(Long userId);

    /**
     * Find active (non-revoked, non-expired) tokens for a user
     */
    @Query("SELECT a FROM Auth a WHERE a.user.id = :userId " +
            "AND a.revoked = false " +
            "AND a.expiresAt > CURRENT_TIMESTAMP")
    List<Auth> findActiveTokensByUserId(Long userId);

    /**
     * Find expired tokens
     */
    @Query("SELECT a FROM Auth a WHERE a.expiresAt < CURRENT_TIMESTAMP")
    List<Auth> findExpiredTokens();

    /**
     * Find revoked tokens
     * Query: SELECT * FROM auth WHERE revoked = true
     */
    List<Auth> findByRevokedTrue();

    /**
     * Check if token exists and is valid
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Auth a " +
            "WHERE a.token = :token " +
            "AND a.revoked = false " +
            "AND a.expiresAt > CURRENT_TIMESTAMP")
    boolean isTokenValid(String token);

    /**
     * Delete expired tokens (cleanup)
     */
    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    /**
     * Delete all tokens for a user (logout from all devices)
     */
    void deleteByUserId(Long userId);
}
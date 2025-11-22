package com.smartinventory.config;

import com.smartinventory.model.User;
import com.smartinventory.service.JwtService;
import com.smartinventory.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JwtAuthenticationFilter - Validates JWT tokens on each request
 *
 * This filter:
 * 1. Extracts JWT token from Authorization header
 * 2. Validates the token
 * 3. Loads user details from database
 * 4. Sets authentication in SecurityContext
 * 5. Passes request to next filter
 *
 * If token is invalid or missing, request continues but without authentication.
 * SecurityConfig will then reject unauthorized requests to protected endpoints.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    @Autowired
    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserService userService
    ) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Get Authorization header
        final String authHeader = request.getHeader("Authorization");

        // If no Authorization header or doesn't start with "Bearer ", skip this filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract token (remove "Bearer " prefix)
            final String jwt = authHeader.substring(7);

            // Extract username from token
            final String username = jwtService.extractUsername(jwt);

            // If username exists and no authentication is set yet
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load user from database
                User user = userService.getUserByUsername(username);

                // Validate token
                if (jwtService.isTokenValid(jwt, user)) {

                    // Create authentication token with user's role
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                    );

                    // Set authentication details (IP, session, etc.)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // If token validation fails, log and continue without authentication
            // SecurityConfig will reject the request if it's a protected endpoint
            logger.error("JWT authentication failed: " + e.getMessage());
        }

        // Continue to next filter
        filterChain.doFilter(request, response);
    }
}

/**
 * HOW THIS FILTER WORKS:
 *
 * 1. Request arrives: GET /api/products
 *    Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 *
 * 2. Filter extracts token from Authorization header
 *
 * 3. Token is validated:
 *    - Signature is valid (not tampered)
 *    - Not expired
 *    - User exists in database
 *
 * 4. If valid, user is loaded from database
 *
 * 5. Authentication object is created with user details and role
 *
 * 6. Authentication is stored in SecurityContext
 *
 * 7. Request continues to controller with authenticated user
 *
 * 8. Controller can access user via SecurityContextHolder.getContext().getAuthentication()
 *
 * USAGE IN CONTROLLERS:
 *
 * // Get authenticated user
 * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 * User user = (User) auth.getPrincipal();
 *
 * // Or use @AuthenticationPrincipal annotation
 * public ResponseEntity<?> getProfile(@AuthenticationPrincipal User user) {
 *     return ResponseEntity.ok(user);
 * }
 */

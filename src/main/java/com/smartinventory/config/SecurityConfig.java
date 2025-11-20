package com.smartinventory.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * SecurityConfig - Security and CORS configuration
 *
 * This configures:
 * 1. Password encryption (BCrypt)
 * 2. CORS (Cross-Origin Resource Sharing)
 * 3. JWT authentication filter
 * 4. Endpoint authorization rules
 * 5. Role-based access control
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    public SecurityConfig(@Lazy JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Password Encoder Bean
     * Used to hash passwords before storing in database
     *
     * BCrypt is a strong hashing algorithm that:
     * - Adds salt automatically
     * - Is slow (protects against brute force)
     * - Is one-way (can't decrypt)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security Filter Chain
     * Configures HTTP security rules with JWT authentication
     *
     * Public endpoints:
     * - /api/auth/register - User registration
     * - /api/auth/login - User login
     * - /FrontEnd/** - Frontend static files
     *
     * Protected endpoints:
     * - All other /api/** endpoints require authentication
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (we're using JWT, not sessions)
                .csrf(csrf -> csrf.disable())

                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Make sessions stateless (no server-side sessions)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Add JWT authentication filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // Configure endpoint authorization
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                        // Frontend static files - allow access
                        .requestMatchers("/FrontEnd/**", "/", "/index.html").permitAll()

                        // All other API endpoints require authentication
                        .requestMatchers("/api/**").authenticated()

                        // All other requests are allowed
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * CORS Configuration
     * Allows frontend to call the API from different domain/port
     *
     * This allows:
     * - http://localhost:5500 (Live Server)
     * - http://127.0.0.1:5500
     * - Any other origin you specify
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow these origins (frontend URLs)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5500",
                "http://127.0.0.1:5500",
                "http://localhost:8000",
                "http://127.0.0.1:8000",
                "http://localhost:3000"  // In case you use React later
        ));

        // Allow all HTTP methods (GET, POST, PUT, DELETE, etc.)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // How long the browser can cache the CORS response
        configuration.setMaxAge(3600L);

        // Apply CORS to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}

/**
 * EXPLANATION:
 *
 * 1. @Configuration - Tells Spring this class contains configuration
 * 2. @EnableWebSecurity - Enables Spring Security
 * 3. @EnableMethodSecurity - Enables @PreAuthorize, @Secured, etc. annotations
 * 4. @Bean - Methods that return objects Spring should manage
 *
 * PASSWORD ENCODER:
 * - Used by AuthService to hash passwords
 * - authService.register() → passwordEncoder.encode(password)
 *
 * SECURITY FILTER CHAIN:
 * - .csrf().disable() - We don't need CSRF with JWT
 * - .sessionManagement().sessionCreationPolicy(STATELESS) - No sessions
 * - .addFilterBefore() - Add JWT filter before Spring's authentication filter
 * - .authorizeHttpRequests() - Configure which endpoints require authentication
 *   * /api/auth/register, /api/auth/login - Public (no auth needed)
 *   * /api/** - Protected (requires JWT token)
 *
 * JWT AUTHENTICATION FLOW:
 * 1. User logs in via /api/auth/login
 * 2. Server returns JWT token
 * 3. Frontend stores token and includes it in every request
 * 4. JwtAuthenticationFilter validates token
 * 5. If valid, user is authenticated and request proceeds
 * 6. If invalid, request is rejected with 401 Unauthorized
 *
 * CORS:
 * - Allows frontend (running on port 5500) to call backend (port 5001)
 * - Without this, browser blocks requests (CORS error)
 */
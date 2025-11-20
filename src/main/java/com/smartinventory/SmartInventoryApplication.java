package com.smartinventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Main Application Class
 *
 * This is the entry point of the Spring Boot application.
 * The @SpringBootApplication annotation does 3 things:
 * 1. @Configuration - Tells Spring this class has configuration
 * 2. @EnableAutoConfiguration - Auto-configures Spring based on dependencies
 * 3. @ComponentScan - Scans for components in this package and sub-packages
 */
@SpringBootApplication
public class SmartInventoryApplication {

    /**
     * Main method - where Java starts the application
     *
     * @param args - Command line arguments (not used)
     */
    public static void main(String[] args) {
        // This single line starts the entire Spring Boot application
        // It:
        // - Starts the embedded Tomcat web server
        // - Connects to the database
        // - Sets up all REST endpoints
        // - Configures security
        SpringApplication.run(SmartInventoryApplication.class, args);
    }

    /**
     * Component that prints the startup message with the actual port
     */
    @Component
    public static class StartupListener {

        @Autowired
        private Environment environment;

        @EventListener(ApplicationReadyEvent.class)
        public void onApplicationReady() {
            String port = environment.getProperty("local.server.port");
            System.out.println("\n========================================");
            System.out.println("SmartInventory API is running!");
            System.out.println("Server: http://localhost:" + port);
            System.out.println("API Base: http://localhost:" + port + "/api");
            System.out.println("========================================\n");
        }
    }
}

/**
 * EXPLANATION:
 *
 * When you run this file, Spring Boot:
 * 1. Reads application.properties
 * 2. Connects to the SQLite database
 * 3. Scans all @Component, @Service, @Controller classes
 * 4. Sets up all @GetMapping, @PostMapping endpoints
 * 5. Starts the web server on port 5001
 *
 * That's it! Just run this file and your API is live.
 */
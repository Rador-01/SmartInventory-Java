package com.smartinventory.controller;

import com.smartinventory.model.Client;
import com.smartinventory.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClientController - Handles client/customer endpoints
 *
 * Endpoints:
 * GET    /api/clients          - Get all clients
 * GET    /api/clients/{id}     - Get client by ID
 * POST   /api/clients          - Create new client
 * PUT    /api/clients/{id}     - Update client
 * DELETE /api/clients/{id}     - Delete client
 * GET    /api/clients/search?q={term} - Search clients
 * GET    /api/clients/top      - Get top clients by purchases
 */
@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*")
public class ClientController {

    private final ClientService clientService;

    @Autowired
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<List<Client>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getClientById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(clientService.getClientById(id));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * POST /api/clients
     * Create new client
     *
     * Request Body:
     * {
     *   "name": "John Doe",
     *   "email": "john@example.com",
     *   "phone": "123-456-7890",
     *   "address": "123 Main St",
     *   "company": "ABC Corp"
     * }
     */
    @PostMapping
    public ResponseEntity<?> createClient(@RequestBody Client client) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(clientService.createClient(client));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateClient(@PathVariable Long id, @RequestBody Client client) {
        try {
            return ResponseEntity.ok(clientService.updateClient(id, client));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClient(@PathVariable Long id) {
        try {
            clientService.deleteClient(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Client deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Client>> searchClients(@RequestParam String q) {
        return ResponseEntity.ok(clientService.searchClients(q));
    }

    @GetMapping("/top")
    public ResponseEntity<List<Client>> getTopClients() {
        return ResponseEntity.ok(clientService.getTopClients());
    }
}
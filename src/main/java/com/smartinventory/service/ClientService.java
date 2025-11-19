package com.smartinventory.service;

import com.smartinventory.model.Client;
import com.smartinventory.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    @Autowired
    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));
    }

    public Client createClient(Client client) {
        if (client.getEmail() != null && clientRepository.existsByEmail(client.getEmail())) {
            throw new RuntimeException("Client with email " + client.getEmail() + " already exists");
        }
        return clientRepository.save(client);
    }

    public Client updateClient(Long id, Client clientDetails) {
        Client client = getClientById(id);

        if (clientDetails.getName() != null) {
            client.setName(clientDetails.getName());
        }

        if (clientDetails.getEmail() != null && !clientDetails.getEmail().equals(client.getEmail())) {
            if (clientRepository.existsByEmail(clientDetails.getEmail())) {
                throw new RuntimeException("Client with email " + clientDetails.getEmail() + " already exists");
            }
            client.setEmail(clientDetails.getEmail());
        }

        if (clientDetails.getPhone() != null) {
            client.setPhone(clientDetails.getPhone());
        }

        if (clientDetails.getAddress() != null) {
            client.setAddress(clientDetails.getAddress());
        }

        if (clientDetails.getCompany() != null) {
            client.setCompany(clientDetails.getCompany());
        }

        return clientRepository.save(client);
    }

    public void deleteClient(Long id) {
        Client client = getClientById(id);
        clientRepository.delete(client);
    }

    public List<Client> searchClients(String searchTerm) {
        return clientRepository.searchClients(searchTerm);
    }

    public List<Client> getTopClients() {
        return clientRepository.findTopClientsByPurchases();
    }
}
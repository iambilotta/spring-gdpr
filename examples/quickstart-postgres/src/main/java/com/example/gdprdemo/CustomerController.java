package com.example.gdprdemo;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerRepository repository;

    public CustomerController(CustomerRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public Customer create(@RequestBody CreateCustomerRequest request) {
        Customer customer = new Customer();
        customer.setId(request.id() != null ? request.id() : UUID.randomUUID().toString());
        customer.setFullName(request.fullName());
        customer.setEmail(request.email());
        customer.setTaxId(request.taxId());
        customer.setHealthCondition(request.healthCondition());
        customer.setCreatedAt(Instant.now());
        return repository.save(customer);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> findOne(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public record CreateCustomerRequest(
            String id,
            String fullName,
            String email,
            String taxId,
            String healthCondition) {
    }
}

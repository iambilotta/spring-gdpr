package com.iambilotta.gdpr.demo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.iambilotta.gdpr.annotations.GdprPersonalData;

/**
 * In-memory repository, intentionally trivial. The advisor fires on
 * {@code @GdprPersonalData}-annotated methods.
 */
@Repository
public class CustomerRepository {

    private final Map<String, Customer> store = new LinkedHashMap<>();

    public Customer save(Customer customer) {
        store.put(customer.getId(), customer);
        return customer;
    }

    @GdprPersonalData(description = "fetch full customer profile by subject id")
    public Optional<Customer> findBySubjectId(String subjectId) {
        return Optional.ofNullable(store.get(subjectId));
    }

    public int eraseBySubjectId(String subjectId) {
        return store.remove(subjectId) != null ? 1 : 0;
    }

    public int size() {
        return store.size();
    }
}

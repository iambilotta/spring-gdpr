package com.iambilotta.gdpr.demo;

import org.springframework.stereotype.Component;

import com.iambilotta.gdpr.annotations.GdprErasable;
import com.iambilotta.gdpr.starter.erasure.ErasureHandler;

@Component
public class CustomerErasureHandler implements ErasureHandler {

    private final CustomerRepository repository;

    public CustomerErasureHandler(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public String entityType() {
        return Customer.class.getName();
    }

    @Override
    public GdprErasable.Strategy strategy() {
        return GdprErasable.Strategy.DELETE;
    }

    @Override
    public int erase(String subjectId) {
        return repository.eraseBySubjectId(subjectId);
    }
}

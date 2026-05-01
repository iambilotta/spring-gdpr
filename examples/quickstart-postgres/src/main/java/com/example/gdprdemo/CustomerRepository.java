package com.example.gdprdemo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iambilotta.gdpr.annotations.GdprPersonalData;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    @GdprPersonalData(description = "fetch full customer profile by subject id")
    Optional<Customer> findById(String subjectId);
}

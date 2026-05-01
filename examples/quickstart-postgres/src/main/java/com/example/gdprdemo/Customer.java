package com.example.gdprdemo;

import java.time.Instant;

import com.iambilotta.gdpr.annotations.GdprDataSubjects;
import com.iambilotta.gdpr.annotations.GdprErasable;
import com.iambilotta.gdpr.annotations.GdprLegalBasis;
import com.iambilotta.gdpr.annotations.GdprPersonalData;
import com.iambilotta.gdpr.annotations.GdprRetention;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
@GdprDataSubjects(categories = {"customer"})
@GdprLegalBasis(
        value = GdprLegalBasis.LawfulBasis.CONTRACT,
        article = "6(1)(b)",
        note = "performance of sales contract",
        specialBasis = GdprLegalBasis.Art9Condition.EXPLICIT_CONSENT)
@GdprRetention(period = "P5Y", strategy = GdprRetention.Strategy.ANONYMIZE, createdAtField = "createdAt")
@GdprErasable(strategy = GdprErasable.Strategy.DELETE, subjectIdField = "id")
public class Customer {

    @Id
    private String id;

    @GdprPersonalData(description = "full legal name")
    @Column(name = "full_name")
    private String fullName;

    @GdprPersonalData(description = "primary email")
    private String email;

    @GdprPersonalData(description = "national tax id")
    @Column(name = "tax_id")
    private String taxId;

    @GdprPersonalData(description = "self-declared health condition", specialCategory = true)
    @Column(name = "health_condition")
    private String healthCondition;

    @Column(name = "created_at")
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getHealthCondition() { return healthCondition; }
    public void setHealthCondition(String healthCondition) { this.healthCondition = healthCondition; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

package com.iambilotta.gdpr.demo;

import java.time.Instant;

import com.iambilotta.gdpr.annotations.GdprDataSubjects;
import com.iambilotta.gdpr.annotations.GdprErasable;
import com.iambilotta.gdpr.annotations.GdprLegalBasis;
import com.iambilotta.gdpr.annotations.GdprPersonalData;
import com.iambilotta.gdpr.annotations.GdprRetention;

/**
 * Demo entity. Drives the integration test that asserts the annotation-driven pipeline:
 * audit advisor fires, retention scheduler picks the row up, DPIA + ROPA artifacts list it.
 */
@GdprDataSubjects(categories = {"customer"})
@GdprLegalBasis(value = GdprLegalBasis.LawfulBasis.CONTRACT, article = "6(1)(b)", note = "performance of sales contract")
@GdprRetention(period = "P5Y", strategy = GdprRetention.Strategy.ANONYMIZE, createdAtField = "createdAt")
@GdprErasable(strategy = GdprErasable.Strategy.DELETE, subjectIdField = "id")
public class Customer {

    private String id;

    @GdprPersonalData(description = "full legal name")
    private String fullName;

    @GdprPersonalData(description = "primary email", specialCategory = false)
    private String email;

    @GdprPersonalData(description = "national tax id", specialCategory = true)
    private String taxId;

    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

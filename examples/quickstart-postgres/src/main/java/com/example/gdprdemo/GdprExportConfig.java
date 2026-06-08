package com.example.gdprdemo;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.iambilotta.gdpr.starter.access.SubjectDataProvider;

/**
 * Wires the Article 15 right-of-access export for the demo. The starter ships the
 * {@code GET /gdpr/access/export} endpoint and the assembly machinery, but only the application
 * knows where a subject's data lives, so it registers one {@link SubjectDataProvider} per source.
 *
 * <p>Here the source is the {@code Customer} JPA entity itself: its {@code @GdprPersonalData} fields
 * (full name, email, tax id, health condition) become the classified rows of the dossier. Returning
 * an empty list when the subject is unknown is the honesty contract: the export lists exactly what
 * the providers return, never more.
 */
@Configuration
public class GdprExportConfig {

    @Bean
    SubjectDataProvider customerDataProvider(CustomerRepository customers) {
        return subjectId -> customers.findById(subjectId)
                .map(List::<Object>of)
                .orElseGet(List::of);
    }
}

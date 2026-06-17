package com.iambilotta.gdpr.starter.erasure;

import java.time.Instant;
import java.util.Map;

import org.springframework.context.ApplicationEvent;

/**
 * Published by {@link ErasureService} once a subject's right-to-erasure (Art. 17) has been honoured
 * by every {@link ErasureHandler} (issue #37). The Spring-native counterpart of the
 * {@link ErasureListener} SPI: an adopter can react with {@code @EventListener(SubjectErasedEvent.class)}
 * instead of implementing the SPI, the typical hook for an event-sourced / CQRS consumer to rebuild a
 * projection that resolved a now-dangling forgettable-payload reference (ADR-0010).
 *
 * <p>The event source is the {@code subjectId}; {@link #affectedByType()} mirrors the
 * {@link ErasureReport} (entity FQN to affected count) and {@link #erasedAt()} is when the report was
 * assembled. Published only when an {@code ApplicationEventPublisher} is available; the SPI path needs
 * no Spring context.
 */
public class SubjectErasedEvent extends ApplicationEvent {

    private final Map<String, Integer> affectedByType;
    private final Instant erasedAt;

    public SubjectErasedEvent(String subjectId, Map<String, Integer> affectedByType, Instant erasedAt) {
        super(subjectId);
        this.affectedByType = Map.copyOf(affectedByType);
        this.erasedAt = erasedAt;
    }

    /** The erased subject (also the {@link #getSource() event source}). */
    public String subjectId() {
        return (String) getSource();
    }

    /** Per-type affected counts, mirroring the {@link ErasureReport}. */
    public Map<String, Integer> affectedByType() {
        return affectedByType;
    }

    /** When the erasure report was assembled. */
    public Instant erasedAt() {
        return erasedAt;
    }
}

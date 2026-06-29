package com.iambilotta.gdpr.starter.audit;

import java.time.Instant;
import java.util.List;

/**
 * Persistence boundary for audit access records. Implementations: SLF4J (default),
 * JDBC (opt-in), user-supplied bean (replaces both).
 */
public interface AuditSink {

    void write(AuditAccessRecord record);

    /**
     * Bring the sink's backing store to a usable state (create or verify the schema). Called once by the
     * autoconfig AFTER the application has started, never during bean construction, so a sink stays
     * constructible without a live backing store (CDS/AOT training, native build, tests, pre-DB boot).
     * Default: no-op (sinks with no schema, e.g. SLF4J).
     */
    default void initializeSchema() {
        // no-op by default
    }

    /**
     * Read records for the right-of-access endpoint (Art. 15). Implementations that cannot
     * query (e.g. SLF4J-only) should throw {@link UnsupportedOperationException}.
     */
    List<AuditAccessRecord> findBySubject(String subjectId, Instant from, Instant to);
}

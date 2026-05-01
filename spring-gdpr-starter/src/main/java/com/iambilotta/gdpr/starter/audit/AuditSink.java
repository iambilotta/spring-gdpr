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
     * Read records for the right-of-access endpoint (Art. 15). Implementations that cannot
     * query (e.g. SLF4J-only) should throw {@link UnsupportedOperationException}.
     */
    List<AuditAccessRecord> findBySubject(String subjectId, Instant from, Instant to);
}

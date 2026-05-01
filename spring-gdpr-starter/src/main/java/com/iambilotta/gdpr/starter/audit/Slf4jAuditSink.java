package com.iambilotta.gdpr.starter.audit;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default sink: emits one structured log line per access event. Cheap to enable, sufficient
 * for log-aggregation backends (ELK, Loki, Datadog) that already retain audit-grade logs.
 *
 * <p>Cannot satisfy the right-of-access endpoint by itself: {@link #findBySubject} throws.
 * Pair with a JDBC sink or external query layer for Art. 15 access exports.
 */
public class Slf4jAuditSink implements AuditSink {

    private static final Logger LOG = LoggerFactory.getLogger("gdpr.audit");

    @Override
    public void write(AuditAccessRecord record) {
        LOG.info(
                "gdpr_access event={} at={} actor={} subject={} type={} member={} basis={} special={}",
                record.eventId(),
                record.at(),
                record.actor(),
                record.subjectId(),
                record.targetType(),
                record.targetMember(),
                record.legalBasis(),
                record.specialCategory()
        );
    }

    @Override
    public List<AuditAccessRecord> findBySubject(String subjectId, Instant from, Instant to) {
        throw new UnsupportedOperationException(
                "Slf4jAuditSink does not support querying. Configure spring.gdpr.audit.jdbc-enabled=true "
                        + "or supply a custom AuditSink bean to enable Art. 15 right-of-access export.");
    }
}

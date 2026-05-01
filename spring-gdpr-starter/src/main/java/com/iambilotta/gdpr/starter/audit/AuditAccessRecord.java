package com.iambilotta.gdpr.starter.audit;

import java.time.Instant;

/**
 * Immutable record of one personal-data access event. Persisted to the audit log
 * (Art. 5(2) accountability + Art. 30(1)(g) security measures evidence).
 *
 * @param eventId           UUID assigned at capture
 * @param at                timestamp of access
 * @param actor             principal who triggered access (Spring Security {@code Authentication#getName()} or "system")
 * @param subjectId         data subject id touched, when resolvable from the call site (may be {@code null})
 * @param targetType        FQN of the type carrying personal data
 * @param targetMember      method or field name accessed
 * @param legalBasis        article reference (e.g. "6(1)(b)") if declared on the call site
 * @param specialCategory   true when Art. 9 / Art. 10 data is involved
 */
public record AuditAccessRecord(
        String eventId,
        Instant at,
        String actor,
        String subjectId,
        String targetType,
        String targetMember,
        String legalBasis,
        boolean specialCategory
) {
}

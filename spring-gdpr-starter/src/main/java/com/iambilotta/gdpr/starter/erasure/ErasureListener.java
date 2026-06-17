package com.iambilotta.gdpr.starter.erasure;

/**
 * Post-erasure extension point (issue #37). Implement and register as a Spring bean to react after a
 * subject's right-to-erasure (Art. 17) has been honoured by every {@link ErasureHandler}.
 *
 * <p>The motivating case is the forgettable-payload pattern (ADR-0010): once
 * {@code ErasureService.eraseSubject} runs, a reference the read side still holds (a display name on
 * a projection, a cached value) now resolves to nothing. A listener is the deterministic signal to
 * rebuild that projection or invalidate that cache, instead of every adopter inventing their own
 * polling. It pairs with crypto-shredding (ADR-0009) identically: a dropped key makes the inline
 * ciphertext unreadable, and the read side must refresh.
 *
 * <p>Invoked <strong>once per successful</strong> {@code eraseSubject}, with the assembled
 * {@link ErasureReport}, <strong>after</strong> all handlers have committed. Zero listeners is a
 * no-op (backward compatible); several listeners all run.
 *
 * <p><strong>Failure semantics.</strong> The erasure already happened by the time a listener runs
 * (the handlers committed). A listener that throws never un-erases the subject. The service still
 * invokes the remaining listeners, then surfaces the failure as an {@link ErasureListenerException}
 * (logged, not swallowed), so a broken downstream rebuild is visible rather than silent. Make a
 * listener idempotent: a rebuild may be retried.
 *
 * <p>An adopter who prefers Spring's eventing can instead listen for {@link SubjectErasedEvent} via
 * {@code @EventListener}; the service publishes that event on the same boundary when an
 * {@code ApplicationEventPublisher} is available (the starter wires one).
 */
@FunctionalInterface
public interface ErasureListener {

    /**
     * React to a completed erasure. Called once per successful {@code eraseSubject}, after every
     * handler committed, with the per-type affected counts.
     */
    void onSubjectErased(ErasureReport report);
}

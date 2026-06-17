package com.iambilotta.gdpr.starter.autoconfig.declfixture.forgettable;

import com.iambilotta.gdpr.annotations.GdprErasable;

/**
 * Test fixture for declarative forgettable-payload erasure (issue #36). Isolated in its own package
 * so a scan rooted here finds only the FORGETTABLE type. {@code order = 30} pins that the declared
 * order flows to the auto-wired handler.
 */
@GdprErasable(strategy = GdprErasable.Strategy.FORGETTABLE, order = 30)
public record ForgettableEntity(String subjectId) {
}

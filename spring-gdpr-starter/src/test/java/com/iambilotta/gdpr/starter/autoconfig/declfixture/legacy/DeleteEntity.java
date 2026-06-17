package com.iambilotta.gdpr.starter.autoconfig.declfixture.legacy;

import com.iambilotta.gdpr.annotations.GdprErasable;

/**
 * Test fixture for the legacy {@code DELETE} strategy (issue #36). Isolated in its own package so a
 * scan rooted here finds only this type and proves the scanner does NOT auto-wire a library handler
 * for DELETE / ANONYMIZE / PSEUDONYMIZE (those stay the adopter's own handler, ADR-0004).
 */
@GdprErasable(strategy = GdprErasable.Strategy.DELETE)
public record DeleteEntity(String subjectId) {
}

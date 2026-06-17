package com.iambilotta.gdpr.starter.autoconfig.declfixture.crypto;

import com.iambilotta.gdpr.annotations.GdprErasable;

/**
 * Test fixture for declarative crypto-shredding (issue #36). Isolated in its own package so a scan
 * rooted here finds only the CRYPTO_SHRED type, never the forgettable / legacy fixtures.
 */
@GdprErasable(strategy = GdprErasable.Strategy.CRYPTO_SHRED)
public record CryptoShreddedEntity(String subjectId) {
}

package com.iambilotta.gdpr.starter.erasure.forgettable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link ForgettablePayloadStore}, for tests and for the in-test "external PII vault"
 * harness that the forgettable-payload spec exercises. NOT for production: values live only in the
 * heap and vanish on restart. Use {@link JdbcForgettablePayloadStore} or a custom SPI backed by a
 * real mutable store in a deployment.
 *
 * <p>Records a tombstone on {@link #erase(String)} so a later {@link #put} for the same subject is
 * refused, mirroring {@code InMemorySubjectKeyStore}'s no-resurrection guarantee (ADR-0010).
 */
public final class InMemoryForgettablePayloadStore implements ForgettablePayloadStore {

    private final Map<String, Map<String, String>> values = new ConcurrentHashMap<>();
    private final Set<String> erased = ConcurrentHashMap.newKeySet();

    @Override
    public void put(String subjectId, String fieldKey, String value) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be provided");
        }
        if (fieldKey == null || fieldKey.isBlank()) {
            throw new IllegalArgumentException("fieldKey must be provided");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (erased.contains(subjectId)) {
            throw new IllegalStateException(
                    "subject " + subjectId + " was erased; writing a payload would un-erase them");
        }
        values.computeIfAbsent(subjectId, id -> new ConcurrentHashMap<>()).put(fieldKey, value);
    }

    @Override
    public Optional<String> resolve(String subjectId, String fieldKey) {
        if (subjectId == null || fieldKey == null) {
            return Optional.empty();
        }
        Map<String, String> forSubject = values.get(subjectId);
        return forSubject == null ? Optional.empty() : Optional.ofNullable(forSubject.get(fieldKey));
    }

    @Override
    public int erase(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be provided");
        }
        erased.add(subjectId);
        Map<String, String> removed = values.remove(subjectId);
        return removed == null ? 0 : removed.size();
    }
}

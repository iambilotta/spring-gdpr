package com.iambilotta.gdpr.starter.erasure.crypto;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link SubjectKeyStore}, for tests and for the in-test "event store" harness that the
 * crypto-shredding spec exercises. NOT for production: keys live only in the heap and vanish on
 * restart (an involuntary erasure of everyone). Use {@link JdbcSubjectKeyStore} or a KMS-backed
 * SPI in a real deployment.
 *
 * <p>Records a tombstone on {@link #drop(String)} so a later {@link #getOrCreate(String)} for the
 * same subject cannot silently mint a fresh key and un-erase them (ADR-0009 key-loss /
 * involuntary-erasure constraint).
 */
public final class InMemorySubjectKeyStore implements SubjectKeyStore {

    private final ConcurrentHashMap<String, byte[]> keys = new ConcurrentHashMap<>();
    private final Set<String> dropped = ConcurrentHashMap.newKeySet();
    private final SubjectKeyFactory keyFactory;

    public InMemorySubjectKeyStore() {
        this.keyFactory = new SubjectKeyFactory();
    }

    @Override
    public Optional<byte[]> keyFor(String subjectId) {
        byte[] key = keys.get(subjectId);
        return key == null ? Optional.empty() : Optional.of(key.clone());
    }

    @Override
    public byte[] getOrCreate(String subjectId) {
        if (dropped.contains(subjectId)) {
            throw new IllegalStateException(
                    "subject " + subjectId + " was erased (key dropped); minting a new key would un-erase them");
        }
        return keys.computeIfAbsent(subjectId, id -> keyFactory.newKey()).clone();
    }

    @Override
    public void drop(String subjectId) {
        dropped.add(subjectId);
        byte[] removed = keys.remove(subjectId);
        if (removed != null) {
            Arrays.fill(removed, (byte) 0);
        }
    }

    @Override
    public boolean exists(String subjectId) {
        return keys.containsKey(subjectId);
    }
}

package com.iambilotta.gdpr.starter.erasure.crypto;

import java.security.SecureRandom;

/**
 * Mints fresh 256-bit per-subject keys. Extracted so every {@link SubjectKeyStore} mints keys the
 * same way (AES-256) and so tests can inject a deterministic {@link SecureRandom} if needed.
 */
final class SubjectKeyFactory {

    /** AES-256: a 32-byte key. AES-256-GCM is the cipher in {@link AesGcmCryptoShredder}. */
    static final int KEY_LENGTH_BYTES = 32;

    private final SecureRandom secureRandom;

    SubjectKeyFactory() {
        this(new SecureRandom());
    }

    SubjectKeyFactory(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    byte[] newKey() {
        byte[] key = new byte[KEY_LENGTH_BYTES];
        secureRandom.nextBytes(key);
        return key;
    }
}

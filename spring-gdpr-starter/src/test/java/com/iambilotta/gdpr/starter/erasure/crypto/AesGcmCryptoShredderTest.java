package com.iambilotta.gdpr.starter.erasure.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Crypto-level guarantees of {@link AesGcmCryptoShredder}: authenticated, fail-closed, unique IV.
 */
class AesGcmCryptoShredderTest {

    private final SubjectKeyStore keys = new InMemorySubjectKeyStore();
    private final AesGcmCryptoShredder shredder = new AesGcmCryptoShredder(keys);

    /**
     * @spec.given a plaintext encrypted twice under the same subject key
     * @spec.when  the two ciphertexts are compared
     * @spec.then  they differ (fresh random IV per record) yet both decrypt to the plaintext
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-016
     */
    @Test
    void usesAFreshIvPerRecordSoCiphertextsDiffer() {
        byte[] c1 = shredder.encrypt("alice-1", "Alice");
        byte[] c2 = shredder.encrypt("alice-1", "Alice");

        assertThat(c1).isNotEqualTo(c2);
        assertThat(shredder.decrypt("alice-1", c1)).contains("Alice");
        assertThat(shredder.decrypt("alice-1", c2)).contains("Alice");
    }

    /**
     * @spec.given a valid ciphertext
     * @spec.when  a single byte of it is flipped (tampering) and decryption is attempted
     * @spec.then  decryption fails closed (empty), never a partial plaintext (GCM authentication)
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-016
     */
    @Test
    void failsClosedOnTamperedCiphertext() {
        byte[] ciphertext = shredder.encrypt("alice-1", "Alice");
        ciphertext[ciphertext.length - 1] ^= 0x01;

        assertThat(shredder.decrypt("alice-1", ciphertext)).isEmpty();
    }

    /**
     * @spec.given a ciphertext sealed under one subject's key
     * @spec.when  it is decrypted under a different subject's key
     * @spec.then  decryption fails closed (empty): keys are not interchangeable across subjects
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-016
     */
    @Test
    void cannotDecryptUnderAnotherSubjectsKey() {
        byte[] alice = shredder.encrypt("alice-1", "Alice");
        keys.getOrCreate("bob-2");

        assertThat(shredder.decrypt("bob-2", alice)).isEmpty();
    }

    /**
     * @spec.given garbage bytes that are not a valid ciphertext
     * @spec.when  decryption is attempted
     * @spec.then  it returns empty instead of throwing (fail closed on malformed input)
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-016
     */
    @Test
    void failsClosedOnMalformedInput() {
        keys.getOrCreate("alice-1");

        assertThat(shredder.decrypt("alice-1", new byte[] {9, 9, 9})).isEmpty();
        assertThat(shredder.decrypt("alice-1", new byte[0])).isEmpty();
    }
}

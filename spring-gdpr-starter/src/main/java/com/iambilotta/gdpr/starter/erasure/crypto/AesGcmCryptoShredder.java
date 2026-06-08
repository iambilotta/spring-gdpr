package com.iambilotta.gdpr.starter.erasure.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM implementation of {@link CryptoShredder}.
 *
 * <p><strong>Crypto choices (ADR-0009).</strong>
 * <ul>
 *   <li><strong>Cipher:</strong> AES-256 in GCM (authenticated encryption, AEAD). Confidentiality
 *       + integrity in one primitive; a tampered ciphertext fails the tag and decrypts to empty.</li>
 *   <li><strong>IV:</strong> a fresh 12-byte (96-bit) IV per record from {@link SecureRandom},
 *       the size NIST SP 800-38D recommends for GCM. Never reused for a given key (reuse would be
 *       catastrophic for GCM). The IV is not secret: it is prepended to the ciphertext.</li>
 *   <li><strong>Tag:</strong> 128-bit GCM authentication tag (the maximum), appended by the cipher.</li>
 *   <li><strong>Wire format:</strong> {@code [1-byte version][12-byte IV][ciphertext||16-byte tag]}.
 *       The version byte lets the format evolve without decrypting old records.</li>
 *   <li><strong>Key:</strong> the per-subject 256-bit key comes from the {@link SubjectKeyStore};
 *       dropping that key is the erasure. Keys are never logged.</li>
 * </ul>
 *
 * <p><strong>Fail-closed.</strong> Any decryption error (missing key, wrong length, failed GCM
 * tag, bad version) returns {@link Optional#empty()}. We never throw on decrypt and never log the
 * plaintext, the key, or the failure cause: an erased subject and a corrupt ciphertext are
 * indistinguishable from the caller's side, and that is the safe default.
 */
public final class AesGcmCryptoShredder implements CryptoShredder {

    private static final byte FORMAT_VERSION = 1;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int HEADER_LENGTH = 1 + IV_LENGTH_BYTES;

    private final SubjectKeyStore keyStore;
    private final SecureRandom secureRandom;

    public AesGcmCryptoShredder(SubjectKeyStore keyStore) {
        this(keyStore, new SecureRandom());
    }

    AesGcmCryptoShredder(SubjectKeyStore keyStore, SecureRandom secureRandom) {
        this.keyStore = keyStore;
        this.secureRandom = secureRandom;
    }

    @Override
    public byte[] encrypt(String subjectId, String plaintext) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be provided");
        }
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext must not be null");
        }
        byte[] key = keyStore.getOrCreate(subjectId);
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, KEY_ALGORITHM),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] sealed = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(HEADER_LENGTH + sealed.length)
                    .put(FORMAT_VERSION)
                    .put(iv)
                    .put(sealed)
                    .array();
        } catch (Exception ex) {
            // Encryption failures are programming/config errors (bad key length, missing provider),
            // not subject input: surface them, but without echoing the plaintext.
            throw new IllegalStateException("crypto-shredding encryption failed", ex);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    @Override
    public Optional<String> decrypt(String subjectId, byte[] ciphertext) {
        if (subjectId == null || ciphertext == null || ciphertext.length <= HEADER_LENGTH) {
            return Optional.empty();
        }
        if (ciphertext[0] != FORMAT_VERSION) {
            return Optional.empty();
        }
        Optional<byte[]> maybeKey = keyStore.keyFor(subjectId);
        if (maybeKey.isEmpty()) {
            // The key was dropped (erased) or never existed: the ciphertext is unrecoverable.
            return Optional.empty();
        }
        byte[] key = maybeKey.get();
        try {
            byte[] iv = Arrays.copyOfRange(ciphertext, 1, HEADER_LENGTH);
            byte[] sealed = Arrays.copyOfRange(ciphertext, HEADER_LENGTH, ciphertext.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, KEY_ALGORITHM),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(sealed);
            return Optional.of(new String(plaintext, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            // Fail closed: failed GCM tag, wrong key, tampering. Never leak the cause or a partial.
            return Optional.empty();
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }
}

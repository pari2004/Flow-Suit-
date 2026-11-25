package com.flowsuite.api.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generates opaque secrets (refresh / reset / invitation tokens) and hashes them for
 * storage. Only the SHA-256 hash is ever persisted; the raw value is returned to the
 * client exactly once. SHA-256 (not BCrypt) is used here because these tokens are
 * already high-entropy random values — the hash only needs to be fast and one-way, and
 * must be deterministic so it can be looked up by hash.
 */
public final class TokenHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private TokenHasher() {
    }

    /** A new 256-bit URL-safe random token (the raw value handed to the client). */
    public static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    /** Deterministic SHA-256 hex hash of a raw token, for storage and lookup. */
    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

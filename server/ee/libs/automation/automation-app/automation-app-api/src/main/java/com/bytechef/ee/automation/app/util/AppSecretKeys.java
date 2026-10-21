/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.util;

import com.bytechef.tenant.domain.TenantKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Generates and hashes app secret keys. The key is a {@link TenantKey} so the tenant can be read from it before any
 * lookup; only its SHA-256 is stored. Unsalted on purpose: the key carries 24 bytes of CSPRNG entropy and the equality
 * lookup needs a deterministic hash.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AppSecretKeys {

    private static final int HINT_LENGTH = 8;

    private AppSecretKeys() {
    }

    public static GeneratedKey generate() {
        String key = String.valueOf(TenantKey.of());

        return new GeneratedKey(key, hash(key), hint(key));
    }

    public static String hash(String key) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of()
                .formatHex(messageDigest.digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException("SHA-256 is not available", noSuchAlgorithmException);
        }
    }

    public static String hint(String key) {
        String stripped = key.replaceAll("=+$", "");

        if (stripped.length() <= HINT_LENGTH) {
            return stripped;
        }

        return stripped.substring(stripped.length() - HINT_LENGTH);
    }

    public record GeneratedKey(String key, String hash, String hint) {

        @Override
        public String toString() {
            return "GeneratedKey[key=[REDACTED], hash=" + hash + ", hint=" + hint + "]";
        }
    }
}

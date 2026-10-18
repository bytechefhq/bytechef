/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * The bearer token an identity provider uses to call the SCIM API: {@code bcscim_{tenantId}_{secret}}. The tenant is
 * part of the token so a request can be resolved to one tenant without scanning them all, and only the SHA-256 of the
 * token is stored, so a database read does not hand out working credentials.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ScimApiKey {

    static final String PREFIX = "bcscim_";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private ScimApiKey() {
    }

    public static String generate(String tenantId) {
        byte[] secret = new byte[32];

        SECURE_RANDOM.nextBytes(secret);

        return PREFIX + tenantId + "_" + Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(secret);
    }

    public static String hash(String scimApiKey) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of()
                .formatHex(messageDigest.digest(scimApiKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }

    public static Optional<String> tenantIdOf(String scimApiKey) {
        if (scimApiKey == null || !scimApiKey.startsWith(PREFIX)) {
            return Optional.empty();
        }

        String rest = scimApiKey.substring(PREFIX.length());
        int separatorIndex = rest.indexOf('_');

        if (separatorIndex <= 0 || separatorIndex == rest.length() - 1) {
            return Optional.empty();
        }

        return Optional.of(rest.substring(0, separatorIndex));
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.mockito.invocation.Invocation;

/**
 * @version ee
 */
public final class AppAuditDataAssertions {

    private static final List<String> FORBIDDEN_KEY_FRAGMENTS = List.of(
        "secret", "hash", "hint", "token", "password", "credential", "authorization", "parameters", "settings",
        "configuration");

    private static final Pattern BASE64_SHAPE = Pattern.compile("[A-Za-z0-9+/]{40,}={0,2}");
    private static final Pattern DECODED_APP_KEY_SHAPE = Pattern.compile("[^:\\s]+:[A-Za-z0-9+/]{32}");
    private static final Pattern HASH_SHAPE = Pattern.compile("[0-9a-fA-F]{64}");
    private static final Pattern JWT_SHAPE =
        Pattern.compile("[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}");
    private static final int MINIMUM_CONTAINED_VALUE_LENGTH = 8;

    private AppAuditDataAssertions() {
    }

    public static void assertNoSecrets(AppAuditPublisher appAuditPublisher, String... forbiddenValues) {
        for (Invocation invocation : mockingDetails(appAuditPublisher).getInvocations()) {
            if (!"publish".equals(invocation.getMethod()
                .getName())) {

                continue;
            }

            Object[] arguments = invocation.getArguments();

            if (arguments.length == 3 && arguments[2] instanceof Map<?, ?> data) {
                assertNoSecrets(String.valueOf(arguments[0]), data, forbiddenValues);
            }
        }
    }

    public static void assertNoSecrets(String eventName, Map<?, ?> data, String... forbiddenValues) {
        List<String> offences = new ArrayList<>();

        collectOffences(eventName, data, forbiddenValues, offences);

        assertThat(offences).as("secret-bearing audit data")
            .isEmpty();
    }

    private static void collectOffences(
        String path, Object value, String[] forbiddenValues, List<String> offences) {

        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String lowerCaseKey = key.toLowerCase(Locale.ROOT);

                for (String fragment : FORBIDDEN_KEY_FRAGMENTS) {
                    if (lowerCaseKey.contains(fragment)) {
                        offences.add(path + " has the key " + key);
                    }
                }

                collectOffences(path + "." + key, entry.getValue(), forbiddenValues, offences);
            }
        } else if (value instanceof Collection<?> collection) {
            for (Object element : collection) {
                collectOffences(path, element, forbiddenValues, offences);
            }
        } else if (value != null) {
            String text = String.valueOf(value);

            for (String forbiddenValue : forbiddenValues) {
                boolean carries = forbiddenValue.length() < MINIMUM_CONTAINED_VALUE_LENGTH
                    ? text.equals(forbiddenValue) : text.contains(forbiddenValue);

                if (carries) {
                    offences.add(path + " carries a forbidden value");
                }
            }

            if (looksLikeSecret(text)) {
                offences.add(path + " carries a value shaped like a secret key, key hash or token");
            }
        }
    }

    private static boolean looksLikeSecret(String text) {
        if (HASH_SHAPE.matcher(text)
            .matches()
            || JWT_SHAPE.matcher(text)
                .matches()) {

            return true;
        }

        if (!BASE64_SHAPE.matcher(text)
            .matches()) {

            return false;
        }

        try {
            String decoded = new String(Base64.getDecoder()
                .decode(text), StandardCharsets.UTF_8);

            return DECODED_APP_KEY_SHAPE.matcher(decoded)
                .matches();
        } catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }
    }
}

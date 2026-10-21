/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.util;

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Normalizes binding keys for use in the flat tool names an app gets for LLM function calling. Binding keys may hold
 * {@code -} and {@code _}, which normalize to the same character, so uniqueness of the flat name has to be enforced on
 * the normalized form: bind and the tool listing share this one definition so they cannot drift.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AppBindingKeys {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]{0,63}$");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Za-z0-9]+");

    private AppBindingKeys() {
    }

    /**
     * Whether {@code key} is a valid binding or workflow key: a lowercase letter followed by up to 63 lowercase
     * letters, digits, {@code -} or {@code _}.
     */
    public static boolean isValidKey(@Nullable String key) {
        return key != null && KEY_PATTERN.matcher(key)
            .matches();
    }

    /**
     * Returns the regular expression a valid binding or workflow key matches.
     */
    public static String keyPattern() {
        return KEY_PATTERN.pattern();
    }

    /**
     * Returns the upper snake case form of the binding key, e.g. {@code support-crm} becomes {@code SUPPORT_CRM}.
     */
    public static String toFlatKey(String bindingKey) {
        return NON_ALPHANUMERIC.matcher(bindingKey)
            .replaceAll("_")
            .toUpperCase();
    }
}

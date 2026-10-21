/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.util;

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Makes a value that came from a third party safe to put in a log line.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AppLogValues {

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]");

    private AppLogValues() {
    }

    /**
     * Replaces line breaks and other control characters with a space, so the value cannot forge a log entry, and
     * abbreviates it to {@code maxLength} characters.
     */
    public static String toLoggableValue(String value, int maxLength) {
        String singleLineValue = CONTROL_CHARACTERS.matcher(value)
            .replaceAll(" ");

        return StringUtils.abbreviate(singleLineValue, maxLength);
    }
}

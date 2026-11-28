/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.util;

import java.util.regex.Pattern;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ToolNameNormalizer {

    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^A-Za-z0-9_-]");

    private ToolNameNormalizer() {
    }

    public static String toToolName(String componentName, String clusterElementName) {
        return sanitize(componentName) + "_" + sanitize(clusterElementName);
    }

    private static String sanitize(String value) {
        return UNSAFE_CHARS.matcher(value)
            .replaceAll("_");
    }
}

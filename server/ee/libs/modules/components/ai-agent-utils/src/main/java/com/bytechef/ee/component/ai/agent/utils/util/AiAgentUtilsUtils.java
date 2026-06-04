/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.ai.agent.utils.util;

import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 * @version ee
 */
public class AiAgentUtilsUtils {

    private static final Pattern PERFORM_FUNCTION_PATTERN = Pattern.compile(
        "(?:function\\s+perform\\s*\\(|def\\s+perform\\s*[\\s(]|perform\\s*<-\\s*function\\s*\\(|\\bperform\\s*\\(Map)");
    private static final String SCRIPTS_DIRECTORY = "scripts/";

    public static boolean isScriptEntry(String entryName) {
        String normalized = entryName.replace('\\', '/');

        return normalized.startsWith(SCRIPTS_DIRECTORY) || normalized.contains("/" + SCRIPTS_DIRECTORY);
    }

    public static boolean hasPerformFunction(String content) {
        return PERFORM_FUNCTION_PATTERN.matcher(content)
            .find();
    }
}

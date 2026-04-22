/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.util;

import com.bytechef.commons.util.JsonUtils;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts {@code {{ variableName }}} placeholders from prompt content. Matches Mustache/Handlebars-style identifiers:
 * ASCII letter/underscore start, then letters/digits/underscores. Whitespace inside the braces is tolerated. Duplicate
 * names collapse; insertion order is preserved.
 *
 * @version ee
 */
public final class PromptVariableExtractor {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*}}");

    private PromptVariableExtractor() {
    }

    /**
     * @return JSON array string (e.g. {@code ["name","age"]}) suitable for persisting to
     *         {@code ai_prompt_version.variables}, or {@code null} when {@code content} is null/empty.
     */
    public static String extractAsJson(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        Set<String> variables = extract(content);

        if (variables.isEmpty()) {
            return null;
        }

        return JsonUtils.write(List.copyOf(variables));
    }

    static Set<String> extract(String content) {
        Set<String> variables = new LinkedHashSet<>();

        Matcher matcher = VARIABLE_PATTERN.matcher(content);

        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return variables;
    }
}

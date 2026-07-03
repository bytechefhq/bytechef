/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.util;

import java.util.regex.Pattern;

/**
 * Computes the LLM-visible tool name for a cluster element. The name has to be a valid identifier per Spring AI's
 * tool-name rules (alphanumeric + underscore + hyphen) AND it has to round-trip through the search advisor's name-based
 * dispatch — when the LLM picks a discovered tool by name, the registered {@link ClusterElementToolCallback}'s
 * {@code getToolDefinition().name()} MUST match what the searcher returned.
 *
 * <p>
 * Format: <code>componentName_clusterElementName</code> with characters that aren't safe in a tool name (dots, slashes,
 * dashes other than hyphens) flattened to underscores. Examples:
 * </p>
 * <ul>
 * <li>{@code (slack, sendMessage)} → {@code slack_sendMessage}</li>
 * <li>{@code (microsoft-365, sendEmail)} → {@code microsoft_365_sendEmail}</li>
 * <li>{@code (github, ai/createIssue)} → {@code github_ai_createIssue}</li>
 * </ul>
 *
 * <p>
 * Centralised here so the catalog feeder, callback registry, and any future debug tooling agree on the shape. Changing
 * this is a wire-compatibility break — old indexed embeddings would no longer match newly-registered callbacks.
 * </p>
 *
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

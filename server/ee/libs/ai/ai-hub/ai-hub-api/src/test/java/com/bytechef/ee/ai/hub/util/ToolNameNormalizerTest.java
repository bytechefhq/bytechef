/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolNameNormalizerTest {

    @Test
    void testSimpleNames() {
        assertThat(ToolNameNormalizer.toToolName("slack", "sendMessage")).isEqualTo("slack_sendMessage");
        assertThat(ToolNameNormalizer.toToolName("github", "createIssue")).isEqualTo("github_createIssue");
    }

    @Test
    void testHyphenatedComponentNames() {
        // Hyphens are valid in tool names per Spring AI's rules; preserve them. (microsoft-365 →
        // microsoft-365_sendEmail)
        assertThat(ToolNameNormalizer.toToolName("microsoft-365", "sendEmail")).isEqualTo("microsoft-365_sendEmail");
    }

    @Test
    void testNamesWithUnsafeChars() {
        // Slashes and dots aren't valid in Spring AI tool names — must be sanitized to underscores so the LLM-visible
        // name is a clean identifier.
        assertThat(ToolNameNormalizer.toToolName("github", "ai/createIssue")).isEqualTo("github_ai_createIssue");
        assertThat(ToolNameNormalizer.toToolName("aws.s3", "putObject")).isEqualTo("aws_s3_putObject");
    }

    @Test
    void testCollapsesAllNonAlphanumericExceptHyphenAndUnderscore() {
        // Defense-in-depth: the regex must NOT silently accept other punctuation that some Spring AI client libs
        // reject. Verify the full ASCII punctuation set collapses.
        String tortured = ToolNameNormalizer.toToolName("comp.name@v1", "do.something#fast!");

        assertThat(tortured).matches("[A-Za-z0-9_-]+");
        assertThat(tortured).startsWith("comp_name_v1_");
    }
}

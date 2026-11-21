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
        assertThat(ToolNameNormalizer.toToolName("microsoft-365", "sendEmail")).isEqualTo("microsoft-365_sendEmail");
    }

    @Test
    void testNamesWithUnsafeChars() {
        assertThat(ToolNameNormalizer.toToolName("github", "ai/createIssue")).isEqualTo("github_ai_createIssue");
        assertThat(ToolNameNormalizer.toToolName("aws.s3", "putObject")).isEqualTo("aws_s3_putObject");
    }

    @Test
    void testCollapsesAllNonAlphanumericExceptHyphenAndUnderscore() {
        String toolName = ToolNameNormalizer.toToolName("comp.name@v1", "do.something#fast!");

        assertThat(toolName).matches("[A-Za-z0-9_-]+");
        assertThat(toolName).startsWith("comp_name_v1_");
    }
}

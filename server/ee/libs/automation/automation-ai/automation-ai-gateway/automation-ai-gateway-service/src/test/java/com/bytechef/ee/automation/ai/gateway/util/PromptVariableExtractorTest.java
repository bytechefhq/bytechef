/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class PromptVariableExtractorTest {

    @Test
    void testExtractAsJsonReturnsNullForNullContent() {
        assertNull(PromptVariableExtractor.extractAsJson(null));
    }

    @Test
    void testExtractAsJsonReturnsNullForEmptyContent() {
        assertNull(PromptVariableExtractor.extractAsJson(""));
    }

    @Test
    void testExtractAsJsonReturnsNullWhenNoVariablesPresent() {
        assertNull(PromptVariableExtractor.extractAsJson("Hello, world!"));
    }

    @Test
    void testExtractAsJsonFindsSimpleVariable() {
        assertEquals("[\"name\"]", PromptVariableExtractor.extractAsJson("Hello, {{name}}!"));
    }

    @Test
    void testExtractAsJsonPreservesInsertionOrder() {
        assertEquals(
            "[\"first\",\"second\",\"third\"]",
            PromptVariableExtractor.extractAsJson("{{first}} and {{second}} and {{third}}"));
    }

    @Test
    void testExtractAsJsonDeduplicatesRepeats() {
        assertEquals(
            "[\"name\",\"age\"]",
            PromptVariableExtractor.extractAsJson("{{name}} is {{age}}, hello {{name}}"));
    }

    @Test
    void testExtractAsJsonTolerantOfInternalWhitespace() {
        assertEquals(
            "[\"name\"]",
            PromptVariableExtractor.extractAsJson("Hello, {{  name  }}!"));
    }

    @Test
    void testExtractAsJsonSkipsMalformedPlaceholders() {
        // Digits-first identifier and empty braces are rejected; only the valid "ok" is returned.
        assertEquals(
            "[\"ok\"]",
            PromptVariableExtractor.extractAsJson("{{ 1invalid }} {{ }} {{ok}}"));
    }

    @Test
    void testExtractAsJsonAllowsUnderscoresAndDigits() {
        assertEquals(
            "[\"user_id\",\"Order2\"]",
            PromptVariableExtractor.extractAsJson("{{user_id}} {{Order2}}"));
    }

    @Test
    void testExtractAsJsonHandlesAdjacentPlaceholders() {
        assertEquals(
            "[\"a\",\"b\"]",
            PromptVariableExtractor.extractAsJson("{{a}}{{b}}"));
    }
}

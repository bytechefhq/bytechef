/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.compression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatRole;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 */
class AiGatewayContextCompressorTest {

    private AiGatewayContextCompressor contextCompressor;

    @BeforeEach
    void setUp() {
        contextCompressor = new AiGatewayContextCompressor(new ObjectMapper());
    }

    @Test
    void testCompressMinifiesJsonContent() {
        String prettyJson = "{\n  \"name\" : \"John\",\n  \"age\" : 30\n}";

        AiGatewayChatMessage message = new AiGatewayChatMessage("user", prettyJson);

        List<AiGatewayChatMessage> result = contextCompressor.compress(List.of(message), 0);

        assertEquals(1, result.size());
        assertEquals("{\"name\":\"John\",\"age\":30}", result.getFirst()
            .content());
    }

    @Test
    void testCompressPreservesNonJsonContent() {
        String plainText = "Hello, how are you doing today?";

        AiGatewayChatMessage message = new AiGatewayChatMessage("user", plainText);

        List<AiGatewayChatMessage> result = contextCompressor.compress(List.of(message), 0);

        assertEquals(1, result.size());
        assertEquals(plainText, result.getFirst()
            .content());
    }

    @Test
    void testCompressPreservesSystemMessages() {
        AiGatewayChatMessage systemMessage = new AiGatewayChatMessage("system", "You are a helpful assistant.");
        AiGatewayChatMessage userMessage1 = new AiGatewayChatMessage("user", "First message with some text.");
        AiGatewayChatMessage userMessage2 = new AiGatewayChatMessage("user", "Second message with some text.");

        List<AiGatewayChatMessage> result = contextCompressor.compress(
            List.of(systemMessage, userMessage1, userMessage2), 1);

        boolean systemPresent = result.stream()
            .anyMatch(message -> AiGatewayChatRole.SYSTEM == message.role());

        assertTrue(systemPresent, "System message should never be removed during trimming");
    }

    @Test
    void testCompressTrimsOldestNonSystemMessages() {
        AiGatewayChatMessage systemMessage = new AiGatewayChatMessage("system", "System prompt.");
        AiGatewayChatMessage userMessage1 = new AiGatewayChatMessage("user", "Oldest user message content.");
        AiGatewayChatMessage assistantMessage =
            new AiGatewayChatMessage("assistant", "Assistant reply content.");
        AiGatewayChatMessage userMessage2 = new AiGatewayChatMessage("user", "Newest user message content.");

        List<AiGatewayChatMessage> messages =
            List.of(systemMessage, userMessage1, assistantMessage, userMessage2);

        List<AiGatewayChatMessage> result = contextCompressor.compress(messages, 1);

        assertEquals(AiGatewayChatRole.SYSTEM, result.getFirst()
            .role());

        boolean oldestUserPresent = result.stream()
            .anyMatch(message -> "Oldest user message content.".equals(message.content()));

        assertTrue(!oldestUserPresent, "Oldest non-system message should be removed first");

        boolean newestUserPresent = result.stream()
            .anyMatch(message -> "Newest user message content.".equals(message.content()));

        assertTrue(newestUserPresent, "Newest non-system message should be preserved");
    }

    @Test
    void testCompressKeepsAtLeastOneNonSystemMessage() {
        AiGatewayChatMessage systemMessage = new AiGatewayChatMessage(
            "system", "System prompt that is quite long to consume tokens in the estimate.");
        AiGatewayChatMessage userMessage = new AiGatewayChatMessage(
            "user", "User message that is also fairly long to exceed any small token limit.");

        List<AiGatewayChatMessage> result = contextCompressor.compress(
            List.of(systemMessage, userMessage), 1);

        long nonSystemCount = result.stream()
            .filter(message -> AiGatewayChatRole.SYSTEM != message.role())
            .count();

        assertEquals(1, nonSystemCount, "At least one non-system message should always be preserved");
    }

    @Test
    void testCompressSkipsTrimmingWhenUnderLimit() {
        AiGatewayChatMessage systemMessage = new AiGatewayChatMessage("system", "System prompt.");
        AiGatewayChatMessage userMessage = new AiGatewayChatMessage("user", "Short message.");

        List<AiGatewayChatMessage> result = contextCompressor.compress(
            List.of(systemMessage, userMessage), 10000);

        assertEquals(2, result.size(), "No messages should be removed when under token limit");
    }

    @Test
    void testCompressSkipsTrimmingWhenMaxTokensZero() {
        AiGatewayChatMessage userMessage1 = new AiGatewayChatMessage("user", "First message.");
        AiGatewayChatMessage userMessage2 = new AiGatewayChatMessage("user", "Second message.");
        AiGatewayChatMessage userMessage3 = new AiGatewayChatMessage("user", "Third message.");

        List<AiGatewayChatMessage> result = contextCompressor.compress(
            List.of(userMessage1, userMessage2, userMessage3), 0);

        assertEquals(3, result.size(), "No trimming should occur when maxTokenEstimate is 0");
    }

    @Test
    void testCompressHandlesMalformedJson() {
        String malformedJson = "{this is not valid json at all}";

        AiGatewayChatMessage message = new AiGatewayChatMessage("user", malformedJson);

        List<AiGatewayChatMessage> result = contextCompressor.compress(List.of(message), 0);

        assertEquals(1, result.size());
        assertEquals(malformedJson, result.getFirst()
            .content(),
            "Malformed JSON content should be returned unchanged");
    }
}

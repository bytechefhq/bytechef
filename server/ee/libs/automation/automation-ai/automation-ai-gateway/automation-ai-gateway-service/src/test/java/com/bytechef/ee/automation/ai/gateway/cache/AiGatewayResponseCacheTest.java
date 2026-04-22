/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayTool;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayTool.AiGatewayToolFunction;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayToolChoice;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayResponseCacheTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiGatewayResponseCache aiGatewayResponseCache =
        new AiGatewayResponseCache(objectMapper);

    @Test
    void testShouldCacheReturnsFalseWhenCacheExplicitlyDisabled() {
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "gpt-4",
            List.of(new AiGatewayChatMessage("user", "Hello")),
            0.0, 100, 1.0, false, null, false);

        assertFalse(aiGatewayResponseCache.shouldCache(request));
    }

    @Test
    void testShouldCacheReturnsFalseWhenStreaming() {
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "gpt-4",
            List.of(new AiGatewayChatMessage("user", "Hello")),
            0.0, 100, 1.0, true, null, true);

        assertFalse(aiGatewayResponseCache.shouldCache(request));
    }

    @Test
    void testShouldCacheReturnsFalseWhenTemperatureAboveZero() {
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "gpt-4",
            List.of(new AiGatewayChatMessage("user", "Hello")),
            0.7, 100, 1.0, false, null, true);

        assertFalse(aiGatewayResponseCache.shouldCache(request));
    }

    @Test
    void testShouldCacheReturnsTrueWhenTemperatureNull() {
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "gpt-4",
            List.of(new AiGatewayChatMessage("user", "Hello")),
            null, 100, 1.0, false, null, true);

        assertTrue(aiGatewayResponseCache.shouldCache(request));
    }

    @Test
    void testShouldCacheReturnsTrueWhenTemperatureZero() {
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "gpt-4",
            List.of(new AiGatewayChatMessage("user", "Hello")),
            0.0, 100, 1.0, false, null, true);

        assertTrue(aiGatewayResponseCache.shouldCache(request));
    }

    @Test
    void testComputeCacheKeyDifferentWithDifferentTools() {
        List<AiGatewayChatMessage> messages = List.of(new AiGatewayChatMessage("user", "Hello"));

        AiGatewayTool toolA = new AiGatewayTool(
            "function",
            new AiGatewayToolFunction("getWeather", "Get the weather", Map.of("type", "object")));

        AiGatewayTool toolB = new AiGatewayTool(
            "function",
            new AiGatewayToolFunction("getTime", "Get the current time", Map.of("type", "object")));

        AiGatewayChatCompletionRequest requestWithToolA = new AiGatewayChatCompletionRequest(
            "gpt-4", messages, 0.0, 100, 1.0, false, null, true, null, List.of(toolA));

        AiGatewayChatCompletionRequest requestWithToolB = new AiGatewayChatCompletionRequest(
            "gpt-4", messages, 0.0, 100, 1.0, false, null, true, null, List.of(toolB));

        String keyA = aiGatewayResponseCache.computeCacheKey(requestWithToolA);
        String keyB = aiGatewayResponseCache.computeCacheKey(requestWithToolB);

        assertNotEquals(keyA, keyB);
    }

    @Test
    void testComputeCacheKeyDifferentWithDifferentToolChoice() {
        List<AiGatewayChatMessage> messages = List.of(new AiGatewayChatMessage("user", "Hello"));

        AiGatewayChatCompletionRequest requestWithAutoChoice = new AiGatewayChatCompletionRequest(
            "gpt-4", messages, 0.0, 100, 1.0, false, null, true, AiGatewayToolChoice.ofString("auto"), null);

        AiGatewayChatCompletionRequest requestWithNoneChoice = new AiGatewayChatCompletionRequest(
            "gpt-4", messages, 0.0, 100, 1.0, false, null, true, AiGatewayToolChoice.ofString("none"), null);

        String keyAuto = aiGatewayResponseCache.computeCacheKey(requestWithAutoChoice);
        String keyNone = aiGatewayResponseCache.computeCacheKey(requestWithNoneChoice);

        assertNotEquals(keyAuto, keyNone);
    }

    @Test
    void testComputeCacheKeyConsistentForSameInput() {
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "gpt-4",
            List.of(new AiGatewayChatMessage("user", "Hello")),
            0.0, 100, 1.0, false, null, true);

        String firstKey = aiGatewayResponseCache.computeCacheKey(request);
        String secondKey = aiGatewayResponseCache.computeCacheKey(request);

        assertEquals(firstKey, secondKey);
    }
}

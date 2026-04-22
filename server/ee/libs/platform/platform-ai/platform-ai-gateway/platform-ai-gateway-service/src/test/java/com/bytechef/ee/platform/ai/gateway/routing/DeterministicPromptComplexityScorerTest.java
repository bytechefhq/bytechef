/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayTool;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class DeterministicPromptComplexityScorerTest {

    private final DeterministicPromptComplexityScorer scorer = new DeterministicPromptComplexityScorer();

    private static AiGatewayChatCompletionRequest request(
        List<AiGatewayChatMessage> messages, Integer maxTokens, List<AiGatewayTool> tools) {

        return new AiGatewayChatCompletionRequest(
            "gpt-4", messages, null, maxTokens, null, false, null, null, null, tools);
    }

    private static AiGatewayChatMessage user(String content) {
        return new AiGatewayChatMessage(AiGatewayChatRole.USER, content);
    }

    @Test
    void testShortPromptScoresLow() {
        double score = scorer.score(request(List.of(user("hi")), null, null));

        assertTrue(score < 0.2, "expected low score, got " + score);
    }

    @Test
    void testLongMultiToolPromptScoresHigh() {
        String longText = "x".repeat(8000);

        AiGatewayTool tool = new AiGatewayTool(
            "function", new AiGatewayTool.AiGatewayToolFunction("f", "d", Map.of()));

        double score = scorer.score(
            request(List.of(user(longText)), 4000, List.of(tool, tool, tool, tool, tool, tool, tool, tool)));

        assertTrue(score > 0.7, "expected high score, got " + score);
    }

    @Test
    void testCodeContentScoresAboveEqualLengthProse() {
        String prose = "please summarize the following text ".repeat(10);
        String code = "```json\n{\"a\": 1, \"b\": 2}\n``` ".repeat(10);

        double proseScore = scorer.score(request(List.of(user(prose)), null, null));
        double codeScore = scorer.score(request(List.of(user(code)), null, null));

        assertTrue(codeScore > proseScore, "code " + codeScore + " should exceed prose " + proseScore);
    }

    @Test
    void testScoreClampedToUnitInterval() {
        String huge = "x".repeat(100000);

        double score = scorer.score(request(List.of(user(huge)), 100000, null));

        assertTrue(score <= 1.0 && score >= 0.0, "score out of range: " + score);
    }

    @Test
    void testNullContentAndNullFieldsContributeZero() {
        AiGatewayChatMessage empty = new AiGatewayChatMessage(AiGatewayChatRole.USER, null);

        double score = scorer.score(request(List.of(empty), null, null));

        assertEquals(0.0, score, 0.0001);
    }
}

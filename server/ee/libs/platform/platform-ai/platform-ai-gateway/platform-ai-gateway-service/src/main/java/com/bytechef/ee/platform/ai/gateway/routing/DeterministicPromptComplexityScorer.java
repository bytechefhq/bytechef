/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayTool;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic, model-free prompt complexity scorer. Combines prompt size, tool count, structured (code/JSON) content,
 * conversation turns, and requested output size into a 0.0–1.0 score. No LLM or embedding call is made, keeping the
 * routing hot path cheap.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class DeterministicPromptComplexityScorer implements PromptComplexityScorer {

    // Signal weights; must sum to 1.0. SIZE/STRUCTURED were tuned to 0.40/0.15 (from an initial 0.35/0.20) so that a
    // large, multi-tool request without structured content still scores above the "complex" routing threshold.
    private static final double SIZE_WEIGHT = 0.40;
    private static final double TOOL_WEIGHT = 0.25;
    private static final double STRUCTURED_WEIGHT = 0.15;
    private static final double TURNS_WEIGHT = 0.10;
    private static final double OUTPUT_WEIGHT = 0.10;

    private static final double SIZE_TOKEN_CEILING = 2000.0;
    private static final double TOOL_COUNT_CEILING = 8.0;

    // 19 turns past the first message (i.e. 20 messages) saturates the turns signal at 1.0.
    private static final double TURNS_CEILING = 19.0;
    private static final double OUTPUT_TOKEN_CEILING = 4000.0;

    @Override
    public double score(AiGatewayChatCompletionRequest request) {
        List<AiGatewayChatMessage> messages = request.messages();

        int totalChars = 0;
        boolean structured = false;

        for (AiGatewayChatMessage message : messages) {
            String content = message.content();

            if (content != null) {
                totalChars += content.length();

                if (!structured && containsStructuredContent(content)) {
                    structured = true;
                }
            }
        }

        List<AiGatewayTool> tools = request.tools();

        int estimatedTokens = totalChars / 4;
        int toolCount = tools == null ? 0 : tools.size();
        int messageCount = messages.size();
        Integer maxTokens = request.maxTokens();

        double sizeScore = Math.min(estimatedTokens / SIZE_TOKEN_CEILING, 1.0);
        double toolScore = Math.min(toolCount / TOOL_COUNT_CEILING, 1.0);
        double structuredScore = structured ? 1.0 : 0.0;
        double turnsScore = Math.min(Math.max(messageCount - 1, 0) / TURNS_CEILING, 1.0);
        double outputScore = maxTokens == null ? 0.0 : Math.min(maxTokens / OUTPUT_TOKEN_CEILING, 1.0);

        double weighted = SIZE_WEIGHT * sizeScore
            + TOOL_WEIGHT * toolScore
            + STRUCTURED_WEIGHT * structuredScore
            + TURNS_WEIGHT * turnsScore
            + OUTPUT_WEIGHT * outputScore;

        return Math.max(0.0, Math.min(weighted, 1.0));
    }

    // Cheap proxy for code/JSON. False positives (e.g. prose containing "{x}" and a "3:00" time) are tolerated
    // because the structured-content weight is intentionally small; messages carrying only contentBlocks (multi-modal,
    // null content) are not inspected and contribute zero — an accepted limitation for this heuristic.
    private static boolean containsStructuredContent(String content) {
        if (content.contains("```")) {
            return true;
        }

        return content.contains("{") && content.contains("}") && content.contains(":");
    }
}

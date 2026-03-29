/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.agent.eval.judge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springaicommunity.judge.DeterministicJudge;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Judgment;

/**
 * A deterministic judge that checks whether a specific tool was used in the agent's execution transcript, with support
 * for position-based and count-based verification.
 *
 * @author Ivica Cardic
 */
class ToolUsageJudge extends DeterministicJudge {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String comparison;
    private final int count;
    private final String position;
    private final String toolName;

    ToolUsageJudge(String toolName, String position, String comparison, int count) {
        super("ToolUsage", "Checks usage of tool: " + toolName);

        this.toolName = toolName;
        this.position = position;
        this.comparison = comparison;
        this.count = count;
    }

    @Override
    public Judgment judge(JudgmentContext context) {
        Map<String, Object> metadata = context.metadata();
        Object transcriptValue = metadata.get("transcript");

        if (transcriptValue == null) {
            return Judgment.fail("No transcript found in context metadata; cannot evaluate tool usage");
        }

        String transcript = transcriptValue.toString();

        List<String> toolCallNames;

        try {
            toolCallNames = extractToolCallNames(transcript);
        } catch (Exception exception) {
            return Judgment.fail("Failed to parse transcript JSON: " + exception.getMessage());
        }

        if ("FIRST".equals(position)) {
            return evaluateFirstPosition(toolCallNames);
        } else if ("LAST".equals(position)) {
            return evaluateLastPosition(toolCallNames);
        } else {
            return evaluateAnywhereWithComparison(toolCallNames);
        }
    }

    private List<String> extractToolCallNames(String transcript) throws Exception {
        JsonNode rootNode = OBJECT_MAPPER.readTree(transcript);
        JsonNode messagesNode = rootNode.path("messages");

        List<String> toolCallNames = new ArrayList<>();

        for (JsonNode messageNode : messagesNode) {
            JsonNode toolCallsNode = messageNode.path("toolCalls");

            if (toolCallsNode.isArray()) {
                for (JsonNode toolCallNode : toolCallsNode) {
                    JsonNode toolNameNode = toolCallNode.path("toolName");

                    if (!toolNameNode.isMissingNode()) {
                        toolCallNames.add(toolNameNode.asText());
                    }
                }
            }
        }

        return toolCallNames;
    }

    private Judgment evaluateFirstPosition(List<String> toolCallNames) {
        if (toolCallNames.isEmpty()) {
            return Judgment.fail("No tool calls found in transcript; expected '" + toolName + "' at FIRST position");
        }

        String firstToolName = toolCallNames.getFirst();

        if (toolName.equals(firstToolName)) {
            return Judgment.pass("Tool '" + toolName + "' was used at the FIRST position as expected");
        }

        return Judgment.fail(
            "Expected tool '" + toolName + "' at FIRST position, but found '" + firstToolName + "'");
    }

    private Judgment evaluateLastPosition(List<String> toolCallNames) {
        if (toolCallNames.isEmpty()) {
            return Judgment.fail("No tool calls found in transcript; expected '" + toolName + "' at LAST position");
        }

        String lastToolName = toolCallNames.getLast();

        if (toolName.equals(lastToolName)) {
            return Judgment.pass("Tool '" + toolName + "' was used at the LAST position as expected");
        }

        return Judgment.fail(
            "Expected tool '" + toolName + "' at LAST position, but found '" + lastToolName + "'");
    }

    private Judgment evaluateAnywhereWithComparison(List<String> toolCallNames) {
        long actualCount = toolCallNames.stream()
            .filter(toolName::equals)
            .count();

        boolean passes = switch (comparison) {
            case "EXACTLY" -> actualCount == count;
            case "AT_MOST" -> actualCount <= count;
            default -> actualCount >= count;
        };

        if (passes) {
            return Judgment.pass(
                "Tool '" + toolName + "' usage count " + actualCount + " satisfies " + comparison + " " + count);
        }

        return Judgment.fail(
            "Tool '" + toolName + "' usage count " + actualCount + " does not satisfy " + comparison + " " + count);
    }
}

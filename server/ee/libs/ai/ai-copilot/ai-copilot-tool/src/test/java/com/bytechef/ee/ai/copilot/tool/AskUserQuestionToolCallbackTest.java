/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AskUserQuestionToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testReturnsAskUserQuestionEnvelope() throws Exception {
        AskUserQuestionToolCallback callback =
            new AskUserQuestionToolCallback(mock(ToolStateVisibilityMetrics.class), jsonMapper);

        // Mirrors the library's input schema: questions[] with question/header/multiSelect/options[label, description].
        String toolInput = """
            {
                "questions": [
                    {
                        "question": "Which messaging component?",
                        "header": "Pick",
                        "multiSelect": false,
                        "options": [
                            {"label": "slack", "description": "Slack messaging"},
                            {"label": "discord", "description": "Discord messaging"}
                        ]
                    }
                ]
            }""";

        String result = callback.call(toolInput);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("kind")
            .asText()).isEqualTo("ask-user-question");
        assertThat(node.get("awaitingAnswer")
            .asBoolean()).isTrue();

        JsonNode questions = node.get("questions");

        assertThat(questions.size()).isEqualTo(1);
        assertThat(questions.get(0)
            .get("question")
            .asText()).isEqualTo("Which messaging component?");
        assertThat(questions.get(0)
            .get("header")
            .asText()).isEqualTo("Pick");
        assertThat(questions.get(0)
            .get("multiSelect")
            .asBoolean()).isFalse();
        assertThat(questions.get(0)
            .get("options")
            .size()).isEqualTo(2);
        assertThat(questions.get(0)
            .get("options")
            .get(0)
            .get("label")
            .asText()).isEqualTo("slack");
    }

    @Test
    void testReturnsToolErrorOnMalformedInput() throws Exception {
        AskUserQuestionToolCallback callback =
            new AskUserQuestionToolCallback(mock(ToolStateVisibilityMetrics.class), jsonMapper);

        String result = callback.call("{not-json}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testToolNameMatchesLibrary() {
        AskUserQuestionToolCallback callback =
            new AskUserQuestionToolCallback(mock(ToolStateVisibilityMetrics.class), jsonMapper);

        assertThat(callback.getToolDefinition()
            .name()).isEqualTo("askUserQuestion");
    }

    @Test
    void testHandlesMultipleQuestions() throws Exception {
        AskUserQuestionToolCallback callback =
            new AskUserQuestionToolCallback(mock(ToolStateVisibilityMetrics.class), jsonMapper);

        // Two-question payload — exercises the multi-question loop in serialiseQuestions and the placeholder
        // map being keyed by question text. Each question carries the minimum 2 options; the wrapper's
        // pre-validation rejects 1-option questions (see testRejectsQuestionWithFewerThanTwoOptions).
        String toolInput = """
            {
                "questions": [
                    {
                        "question": "First?",
                        "header": "Q1",
                        "multiSelect": false,
                        "options": [{"label": "a", "description": "alpha"}, {"label": "b", "description": "beta"}]
                    },
                    {
                        "question": "Second?",
                        "header": "Q2",
                        "multiSelect": true,
                        "options": [{"label": "x", "description": "x-ray"}, {"label": "y", "description": "yankee"}]
                    }
                ]
            }""";

        String result = callback.call(toolInput);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("questions")
            .size()).isEqualTo(2);
        assertThat(node.get("questions")
            .get(1)
            .get("multiSelect")
            .asBoolean()).isTrue();
    }

    @Test
    void testRejectsHeaderLongerThanTwelveCharacters() throws Exception {
        // The library only logs WARN for header > 12 chars and lets the malformed Question through, which
        // then causes Spring AI's Anthropic adapter to emit an empty tool_result content block — Anthropic
        // rejects the next iteration with "messages.<N>: user messages must have non-empty content."
        // Reproduced in production with header "Slack Channel" (13 chars). Reject in our wrapper instead.
        AskUserQuestionToolCallback callback =
            new AskUserQuestionToolCallback(mock(ToolStateVisibilityMetrics.class), jsonMapper);

        String toolInput = """
            {
                "questions": [
                    {
                        "question": "Which channel?",
                        "header": "Slack Channel",
                        "options": [
                            {"label": "general", "description": "general channel"},
                            {"label": "engineering", "description": "engineering channel"}
                        ]
                    }
                ]
            }""";

        String result = callback.call(toolInput);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText())
                .contains("header")
                .contains("12");
    }

    @Test
    void testRejectsQuestionWithFewerThanTwoOptions() throws Exception {
        // Single-option questions reproduce the same production failure mode as the header-too-long case
        // — library WARNs and accepts; downstream serialisation produces an empty tool_result content block.
        AskUserQuestionToolCallback callback =
            new AskUserQuestionToolCallback(mock(ToolStateVisibilityMetrics.class), jsonMapper);

        String toolInput = """
            {
                "questions": [
                    {
                        "question": "Which connection?",
                        "header": "Conn",
                        "options": [{"label": "prod", "description": "production"}]
                    }
                ]
            }""";

        String result = callback.call(toolInput);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText())
                .contains("options")
                .contains("2-4");
    }

    @Test
    void testRejectsOptionWithBlankDescription() throws Exception {
        // The library's Option compact constructor throws on blank description; Spring AI's MethodToolCallback
        // would catch and convert it into a generic error. Catching it in our wrapper produces a more
        // actionable error message the LLM can act on without seeing internal exception text.
        AskUserQuestionToolCallback callback =
            new AskUserQuestionToolCallback(mock(ToolStateVisibilityMetrics.class), jsonMapper);

        String toolInput = """
            {
                "questions": [
                    {
                        "question": "Which environment?",
                        "header": "Env",
                        "options": [
                            {"label": "prod", "description": ""},
                            {"label": "dev", "description": "development"}
                        ]
                    }
                ]
            }""";

        String result = callback.call(toolInput);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("description");
    }

    @Test
    void testRejectsMoreThanFourQuestions() throws Exception {
        // Defensive — library would throw IllegalArgumentException here, but matching its 1-4 bound in our
        // wrapper means the LLM sees a consistent error format across all schema violations.
        AskUserQuestionToolCallback callback =
            new AskUserQuestionToolCallback(mock(ToolStateVisibilityMetrics.class), jsonMapper);

        StringBuilder questions = new StringBuilder("[");

        for (int i = 0; i < 5; i++) {
            if (i > 0) {
                questions.append(",");
            }

            questions.append("{\"question\":\"Q")
                .append(i)
                .append("?\",\"header\":\"H")
                .append(i)
                .append("\",\"options\":[{\"label\":\"a\",\"description\":\"alpha\"},")
                .append("{\"label\":\"b\",\"description\":\"beta\"}]}");
        }

        questions.append("]");

        String result = callback.call("{\"questions\":" + questions + "}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("1-4");
    }
}

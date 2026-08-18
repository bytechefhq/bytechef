/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.copilot.tool.AskUserQuestionToolCallback;
import com.bytechef.ai.copilot.tool.ToolStateVisibilityMetrics;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SubagentAskUserQuestionToolCallbackTest {

    // Mirrors the library's input schema, copied from AskUserQuestionToolCallbackTest.
    private static final String VALID_TOOL_INPUT = """
        {
            "questions": [
                {
                    "question": "Which agent did you mean?",
                    "header": "Agent",
                    "multiSelect": false,
                    "options": [
                        {"label": "Support", "description": "the support agent"},
                        {"label": "Sales", "description": "the sales agent"}
                    ]
                }
            ]
        }""";

    @Test
    void testCallWritesTheEnvelopeToTheChannelAndReturnsTheStopInstruction() {
        String result = SubagentAskChannel.runWithChannel(() -> {
            SubagentAskUserQuestionToolCallback toolCallback = new SubagentAskUserQuestionToolCallback();

            String returned = toolCallback.call(VALID_TOOL_INPUT, null);

            assertThat(SubagentAskChannel.pending()).contains("\"kind\":\"ask-user-question\"");

            return returned;
        });

        assertThat(result).contains("Stop now");
        assertThat(result).doesNotContain("ask-user-question");
    }

    /**
     * The envelope must match the main agent's byte for byte — both feed the same client renderer.
     */
    @Test
    void testTheOfferedEnvelopeCarriesEveryRenderedField() {
        String pending = SubagentAskChannel.runWithChannel(() -> {
            SubagentAskUserQuestionToolCallback toolCallback = new SubagentAskUserQuestionToolCallback();

            toolCallback.call(VALID_TOOL_INPUT, null);

            return SubagentAskChannel.pending();
        });

        assertThat(pending).contains("\"awaitingAnswer\":true");
        assertThat(pending).contains("Which agent did you mean?");
        assertThat(pending).contains("\"header\":\"Agent\"");
        assertThat(pending).contains("\"label\":\"Support\"");
    }

    @Test
    void testSecondQuestionInOneDelegationReturnsAToolError() {
        String result = SubagentAskChannel.runWithChannel(() -> {
            SubagentAskUserQuestionToolCallback toolCallback = new SubagentAskUserQuestionToolCallback();

            toolCallback.call(VALID_TOOL_INPUT, null);

            return toolCallback.call(VALID_TOOL_INPUT, null);
        });

        assertThat(result).contains("error");
    }

    /**
     * A rejected input must not be mistaken for a question — nothing is offered and the delegate's own error text
     * reaches the specialist's LLM unchanged.
     */
    @Test
    void testMalformedInputReturnsTheToolErrorAndOffersNothing() {
        String pending = SubagentAskChannel.runWithChannel(() -> {
            SubagentAskUserQuestionToolCallback toolCallback = new SubagentAskUserQuestionToolCallback();

            String returned = toolCallback.call("{not-json}", null);

            assertThat(returned).contains("error");

            return SubagentAskChannel.pending();
        });

        assertThat(pending).isNull();
    }

    @Test
    void testToolIsNamedAskUserQuestionSoPromptsMatchTheMainAgent() {
        SubagentAskUserQuestionToolCallback toolCallback = new SubagentAskUserQuestionToolCallback();

        assertThat(
            toolCallback.getToolDefinition()
                .name()).isEqualTo("askUserQuestion");
    }

    @Test
    void testInputSchemaIsTheMainAgentToolsVerbatimSoTheEnvelopeCannotDrift() {
        SubagentAskUserQuestionToolCallback toolCallback = new SubagentAskUserQuestionToolCallback();
        AskUserQuestionToolCallback mainAgentToolCallback =
            new AskUserQuestionToolCallback(ToolStateVisibilityMetrics.NOOP);

        ToolDefinition toolDefinition = toolCallback.getToolDefinition();
        ToolDefinition mainAgentToolDefinition = mainAgentToolCallback.getToolDefinition();

        assertThat(toolDefinition.inputSchema()).isEqualTo(mainAgentToolDefinition.inputSchema());
    }

    /**
     * The specialist contract cannot live in the specialist's system prompt: that file is shared with a Copilot panel
     * agent whose own {@code askUserQuestion} follows the opposite contract, or which has no such tool at all. The
     * description is read only by agents actually holding the tool.
     */
    @Test
    void testDescriptionTellsTheSpecialistToStopAfterAsking() {
        SubagentAskUserQuestionToolCallback toolCallback = new SubagentAskUserQuestionToolCallback();
        AskUserQuestionToolCallback mainAgentToolCallback =
            new AskUserQuestionToolCallback(ToolStateVisibilityMetrics.NOOP);

        ToolDefinition toolDefinition = toolCallback.getToolDefinition();
        ToolDefinition mainAgentToolDefinition = mainAgentToolCallback.getToolDefinition();

        assertThat(toolDefinition.description())
            .startsWith(mainAgentToolDefinition.description())
            .contains("Ask at most ONE question per delegation, then STOP")
            .contains("Return a one-line summary naming what you asked and what you had already established")
            .contains("never the user's answer");
    }

    /**
     * Per-conversation specialist memory is attached only by {@code AiHubConfiguration#wrapDelegate}. Both Copilot
     * panel configurations and all three management-MCP contributors pass an identity {@code chatClientDecorator}, and
     * the MCP surface carries no conversation id to key a session on at all — so a specialist there genuinely starts
     * from nothing on the follow-up call. One tool instance is attached on every surface, so neither of its two texts
     * may promise a retention only one surface provides.
     */
    @Test
    void testNeitherTextPromisesThatTheSpecialistsContextSurvivesTheFollowUpCall() {
        SubagentAskUserQuestionToolCallback toolCallback = new SubagentAskUserQuestionToolCallback();

        ToolDefinition toolDefinition = toolCallback.getToolDefinition();

        String stopInstruction = SubagentAskChannel.runWithChannel(
            () -> toolCallback.call(VALID_TOOL_INPUT, null));

        assertThat(toolDefinition.description())
            .doesNotContain("context intact")
            .doesNotContain("prior context");
        assertThat(stopInstruction)
            .doesNotContain("context intact")
            .doesNotContain("prior context");
        assertThat(stopInstruction)
            .as("the summary is the only thing that reaches the follow-up call on every surface, so the specialist"
                + " must be told to put what matters into it")
            .contains("put anything the follow-up needs into that summary");
    }
}

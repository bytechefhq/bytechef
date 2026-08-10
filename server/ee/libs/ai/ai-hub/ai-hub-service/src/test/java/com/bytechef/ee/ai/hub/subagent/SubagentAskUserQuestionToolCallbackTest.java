/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
}

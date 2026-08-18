/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.copilot.tool.ask.SubAgentAskRelayToolCallback;
import com.bytechef.ai.copilot.tool.ask.SubAgentQuestionRenderer;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Drives a stub specialist that calls the real ask tool, through the real {@link SubagentAskChannelRelay} and the CE
 * {@link SubAgentAskRelayToolCallback} the catalog wraps every intelligent delegate in, pinning the whole path from
 * "specialist asks" to "delegate's tool result carries the question".
 *
 * <p>
 * Not a Spring or Testcontainers test — the wiring under test is plain object composition, and a real {@code ChatModel}
 * would only add nondeterminism to a path that has none.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SubagentAskRelayEndToEndTest {

    private static final String SPECIALIST_SUMMARY = "Asked which agent the user meant.";

    private static final String VALID_ASK_INPUT = """
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
    void testDelegateReturnsTheQuestionPayloadWhenTheSpecialistAsks() {
        ToolCallback toolCallback = newToolCallback(true, SubAgentQuestionRenderer.JSON);

        String result = toolCallback.call("{\"request\": \"create an agent\"}");

        assertThat(result).contains("\"kind\":\"ask-user-question\"");
        assertThat(result).contains("Which agent did you mean?");
        assertThat(result).doesNotContain(SPECIALIST_SUMMARY);
    }

    @Test
    void testDelegateReturnsTheSummaryWhenTheSpecialistAsksNothing() {
        ToolCallback toolCallback = newToolCallback(false, SubAgentQuestionRenderer.JSON);

        String result = toolCallback.call("{\"request\": \"create an agent\"}");

        assertThat(result).isEqualTo(SPECIALIST_SUMMARY);
    }

    /**
     * The management MCP surface has no client-side renderer for the JSON envelope, so its question comes back as
     * numbered text carrying the re-invocation instruction.
     */
    @Test
    void testMcpSurfaceRendersTheQuestionAsNumberedPlainText() {
        ToolCallback toolCallback = newToolCallback(true, SubAgentQuestionRenderer.PLAIN_TEXT);

        String result = toolCallback.call("{\"request\": \"create an agent\"}");

        assertThat(result)
            .contains("Which agent did you mean?")
            .contains("1. Support — the support agent")
            .contains("2. Sales — the sales agent")
            .contains("call configureMcpServer again, restating the original request together with the chosen"
                + " answer")
            .contains("this agent does not carry anything over from the call that asked")
            .doesNotContain("\"kind\":\"ask-user-question\"");
    }

    /**
     * The specialist must never read its own question back as though it were the answer: the ask tool returns a stop
     * instruction, and only the delegate returns the payload.
     */
    @Test
    void testTheAskToolReturnsAStopInstructionToTheSpecialistNotThePayload() {
        StubSpecialist specialist = new StubSpecialist(true);

        newToolCallback(specialist, SubAgentQuestionRenderer.JSON).call("{\"request\": \"create an agent\"}");

        assertThat(specialist.askToolResult)
            .isNotNull()
            .doesNotContain("ask-user-question")
            .contains("Stop now and return a one-line summary");
    }

    /**
     * Matters more than it looks: tool-execution threads are pooled, so a leaked binding would make an unrelated later
     * delegation return a stale question card.
     */
    @Test
    void testChannelIsUnboundAfterTheDelegationSoTheNextTurnStartsClean() {
        ToolCallback toolCallback = newToolCallback(true, SubAgentQuestionRenderer.JSON);

        toolCallback.call("{\"request\": \"create an agent\"}");

        assertThat(SubagentAskChannel.pending()).isNull();
    }

    private static ToolCallback newToolCallback(boolean specialistAsks, SubAgentQuestionRenderer questionRenderer) {
        return newToolCallback(new StubSpecialist(specialistAsks), questionRenderer);
    }

    private static ToolCallback newToolCallback(
        StubSpecialist specialist, SubAgentQuestionRenderer questionRenderer) {

        return new SubAgentAskRelayToolCallback(specialist, new SubagentAskChannelRelay(), questionRenderer);
    }

    /**
     * Stands in for one intelligent delegate {@code ToolCallback} — the thing the catalog builds and wraps. Its
     * "specialist" is the lambda body: it optionally calls the real ask tool, then returns its own one-line summary,
     * exactly as a real delegate's inner {@code ChatClient} call would.
     */
    private static final class StubSpecialist implements ToolCallback {

        private final boolean specialistAsks;

        private @Nullable String askToolResult;

        private StubSpecialist(boolean specialistAsks) {
            this.specialistAsks = specialistAsks;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("configureMcpServer")
                .description("Manages MCP servers.")
                .inputSchema("{\"type\":\"object\"}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, @Nullable ToolContext toolContext) {
            if (specialistAsks) {
                SubagentAskUserQuestionToolCallback askToolCallback = new SubagentAskUserQuestionToolCallback();

                askToolResult = askToolCallback.call(VALID_ASK_INPUT, null);
            }

            return SPECIALIST_SUMMARY;
        }
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.tool.AutomationSubAgentType;
import com.bytechef.automation.ai.tool.SubAgentToolCallback;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Drives a stub specialist that calls the real ask tool, through the real {@link SubagentAskChannelRelay} and
 * {@link SubAgentToolCallback}, pinning the whole path from "specialist asks" to "delegate's tool result carries the
 * question".
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
        SubAgentToolCallback toolCallback = newToolCallback(true);

        String result = toolCallback.call("{\"request\": \"create an agent\"}");

        assertThat(result).contains("\"kind\":\"ask-user-question\"");
        assertThat(result).contains("Which agent did you mean?");
        assertThat(result).doesNotContain(SPECIALIST_SUMMARY);
    }

    @Test
    void testDelegateReturnsTheSummaryWhenTheSpecialistAsksNothing() {
        SubAgentToolCallback toolCallback = newToolCallback(false);

        String result = toolCallback.call("{\"request\": \"create an agent\"}");

        assertThat(result).isEqualTo(SPECIALIST_SUMMARY);
    }

    /**
     * Matters more than it looks: tool-execution threads are pooled, so a leaked binding would make an unrelated later
     * delegation return a stale question card.
     */
    @Test
    void testChannelIsUnboundAfterTheDelegationSoTheNextTurnStartsClean() {
        SubAgentToolCallback toolCallback = newToolCallback(true);

        toolCallback.call("{\"request\": \"create an agent\"}");

        assertThat(SubagentAskChannel.pending()).isNull();
    }

    /**
     * @param specialistAsks whether the stub specialist calls the ask tool while the delegation runs
     */
    private static SubAgentToolCallback newToolCallback(boolean specialistAsks) {
        ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        when(chatClient.prompt(anyString())
            .toolContext(Map.of())
            .call()
            .content()).thenAnswer(invocation -> {
                if (specialistAsks) {
                    SubagentAskUserQuestionToolCallback askToolCallback = new SubagentAskUserQuestionToolCallback();

                    askToolCallback.call(VALID_ASK_INPUT, null);
                }

                return SPECIALIST_SUMMARY;
            });

        return new SubAgentToolCallback(
            AutomationSubAgentType.PROJECT_DEPLOYMENT_AGENT, chatClient, "Manages project deployments.",
            new SubagentAskChannelRelay());
    }
}

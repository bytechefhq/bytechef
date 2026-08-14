/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;

/**
 * Pins the round trip the two specs only make sense together: a specialist asks, the user answers, and the follow-up
 * delegation still holds what the specialist knew when it asked.
 *
 * <p>
 * Without memory this feature is worse than nothing — a specialist that asks a good question and is re-delegated with
 * total amnesia has to re-derive everything, and the user, having answered a specific question, watches it start over.
 * Buttons make that failure faster to reach, not less painful. This test is what keeps the two wired together.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class SubagentAskThenAnswerContinuityTest {

    private static final String TASK_AGENT = "task_agent";
    private static final String THREAD_ID = "thread-1";

    private AiHubSessionMemory aiHubSessionMemory;

    @BeforeEach
    void setUp() {
        aiHubSessionMemory = new AiHubSessionMemory(
            InMemorySessionRepository.builder()
                .build(),
            null);
    }

    @Test
    void testTheDelegationAfterAnAnswerStillSeesWhyTheQuestionWasAsked() {
        String sessionId = SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, TASK_AGENT);

        // Turn 1: the specialist drafts, then asks which of two agents the user meant.
        appendExchange(
            sessionId, "rename my support agent",
            "Draft: 'Support Triage'. Asked whether you meant Support or Sales.");

        // Turn 2: the parent re-delegates with the user's answer. What the specialist loads is turn 1.
        List<SessionEvent> replayed = aiHubSessionMemory.sessionService()
            .getEvents(sessionId, EventFilter.lastN(SubAgentSessionMemoryContributor.MAX_EVENTS));

        assertThat(replayed).hasSize(2);
        assertThat(
            replayed.get(1)
                .getMessage()
                .getText())
                    .contains("Support Triage")
                    .contains("Support or Sales");
    }

    /**
     * The answer belongs to the specialist that asked. A different specialist on the same conversation must not see it
     * — it never asked anything, and inheriting another's half-finished decision is worse than starting clean.
     */
    @Test
    void testAnotherSpecialistOnTheSameThreadDoesNotInheritTheAnswer() {
        appendExchange(
            SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, TASK_AGENT),
            "rename my support agent", "Asked whether you meant Support or Sales.");

        List<SessionEvent> otherSpecialistEvents = aiHubSessionMemory.sessionService()
            .getEvents(
                SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, "mcp_agent"),
                EventFilter.lastN(SubAgentSessionMemoryContributor.MAX_EVENTS));

        assertThat(otherSpecialistEvents).isEmpty();
    }

    private void appendExchange(String sessionId, String userText, String assistantText) {
        if (aiHubSessionMemory.sessionRepository()
            .findById(sessionId) == null) {

            aiHubSessionMemory.sessionRepository()
                .save(
                    Session.builder()
                        .id(sessionId)
                        .userId(AiHubSessionMemory.SESSION_USER_ID)
                        .createdAt(Instant.now())
                        .build());
        }

        aiHubSessionMemory.sessionRepository()
            .appendEvent(
                SessionEvent.builder()
                    .sessionId(sessionId)
                    .message(new UserMessage(userText))
                    .build());

        aiHubSessionMemory.sessionRepository()
            .appendEvent(
                SessionEvent.builder()
                    .sessionId(sessionId)
                    .message(new AssistantMessage(assistantText))
                    .build());
    }
}

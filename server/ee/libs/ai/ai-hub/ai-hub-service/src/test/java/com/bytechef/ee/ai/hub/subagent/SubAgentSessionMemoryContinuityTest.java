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
 * Pins what a specialist subagent actually loads on its second delegation: its own prior exchange, nothing from a
 * different specialist on the same conversation, and no more than the replay window.
 *
 * <p>
 * Writes turns straight through the session store and asserts what a subsequent delegation would read, using the same
 * {@link EventFilter} the contributor builds. Standing up a real {@code ChatModel} would add nothing — the LLM is not
 * the behavior under test, and key isolation plus replay is backend-independent, so the in-memory repository is used
 * rather than Testcontainers.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class SubAgentSessionMemoryContinuityTest {

    private static final String THREAD_ID = "thread-1";
    private static final String PROJECT_DEPLOYMENT_AGENT = "project_deployment_agent";
    private static final String MCP_AGENT = "mcp_agent";

    private AiHubSessionMemory aiHubSessionMemory;

    @BeforeEach
    void setUp() {
        aiHubSessionMemory = new AiHubSessionMemory(
            InMemorySessionRepository.builder()
                .build(),
            null);
    }

    @Test
    void testSecondDelegationSeesTheFirstExchange() {
        String sessionId = SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, PROJECT_DEPLOYMENT_AGENT);

        appendExchange(sessionId, "deploy version 3 to staging", "Deployed version 3 to staging.");

        List<SessionEvent> events = aiHubSessionMemory.sessionService()
            .getEvents(sessionId, EventFilter.lastN(SubAgentSessionMemoryContributor.MAX_EVENTS));

        assertThat(events).hasSize(2);
        assertThat(
            events.get(1)
                .getMessage()
                .getText()).contains("Deployed version 3");
    }

    @Test
    void testDifferentSpecialistsOnOneThreadDoNotSeeEachOther() {
        appendExchange(
            SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, PROJECT_DEPLOYMENT_AGENT),
            "roll back the staging deployment", "Rolled back to version 2.");

        List<SessionEvent> otherSpecialistEvents = aiHubSessionMemory.sessionService()
            .getEvents(
                SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, MCP_AGENT),
                EventFilter.lastN(SubAgentSessionMemoryContributor.MAX_EVENTS));

        assertThat(otherSpecialistEvents).isEmpty();
    }

    @Test
    void testOneSpecialistOnDifferentThreadsDoesNotCrossConversations() {
        appendExchange(
            SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, PROJECT_DEPLOYMENT_AGENT),
            "roll back the staging deployment", "Rolled back to version 2.");

        List<SessionEvent> otherConversationEvents = aiHubSessionMemory.sessionService()
            .getEvents(
                SubAgentSessionMemoryContributor.sessionKey("thread-2", PROJECT_DEPLOYMENT_AGENT),
                EventFilter.lastN(SubAgentSessionMemoryContributor.MAX_EVENTS));

        assertThat(otherConversationEvents).isEmpty();
    }

    @Test
    void testReplayIsCappedAtMaxEvents() {
        String sessionId = SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, PROJECT_DEPLOYMENT_AGENT);

        for (int exchangeIndex = 0; exchangeIndex < SubAgentSessionMemoryContributor.MAX_EVENTS; exchangeIndex++) {
            appendExchange(sessionId, "request " + exchangeIndex, "reply " + exchangeIndex);
        }

        List<SessionEvent> events = aiHubSessionMemory.sessionService()
            .getEvents(sessionId, EventFilter.lastN(SubAgentSessionMemoryContributor.MAX_EVENTS));

        // The window caps what is replayed; every event written stays in the store (see the spec — the read filter
        // is deliberately not compaction).
        assertThat(events).hasSize(SubAgentSessionMemoryContributor.MAX_EVENTS);

        List<SessionEvent> allEvents = aiHubSessionMemory.sessionService()
            .getEvents(sessionId, EventFilter.all());

        assertThat(allEvents).hasSize(SubAgentSessionMemoryContributor.MAX_EVENTS * 2);
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

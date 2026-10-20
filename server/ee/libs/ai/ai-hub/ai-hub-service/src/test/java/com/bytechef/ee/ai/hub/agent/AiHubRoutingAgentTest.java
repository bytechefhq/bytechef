/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.event.RunStartedEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiHubRoutingAgent}: each branch of the dispatch decision, including a missing chat, a missing
 * thread id and an absent bridge bean.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubRoutingAgentTest {

    private static final String AGENT_ID = "ai_hub_ask";
    private static final String THREAD_ID = "thread-1";

    private AiHubSpringAIAgent llmAgent;
    private WebhookBridgeAgent webhookBridgeAgent;
    private AiHubChatService chatService;
    private AgentSubscriber subscriber;

    @BeforeEach
    void setUp() {
        llmAgent = mock(AiHubSpringAIAgent.class);
        webhookBridgeAgent = mock(WebhookBridgeAgent.class);
        chatService = mock(AiHubChatService.class);
        subscriber = mock(AgentSubscriber.class);

        when(llmAgent.runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(webhookBridgeAgent.runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void testWorkflowChatDispatchesToBridgeAgent() throws AGUIException {
        stubChatKind(AiHubChatKind.WORKFLOW_CHAT);

        AiHubRoutingAgent agent = new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, chatService);

        agent.runAgent(parametersOf(THREAD_ID), subscriber)
            .join();

        verify(webhookBridgeAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        verify(llmAgent, never()).runAgent(any(), any());
    }

    @Test
    void testStandardChatDispatchesToLlmAgent() throws AGUIException {
        stubChatKind(AiHubChatKind.STANDARD);

        AiHubRoutingAgent agent = new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, chatService);

        agent.runAgent(parametersOf(THREAD_ID), subscriber)
            .join();

        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        verify(webhookBridgeAgent, never()).runAgent(any(), any());
    }

    @Test
    void testMissingChatFallsBackToLlmAgent() throws AGUIException {
        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.empty());

        AiHubRoutingAgent agent = new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, chatService);

        agent.runAgent(parametersOf(THREAD_ID), subscriber)
            .join();

        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        verify(webhookBridgeAgent, never()).runAgent(any(), any());
    }

    @Test
    void testWorkflowChatEndsWithARunErrorWhenBridgeBeanAbsent() throws Exception {
        stubChatKind(AiHubChatKind.WORKFLOW_CHAT);

        RecordingSubscriber recordingSubscriber = new RecordingSubscriber();
        AiHubRoutingAgent agent = new AiHubRoutingAgent(AGENT_ID, llmAgent, null, chatService);

        agent.runAgent(parametersOf(THREAD_ID), recordingSubscriber)
            .get(10, TimeUnit.SECONDS);

        assertThat(recordingSubscriber.events).hasSize(2);
        assertThat(recordingSubscriber.events.getFirst()).isInstanceOf(RunStartedEvent.class);
        assertThat(recordingSubscriber.events.getLast()).isInstanceOfSatisfying(
            RunErrorEvent.class, runErrorEvent -> assertThat(runErrorEvent.getError()).contains("Workflow chats"));
        assertThat(recordingSubscriber.finalized).isTrue();
        verify(llmAgent, never()).runAgent(any(), any());
    }

    @Test
    void testNullThreadIdRoutesToLlmAgentWithoutLookup() throws AGUIException {
        AiHubRoutingAgent agent = new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, chatService);

        agent.runAgent(parametersOf(null), subscriber)
            .join();

        verify(chatService, never()).findByThreadId(any());
        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
    }

    @Test
    void testRouterCarriesTheAgentIdItWasGiven() throws AGUIException {
        AiHubRoutingAgent agent = new AiHubRoutingAgent("ai_hub_build", llmAgent, webhookBridgeAgent, chatService);

        assertThat(agent.getAgentId()).isEqualTo("ai_hub_build");
    }

    private void stubChatKind(AiHubChatKind kind) {
        AiHubChat chat = mock(AiHubChat.class);

        when(chat.getKind()).thenReturn(kind);
        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));
    }

    private static RunAgentParameters parametersOf(@Nullable String threadId) {
        RunAgentParameters.Builder builder = RunAgentParameters.builder()
            .runId("run-1")
            .messages(List.<BaseMessage>of());

        if (threadId != null) {
            builder.threadId(threadId);
        }

        return builder.build();
    }

    private static final class RecordingSubscriber implements AgentSubscriber {

        private final List<BaseEvent> events = new CopyOnWriteArrayList<>();

        private volatile boolean finalized;

        @Override
        public void onEvent(BaseEvent event) {
            events.add(event);
        }

        @Override
        public void onRunFinalized(AgentSubscriberParams params) {
            finalized = true;
        }
    }
}

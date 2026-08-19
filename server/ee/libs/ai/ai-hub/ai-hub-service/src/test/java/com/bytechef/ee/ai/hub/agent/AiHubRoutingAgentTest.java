/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiHubRoutingAgent}. The router's only behaviour worth pinning is "the right kind goes to the
 * right agent" — but the regression cost of getting that wrong is huge (every chat routes to the wrong agent and unit
 * tests of the leaf agents wouldn't catch it). The tests below pin every branch of the dispatch decision tree,
 * including the fallback paths that fire when the bridge bean is absent or the chat lookup fails.
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
    private AssetFileFacade assetFileFacade;

    @BeforeEach
    void setUp() {
        llmAgent = mock(AiHubSpringAIAgent.class);
        webhookBridgeAgent = mock(WebhookBridgeAgent.class);
        chatService = mock(AiHubChatService.class);
        subscriber = mock(AgentSubscriber.class);
        assetFileFacade = mock(AssetFileFacade.class);

        when(llmAgent.runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(webhookBridgeAgent.runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void testWorkflowChatChatDispatchesToBridgeAgent() throws AGUIException {
        AiHubChat chat = mock(AiHubChat.class);

        when(chat.getKind()).thenReturn(AiHubChatKind.WORKFLOW_CHAT);
        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        AiHubRoutingAgent agent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, chatService, assetFileFacade);

        agent.runAgent(parametersOf(THREAD_ID), subscriber)
            .join();

        verify(webhookBridgeAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        verify(llmAgent, never()).runAgent(any(), any());
    }

    @Test
    void testAgentChatDispatchesToBridgeAgent() throws AGUIException {
        // An agent chat is webhook-bridged exactly like a workflow chat — it binds to the workflow inside the agent's
        // hidden __AI_AGENT__ project. If this ever routed to the LLM agent instead, the user would get a plausible
        // AI Hub answer in place of their agent's actual run, with nothing in the logs to say so.
        AiHubChat chat = mock(AiHubChat.class);

        when(chat.getKind()).thenReturn(AiHubChatKind.AGENT_CHAT);
        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        AiHubRoutingAgent agent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, chatService, assetFileFacade);

        agent.runAgent(parametersOf(THREAD_ID), subscriber)
            .join();

        verify(webhookBridgeAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        verify(llmAgent, never()).runAgent(any(), any());
    }

    @Test
    void testStandardChatDispatchesToLlmAgent() throws AGUIException {
        AiHubChat chat = mock(AiHubChat.class);

        when(chat.getKind()).thenReturn(AiHubChatKind.STANDARD);
        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        AiHubRoutingAgent agent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, chatService, assetFileFacade);

        agent.runAgent(parametersOf(THREAD_ID), subscriber)
            .join();

        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        verify(webhookBridgeAgent, never()).runAgent(any(), any());
    }

    @Test
    void testMissingChatFallsBackToLlmAgent() throws AGUIException {
        // When the threadId doesn't map to a chat row (race with delete, unknown threadId), the router
        // falls through to the LLM agent rather than throwing — the LLM agent's own error handling will
        // surface the missing-chat case if it matters. Pin: bridge is NOT invoked.
        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.empty());

        AiHubRoutingAgent agent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, chatService, assetFileFacade);

        agent.runAgent(parametersOf(THREAD_ID), subscriber)
            .join();

        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        verify(webhookBridgeAgent, never()).runAgent(any(), any());
    }

    @Test
    void testWorkflowChatFallsBackToLlmAgentWhenBridgeBeanAbsent() throws AGUIException {
        // Deployments without the webhook coordinator don't register a WebhookBridgeAgent bean. Without the
        // fallback the user would stare at a hung stream forever. Falling back to the LLM agent surfaces
        // SOMETHING — sub-optimal but recoverable. Pin: WORKFLOW_CHAT + null bridge → LLM agent.
        AiHubChat chat = mock(AiHubChat.class);

        when(chat.getKind()).thenReturn(AiHubChatKind.WORKFLOW_CHAT);
        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        AiHubRoutingAgent agent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, null, chatService, assetFileFacade);

        agent.runAgent(parametersOf(THREAD_ID), subscriber)
            .join();

        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
    }

    @Test
    void testNullThreadIdRoutesToLlmAgentWithoutLookup() throws AGUIException {
        // Defensive: a malformed RunAgentParameters with no threadId should NOT trigger a database lookup —
        // findByThreadId(null) would explode. The router's resolveKind short-circuits on blank/null threadId
        // and returns STANDARD (the safe default). Pin: chatService is NEVER called.
        AiHubRoutingAgent agent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, chatService, assetFileFacade);

        agent.runAgent(parametersOf(null), subscriber)
            .join();

        verify(chatService, never()).findByThreadId(any());
        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
    }

    private static RunAgentParameters parametersOf(String threadId) {
        RunAgentParameters.Builder builder = RunAgentParameters.builder()
            .runId("run-1")
            .messages(List.<BaseMessage>of());

        if (threadId != null) {
            builder.threadId(threadId);
        }

        return builder.build();
    }
}

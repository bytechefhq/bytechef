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
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.message.BaseMessage;
import com.agui.server.LocalAgent;
import com.bytechef.ee.ai.hub.approval.AiHubContinuationStarter;
import com.bytechef.ee.ai.hub.approval.AiHubToolApproval;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubContinuationStarterTest {

    private static final long OWNER_USER_ID = 1L;
    private static final long REQUESTER_USER_ID = 3L;
    private static final long WORKSPACE_ID = 7L;
    private static final String THREAD_ID = "thread-1";

    @Test
    void testStartContinuationRunsTheModesAgentWithTheVerifiedKeys() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        LocalAgent askAgent = agent("ai_hub_ask");
        LocalAgent buildAgent = agent("ai_hub_build");

        AiHubContinuationStarter continuationStarter = new AiHubContinuationStarterImpl(
            new AiHubChatStreamer(registry, new JsonMapper()), registry, List.of(askAgent, buildAgent));

        Optional<String> runId = continuationStarter.startContinuation(
            approval("BUILD"), chat(), WORKSPACE_ID, "[tool-approval #5] approved");

        assertThat(runId).isPresent();
        assertThat(registry.isInFlight(THREAD_ID)).isTrue();

        ArgumentCaptor<RunAgentParameters> parametersCaptor = ArgumentCaptor.forClass(RunAgentParameters.class);

        verify(buildAgent).runAgent(parametersCaptor.capture(), any(AgentSubscriber.class));
        verify(askAgent, never()).runAgent(any(), any());

        RunAgentParameters parameters = parametersCaptor.getValue();
        Map<String, Object> stateMap = parameters.getState()
            .getState();

        assertThat(parameters.getThreadId()).isEqualTo(THREAD_ID);
        assertThat(parameters.getRunId()).isEqualTo(runId.get());
        assertThat(parameters.getTools()).isEmpty();
        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_THREAD_ID)).isEqualTo(THREAD_ID);
        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_WORKSPACE_ID)).isEqualTo(WORKSPACE_ID);
        assertThat(stateMap.get(AiHubStateKeys.AUTHENTICATED_USER_ID)).isEqualTo(REQUESTER_USER_ID);
        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_OWNER_USER_ID)).isEqualTo(OWNER_USER_ID);
        assertThat(stateMap.get("mode")).isEqualTo("BUILD");

        List<BaseMessage> messages = parameters.getMessages();

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst()
            .getContent()).isEqualTo("[tool-approval #5] approved");
    }

    @Test
    void testStartContinuationReturnsEmptyWhileATurnIsInFlight() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        LocalAgent askAgent = agent("ai_hub_ask");

        registry.tryStart(THREAD_ID, "run-running");

        AiHubContinuationStarter continuationStarter = new AiHubContinuationStarterImpl(
            new AiHubChatStreamer(registry, new JsonMapper()), registry, List.of(askAgent));

        assertThat(continuationStarter.startContinuation(approval("ASK"), chat(), WORKSPACE_ID, "message"))
            .isEmpty();

        verify(askAgent, never()).runAgent(any(), any());
    }

    @Test
    void testStartContinuationReturnsEmptyWhenTheModesAgentIsNotRegistered() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();

        AiHubContinuationStarter continuationStarter = new AiHubContinuationStarterImpl(
            new AiHubChatStreamer(registry, new JsonMapper()), registry, List.of(agent("ai_hub_ask")));

        assertThat(continuationStarter.startContinuation(approval("BUILD"), chat(), WORKSPACE_ID, "message"))
            .isEmpty();
        assertThat(registry.isInFlight(THREAD_ID)).isFalse();
    }

    private static LocalAgent agent(String agentId) {
        LocalAgent agent = mock(LocalAgent.class);

        when(agent.getAgentId()).thenReturn(agentId);

        return agent;
    }

    private static AiHubToolApproval approval(String mode) {
        AiHubToolApproval approval = mock(AiHubToolApproval.class);

        when(approval.getMode()).thenReturn(mode);
        when(approval.getRequestedByUserId()).thenReturn(REQUESTER_USER_ID);
        when(approval.getEnvironment()).thenReturn(0);

        return approval;
    }

    private static AiHubChat chat() {
        AiHubChat chat = new AiHubChat(OWNER_USER_ID);

        chat.setId(100L);
        chat.setThreadId(THREAD_ID);
        chat.setWorkspaceId(WORKSPACE_ID);

        return chat;
    }
}

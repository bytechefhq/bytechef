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

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.state.State;
import com.agui.core.tool.Tool;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry.InFlightRun;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatStreamerTest {

    @Test
    void testStreamRunsTheAgentOnTheRunsThreadWithoutClientSuppliedTools() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        AiHubChatStreamer streamer = new AiHubChatStreamer(registry, new JsonMapper());
        LocalAgent agent = mock(LocalAgent.class);

        AgUiParameters agUiParameters = buildAgUiParameters();

        agUiParameters.setThreadId("client-thread");
        agUiParameters.setTools(List.of(new Tool("clientTool", "client tool", null)));

        InFlightRun run = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        streamer.stream(agent, agUiParameters, run);

        ArgumentCaptor<RunAgentParameters> parametersCaptor = ArgumentCaptor.forClass(RunAgentParameters.class);

        verify(agent).runAgent(parametersCaptor.capture(), any(AgentSubscriber.class));

        RunAgentParameters parameters = parametersCaptor.getValue();

        assertThat(parameters.getTools()).isEmpty();
        assertThat(parameters.getThreadId()).isEqualTo("thread-1");
        assertThat(parameters.getRunId()).isEqualTo("run-1");
    }

    @Test
    void testStreamDoesNotRunTheAgentForARunCancelledBeforeItStarted() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        AiHubChatStreamer streamer = new AiHubChatStreamer(registry, new JsonMapper());
        LocalAgent agent = mock(LocalAgent.class);

        registry.cancel("thread-1", "run-1");

        InFlightRun run = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        streamer.stream(agent, buildAgUiParameters(), run);

        verify(agent, never()).runAgent(any(), any());
    }

    @Test
    void testFinishedAgentRunReleasesTheThread() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        AiHubChatStreamer streamer = new AiHubChatStreamer(registry, new JsonMapper());
        LocalAgent agent = mock(LocalAgent.class);

        InFlightRun run = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        streamer.start(agent, buildAgUiParameters(), run);

        ArgumentCaptor<AgentSubscriber> subscriberCaptor = ArgumentCaptor.forClass(AgentSubscriber.class);

        verify(agent).runAgent(any(RunAgentParameters.class), subscriberCaptor.capture());

        assertThat(registry.isInFlight("thread-1")).isTrue();

        subscriberCaptor.getValue()
            .onRunFinalized(null);

        assertThat(registry.isInFlight("thread-1")).isFalse();
    }

    private static AgUiParameters buildAgUiParameters() {
        AgUiParameters agUiParameters = new AgUiParameters();

        agUiParameters.setThreadId("thread-1");
        agUiParameters.setRunId("run-1");
        agUiParameters.setState(new State());

        return agUiParameters;
    }
}

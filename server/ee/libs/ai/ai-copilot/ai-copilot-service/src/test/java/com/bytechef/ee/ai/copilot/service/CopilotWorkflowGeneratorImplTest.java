/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentParameters;
import com.agui.server.LocalAgent;
import com.bytechef.ee.ai.copilot.util.CopilotStateKeys;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CopilotWorkflowGeneratorImplTest {

    @Test
    void testSeedsTenantIntoState() {
        AtomicReference<Map<String, Object>> capturedState = new AtomicReference<>();

        LocalAgent stubAgent = mock(LocalAgent.class);

        when(stubAgent.getAgentId()).thenReturn("workflow_editor_build");

        when(stubAgent.runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class)))
            .thenAnswer(invocation -> {
                RunAgentParameters runAgentParameters = invocation.getArgument(0);
                AgentSubscriber agentSubscriber = invocation.getArgument(1);

                capturedState.set(runAgentParameters.getState()
                    .getState());

                agentSubscriber.onRunFinalized(new AgentSubscriberParams(List.of(), null, stubAgent, null));

                return CompletableFuture.completedFuture(null);
            });

        CopilotWorkflowGeneratorImpl generator = new CopilotWorkflowGeneratorImpl(List.of(stubAgent));

        TenantContext.runWithTenantId("acme", () -> generator.generateWorkflow("wf-1", "build a thing", Set.of()));

        assertThat(capturedState.get()).containsEntry(CopilotStateKeys.STATE_TENANT_ID, "acme");
    }
}

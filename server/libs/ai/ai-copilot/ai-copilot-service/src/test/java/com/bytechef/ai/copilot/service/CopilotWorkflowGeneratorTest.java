/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.copilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentParameters;
import com.agui.server.LocalAgent;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class CopilotWorkflowGeneratorTest {

    @Test
    void testGenerateWorkflowPutsNonBlankSystemPromptIntoState() {
        LocalAgent localAgent = newCompletingAgent();

        CopilotWorkflowGeneratorImpl generator = new CopilotWorkflowGeneratorImpl(List.of(localAgent));

        generator.generateWorkflow("wf-1", "Build it", "Prefer Slack.", Set.of("slack"));

        Map<String, Object> stateMap = captureState(localAgent);

        assertThat(stateMap).containsEntry(CopilotConstants.STATE_ADDITIONAL_SYSTEM_PROMPT, "Prefer Slack.");
    }

    @Test
    void testGenerateWorkflowOmitsBlankSystemPromptFromState() {
        LocalAgent localAgent = newCompletingAgent();

        CopilotWorkflowGeneratorImpl generator = new CopilotWorkflowGeneratorImpl(List.of(localAgent));

        generator.generateWorkflow("wf-1", "Build it", "   ", Set.of("slack"));

        Map<String, Object> stateMap = captureState(localAgent);

        assertThat(stateMap).doesNotContainKey(CopilotConstants.STATE_ADDITIONAL_SYSTEM_PROMPT);
    }

    private static LocalAgent newCompletingAgent() {
        LocalAgent localAgent = mock(LocalAgent.class);

        when(localAgent.getAgentId()).thenReturn("workflow_editor_build");

        // The generator blocks on a CountDownLatch released by onRunFinalized; release it synchronously so the
        // unit test does not wait on the real 10-minute timeout.
        doAnswer(invocation -> {
            AgentSubscriber subscriber = invocation.getArgument(1);

            subscriber.onRunFinalized(null);

            return null;
        }).when(localAgent)
            .runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));

        return localAgent;
    }

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

        TenantContext.runWithTenantId("acme",
            () -> generator.generateWorkflow("wf-1", "build a thing", null, Set.of()));

        assertThat(capturedState.get()).containsEntry(CopilotConstants.STATE_TENANT_ID, "acme");
    }

    private static Map<String, Object> captureState(LocalAgent localAgent) {
        ArgumentCaptor<RunAgentParameters> parametersCaptor = ArgumentCaptor.forClass(RunAgentParameters.class);

        verify(localAgent).runAgent(parametersCaptor.capture(), any(AgentSubscriber.class));

        return parametersCaptor.getValue()
            .getState()
            .getState();
    }
}

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
import com.agui.core.agent.RunAgentParameters;
import com.agui.server.LocalAgent;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class CopilotSkillGeneratorTest {

    @Test
    void testSeedsEnvironmentIntoState() {
        LocalAgent localAgent = newCompletingAgent();

        CopilotSkillGeneratorImpl generator = new CopilotSkillGeneratorImpl(List.of(localAgent));

        // Non-default ordinal (STAGING) proves the caller-supplied environment is threaded into the agent state as a
        // Long, so CopilotSpringAIAgent.runWithEnvironment and SkillsSpringAIAgent.advisorParams bind it rather than
        // falling back to PRODUCTION.
        TenantContext.runWithTenantId("acme", () -> generator.generateSkill(42L, "Build a skill", 1));

        Map<String, Object> stateMap = captureState(localAgent);

        assertThat(stateMap).containsEntry(CopilotConstants.STATE_ENVIRONMENT_ID, 1L)
            .containsEntry("currentSelectedSkillId", 42L)
            .containsEntry(CopilotConstants.STATE_TENANT_ID, "acme");
    }

    private static LocalAgent newCompletingAgent() {
        LocalAgent localAgent = mock(LocalAgent.class);

        when(localAgent.getAgentId()).thenReturn("skills_build");

        // The generator blocks on a CountDownLatch released by onRunFinalized; release it synchronously so the unit
        // test does not wait on the real 10-minute timeout.
        doAnswer(invocation -> {
            AgentSubscriber subscriber = invocation.getArgument(1);

            subscriber.onRunFinalized(null);

            return null;
        }).when(localAgent)
            .runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));

        return localAgent;
    }

    private static Map<String, Object> captureState(LocalAgent localAgent) {
        ArgumentCaptor<RunAgentParameters> parametersCaptor = ArgumentCaptor.forClass(RunAgentParameters.class);

        verify(localAgent).runAgent(parametersCaptor.capture(), any(AgentSubscriber.class));

        return parametersCaptor.getValue()
            .getState()
            .getState();
    }
}

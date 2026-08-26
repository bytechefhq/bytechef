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

package com.bytechef.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Guards the two CE agents that were moved from extending {@code SpringAIAgent} directly onto
 * {@link CopilotSpringAIAgent}, so that a future edit cannot quietly re-divide the family and take these two back out
 * of the tenant seam.
 *
 * <p>
 * Reparenting also gave them the {@link EnvironmentContext} binding the rest of the family already had, and that is a
 * real behaviour change rather than a no-op: the client sends {@code environmentId} on every copilot turn
 * ({@code CopilotRuntimeProvider}, defaulting to {@code 0} = {@link Environment#DEVELOPMENT}), and
 * {@code CopilotChatFacadeImpl} does not overwrite it. These agents previously resolved their chat model against an
 * unbound {@code EnvironmentContext} — which falls back to {@link Environment#PRODUCTION} — and now resolve it against
 * the environment the caller is actually in.
 *
 * <p>
 * That is the fix rather than a regression: AI providers are activated per environment, and the tools of these same
 * agents already ran in the caller's environment ({@code CopilotToolContextUtils} →
 * {@code RehydrateContextToolCallback.withEnvironment}). An agent resolving a PRODUCTION model while its own tools ran
 * against DEVELOPMENT was the bug. The tests below pin the binding so the change stays visible if it is ever reverted
 * or altered.
 *
 * @author Ivica Cardic
 */
class ReparentedCopilotAgentContextTest {

    @BeforeEach
    @AfterEach
    void resetContexts() {
        EnvironmentContext.clear();
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testWorkflowCodeEditorAgentIsOnTheSharedBase() throws AGUIException {
        assertThat(newWorkflowCodeEditorAgent()).isInstanceOf(CopilotSpringAIAgent.class);
    }

    @Test
    void testWorkflowExecutionAgentIsOnTheSharedBase() throws AGUIException {
        assertThat(newWorkflowExecutionAgent()).isInstanceOf(CopilotSpringAIAgent.class);
    }

    @Test
    void testWorkflowCodeEditorAgentBindsTheCarriedTenant() throws AGUIException {
        assertBindsTenant(newWorkflowCodeEditorAgent());
    }

    @Test
    void testWorkflowExecutionAgentBindsTheCarriedTenant() throws AGUIException {
        assertBindsTenant(newWorkflowExecutionAgent());
    }

    @Test
    void testWorkflowCodeEditorAgentBindsTheCarriedEnvironment() throws AGUIException {
        assertBindsEnvironment(newWorkflowCodeEditorAgent());
    }

    @Test
    void testWorkflowExecutionAgentBindsTheCarriedEnvironment() throws AGUIException {
        assertBindsEnvironment(newWorkflowExecutionAgent());
    }

    private static void assertBindsTenant(CopilotSpringAIAgent agent) {
        State state = new State();

        state.set(CopilotConstants.STATE_TENANT_ID, "acme");

        AtomicReference<String> capturedTenantId = new AtomicReference<>();

        agent.runWithTenant(newInput(state), () -> capturedTenantId.set(TenantContext.getCurrentTenantId()));

        assertThat(capturedTenantId.get()).isEqualTo("acme");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    /**
     * {@code DEVELOPMENT} rather than {@code PRODUCTION} is the whole point: {@code 0} is what the client sends when it
     * has no environment of its own, and it is what these two agents would previously have ignored.
     */
    private static void assertBindsEnvironment(CopilotSpringAIAgent agent) {
        // Seeded as a long where the client sends the string "0"; NumberUtils.asLong parses both to the same value, so
        // the path under test is identical. Do not "correct" this to a long on the assumption that is what production
        // sends -- it is not.

        State state = new State();

        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, 0L);

        AtomicReference<Environment> capturedEnvironment = new AtomicReference<>();

        agent.runWithEnvironment(
            newInput(state), () -> capturedEnvironment.set(EnvironmentContext.getCurrentEnvironment()));

        assertThat(capturedEnvironment.get()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
    }

    private static RunAgentInput newInput(State state) {
        return new RunAgentInput("thread", "run", state, List.of(), List.of(), List.of(), null);
    }

    private static CopilotSpringAIAgent newWorkflowCodeEditorAgent() throws AGUIException {
        return WorkflowCodeEditorSpringAIAgent.builder()
            .agentId("workflow_code_editor_ask")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State())
            .build();
    }

    private static CopilotSpringAIAgent newWorkflowExecutionAgent() throws AGUIException {
        return WorkflowExecutionSpringAIAgent.builder()
            .agentId("workflow_execution_ask")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State())
            .build();
    }
}

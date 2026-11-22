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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.context.Context;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.SystemMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.tenant.TenantContext;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.task.TaskExecutor;

/**
 * @author Ivica Cardic
 */
class CopilotSpringAIAgentTenantContextTest {

    private static final String CALLER_TENANT_ID = "acme";

    @BeforeEach
    @AfterEach
    void resetTenantContext() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testRunBindsTheCarriedTenantOnTheAgentThread() throws AGUIException {
        TenantCapturingAgent agent = newAgent();

        RunAgentInput input = newInput(stateWithTenantId(CALLER_TENANT_ID));

        runOnWorkerThread(agent, input);

        assertThat(agent.getCapturedTenantId()).isEqualTo(CALLER_TENANT_ID);
    }

    @Test
    void testRunLeavesTheWorkerThreadOnItsPreviousTenant() throws AGUIException {
        TenantCapturingAgent agent = newAgent();

        RunAgentInput input = newInput(stateWithTenantId(CALLER_TENANT_ID));

        runOnWorkerThread(agent, input);

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testRunRestoresAPreExistingTenant() throws AGUIException {
        TenantCapturingAgent agent = newAgent();

        TenantContext.setCurrentTenantId("other");

        agent.runWithTenant(newInput(stateWithTenantId(CALLER_TENANT_ID)), () -> {});

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("other");
    }

    @Test
    void testRunToleratesANestedBindingAroundToolCalls() throws AGUIException {
        TenantCapturingAgent agent = newAgent();

        AtomicReference<String> tenantIdAfterNestedBinding = new AtomicReference<>();

        agent.runWithTenant(newInput(stateWithTenantId(CALLER_TENANT_ID)), () -> {
            String previousTenantId = TenantContext.getCurrentTenantId();

            TenantContext.setCurrentTenantId("nested");
            TenantContext.setCurrentTenantId(previousTenantId);

            tenantIdAfterNestedBinding.set(TenantContext.getCurrentTenantId());
        });

        assertThat(tenantIdAfterNestedBinding.get()).isEqualTo(CALLER_TENANT_ID);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testRunLeavesTheAmbientTenantWhenStateCarriesNone() throws AGUIException {
        TenantCapturingAgent agent = newAgent();

        TenantContext.setCurrentTenantId("ambient");

        AtomicReference<String> tenantIdInsideAction = new AtomicReference<>();

        agent.runWithTenant(
            newInput(new State()), () -> tenantIdInsideAction.set(TenantContext.getCurrentTenantId()));

        assertThat(tenantIdInsideAction.get()).isEqualTo("ambient");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("ambient");
    }

    @Test
    void testRunIsSealedAgainstOverrides() throws NoSuchMethodException {
        Method runMethod = CopilotSpringAIAgent.class.getDeclaredMethod(
            "run", RunAgentInput.class, AgentSubscriber.class);

        assertThat(Modifier.isFinal(runMethod.getModifiers()))
            .as("CopilotSpringAIAgent.run must stay final; use decorateSubscriber or another hook instead")
            .isTrue();
    }

    @Test
    void testConverterAgentStillDecoratesTheSubscriber() throws AGUIException {
        CopilotSpringAIAgent agent = ConverterSpringAIAgent.builder()
            .agentId("converter_build")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State())
            .build();

        AgentSubscriber subscriber = new AgentSubscriber() {};

        assertThat(agent.decorateSubscriber(subscriber)).isNotSameAs(subscriber);
    }

    private static void runOnWorkerThread(TenantCapturingAgent agent, RunAgentInput input) {
        TaskExecutor taskExecutor = workerThreadExecutor();

        assertThatThrownBy(() -> taskExecutor.execute(() -> agent.run(input, new AgentSubscriber() {})))
            .isInstanceOf(SystemMessageReachedException.class);
    }

    private static TaskExecutor workerThreadExecutor() {
        return runnable -> {
            TenantContext.resetCurrentTenantId();

            runnable.run();
        };
    }

    private static State stateWithTenantId(String tenantId) {
        State state = new State();

        state.set(CopilotConstants.STATE_TENANT_ID, tenantId);

        return state;
    }

    private static RunAgentInput newInput(State state) {
        UserMessage userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("build me a workflow");

        List<BaseMessage> messages = new ArrayList<>();

        messages.add(userMessage);

        return new RunAgentInput("thread", "run", state, messages, List.of(), List.of(), null);
    }

    private static TenantCapturingAgent newAgent() throws AGUIException {
        SpringAIAgent.Builder builder = SpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State());

        return new TenantCapturingAgent(builder);
    }

    private static final class TenantCapturingAgent extends CopilotSpringAIAgent {

        private final AtomicReference<String> capturedTenantId = new AtomicReference<>();

        private TenantCapturingAgent(SpringAIAgent.Builder builder) throws AGUIException {
            super(builder, null);
        }

        private @Nullable String getCapturedTenantId() {
            return capturedTenantId.get();
        }

        @Override
        protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
            capturedTenantId.set(TenantContext.getCurrentTenantId());

            throw new SystemMessageReachedException();
        }
    }

    private static final class SystemMessageReachedException extends RuntimeException {
    }
}

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
 * Covers the tenant binding {@link CopilotSpringAIAgent#run} applies on the agent's worker thread.
 *
 * <p>
 * In production that thread comes from {@code ForkJoinPool.commonPool()}, because {@code LocalAgent.runAgent} hands the
 * agent body to a bare {@code CompletableFuture.runAsync}. {@link #workerThreadExecutor()} stands in for it: it runs
 * synchronously — so the assertions are deterministic with no threads and nothing to wait on — but resets
 * {@link TenantContext} first, so it cannot quietly keep the calling thread's tenant alive and pass a test that a real
 * worker would fail.
 *
 * @author Ivica Cardic
 */
class CopilotSpringAIAgentTenantContextTest {

    private static final String CALLER_TENANT_ID = "acme";

    @BeforeEach
    @AfterEach
    void resetTenantContext() {
        TenantContext.resetCurrentTenantId();
    }

    /**
     * The agent body — not merely the tool calls — must see the tenant the request thread captured. The capture point
     * is {@code createSystemMessage}, which is where {@code WorkflowEditorSpringAIAgent} calls
     * {@code workflowService.getWorkflow(workflowId)}.
     */
    @Test
    void testRunBindsTheCarriedTenantOnTheAgentThread() throws AGUIException {
        TenantCapturingAgent agent = newAgent();

        RunAgentInput input = newInput(stateWithTenantId(CALLER_TENANT_ID));

        runOnWorkerThread(agent, input);

        assertThat(agent.getCapturedTenantId()).isEqualTo(CALLER_TENANT_ID);
    }

    /**
     * The common pool is shared, so a binding left behind would contaminate whatever runs next on that thread.
     */
    @Test
    void testRunLeavesTheWorkerThreadOnItsPreviousTenant() throws AGUIException {
        TenantCapturingAgent agent = newAgent();

        RunAgentInput input = newInput(stateWithTenantId(CALLER_TENANT_ID));

        runOnWorkerThread(agent, input);

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    /**
     * A worker that already carries someone else's tenant gets it back, not the default — the same save/restore shape
     * {@code RehydrateContextToolCallback} relies on when it nests its own binding inside this one.
     */
    @Test
    void testRunRestoresAPreExistingTenant() throws AGUIException {
        TenantCapturingAgent agent = newAgent();

        TenantContext.setCurrentTenantId("other");

        agent.runWithTenant(newInput(stateWithTenantId(CALLER_TENANT_ID)), () -> {});

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("other");
    }

    /**
     * A nested binding that restores on its way out — what {@code RehydrateContextToolCallback} does around each tool
     * call — must leave the agent's own binding intact and the thread clean at the end.
     */
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

    /**
     * An absent key must leave the ambient tenant alone rather than binding the default over it — binding
     * {@code "public"} here would make a producer that forgot to capture the tenant indistinguishable from one that
     * captured it correctly.
     */
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

    /**
     * The seam is only worth anything if it cannot be stepped around, and {@code isInstanceOf} assertions elsewhere are
     * only sufficient because of this: a subclass that overrode {@code run} and omitted {@code super.run} would bypass
     * both bindings while still passing every "is on the shared base" test. {@code final} makes that a compile error,
     * and this pins the {@code final} so it cannot be quietly dropped to make room for an override.
     */
    @Test
    void testRunIsSealedAgainstOverrides() throws NoSuchMethodException {
        Method runMethod = CopilotSpringAIAgent.class.getDeclaredMethod(
            "run", RunAgentInput.class, AgentSubscriber.class);

        assertThat(Modifier.isFinal(runMethod.getModifiers()))
            .as("CopilotSpringAIAgent.run must stay final; use decorateSubscriber or another hook instead")
            .isTrue();
    }

    /**
     * {@code ConverterSpringAIAgent} is the one agent that needed to wrap the run, and it did so with a {@code run}
     * override. Sealing {@code run} moved it onto {@link CopilotSpringAIAgent#decorateSubscriber}; this asserts the
     * decoration survived the move, so the hook is carrying the behaviour rather than merely existing.
     */
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

    /**
     * Synchronous, so the assertions are deterministic without waiting on anything, but with {@link TenantContext}
     * reset so it stands in for a real worker thread rather than quietly keeping the caller's tenant alive.
     */
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

    /**
     * Captures the tenant at the first point the real agents touch the database — {@code createSystemMessage} — and
     * then aborts the run, so no chat model is ever exercised.
     */
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

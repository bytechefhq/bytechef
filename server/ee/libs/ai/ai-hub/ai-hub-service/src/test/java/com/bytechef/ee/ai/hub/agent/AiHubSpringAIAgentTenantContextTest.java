/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

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
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.task.TaskExecutor;

/**
 * Covers the tenant binding {@link AiHubSpringAIAgent#run} applies on the agent's worker thread — the AI Hub twin of
 * {@code CopilotSpringAIAgentTenantContextTest}.
 *
 * <p>
 * {@link #workerThreadExecutor()} stands in for the {@code ForkJoinPool.commonPool()} worker
 * {@code LocalAgent.runAgent} dispatches onto: it runs synchronously, so the assertions are deterministic with no
 * threads and nothing to wait on, but resets {@link TenantContext} first so it cannot quietly keep the calling thread's
 * tenant alive.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubSpringAIAgentTenantContextTest {

    private static final String CALLER_TENANT_ID = "acme";

    @BeforeEach
    @AfterEach
    void resetTenantContext() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testRunBindsTheCarriedTenantOnTheAgentThread() throws AGUIException {
        AtomicReference<String> capturedTenantId = new AtomicReference<>();

        TenantCapturingAgent agent = newAgent(capturedTenantId);

        RunAgentInput input = newInput(stateWithTenantId(CALLER_TENANT_ID));

        TaskExecutor taskExecutor = workerThreadExecutor();

        assertThatThrownBy(() -> taskExecutor.execute(() -> agent.run(input, new AgentSubscriber() {})))
            .isInstanceOf(SystemMessageReachedException.class);

        assertThat(capturedTenantId.get()).isEqualTo(CALLER_TENANT_ID);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testBinderRestoresAPreExistingTenant() {
        TenantContext.setCurrentTenantId("other");

        AiHubAgentTenantBinder.runWithTenant("ai_hub", stateWithTenantId(CALLER_TENANT_ID), () -> {});

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("other");
    }

    @Test
    void testBinderLeavesTheAmbientTenantWhenStateCarriesNone() {
        TenantContext.setCurrentTenantId("ambient");

        AtomicReference<String> tenantIdInsideAction = new AtomicReference<>();

        AiHubAgentTenantBinder.runWithTenant(
            "ai_hub", new State(), () -> tenantIdInsideAction.set(TenantContext.getCurrentTenantId()));

        assertThat(tenantIdInsideAction.get()).isEqualTo("ambient");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("ambient");
    }

    private static TaskExecutor workerThreadExecutor() {
        return runnable -> {
            TenantContext.resetCurrentTenantId();

            runnable.run();
        };
    }

    private static State stateWithTenantId(String tenantId) {
        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_TENANT_ID, tenantId);

        return state;
    }

    private static RunAgentInput newInput(State state) {
        UserMessage userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("Draft a spec");

        List<BaseMessage> messages = new ArrayList<>();

        messages.add(userMessage);

        return new RunAgentInput("thread", "run", state, messages, List.of(), List.of(), null);
    }

    private static TenantCapturingAgent newAgent(AtomicReference<String> capturedTenantId) throws AGUIException {
        AiHubSpringAIAgent.Builder builder = AiHubSpringAIAgent.builder()
            .agentId("ai_hub")
            .chatModel(mock(ChatModel.class))
            .systemMessage("test")
            .state(new State());

        return new TenantCapturingAgent(builder, capturedTenantId);
    }

    private static final class TenantCapturingAgent extends AiHubSpringAIAgent {

        private final AtomicReference<String> capturedTenantId;

        private TenantCapturingAgent(Builder builder, AtomicReference<String> capturedTenantId) throws AGUIException {
            super(builder);

            this.capturedTenantId = capturedTenantId;
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

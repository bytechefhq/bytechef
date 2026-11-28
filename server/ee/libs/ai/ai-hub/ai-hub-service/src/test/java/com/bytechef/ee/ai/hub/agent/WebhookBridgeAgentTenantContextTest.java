/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.metric.WorkflowChatMetrics;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.core.task.TaskExecutor;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WebhookBridgeAgentTenantContextTest {

    private static final String CALLER_TENANT_ID = "acme";
    private static final String THREAD_ID = "00000000-0000-0000-0000-00000000004e";

    @BeforeEach
    @AfterEach
    void resetTenantContext() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testRunBindsTheCarriedTenantBeforeTheFirstDatabaseRead() throws AGUIException {
        AtomicReference<String> capturedTenantId = new AtomicReference<>();

        AiHubChatService chatService = mock(AiHubChatService.class);

        when(chatService.findByThreadId(THREAD_ID)).thenAnswer(invocation -> {
            capturedTenantId.set(TenantContext.getCurrentTenantId());

            return Optional.empty();
        });

        WebhookBridgeAgent agent = newAgent(chatService);

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_TENANT_ID, CALLER_TENANT_ID);

        RunAgentInput input = newInput(state);

        workerThreadExecutor().execute(() -> agent.run(input, new AgentSubscriber() {}));

        assertThat(capturedTenantId.get()).isEqualTo(CALLER_TENANT_ID);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    private static TaskExecutor workerThreadExecutor() {
        return runnable -> {
            TenantContext.resetCurrentTenantId();

            runnable.run();
        };
    }

    private static RunAgentInput newInput(State state) {
        UserMessage userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("hello");

        List<BaseMessage> messages = new ArrayList<>();

        messages.add(userMessage);

        return new RunAgentInput(THREAD_ID, "run-1", state, messages, List.of(), List.of(), null);
    }

    private static WebhookBridgeAgent newAgent(AiHubChatService chatService) throws AGUIException {
        WorkflowChatGuard guard = mock(WorkflowChatGuard.class);

        when(guard.tryAdmit(anyString())).thenReturn(WorkflowChatGuard.AdmissionResult.admit());

        return new WebhookBridgeAgent(
            mock(WebhookWorkflowExecutor.class), chatService, mock(WebhookResumeRegistry.class), JsonMapper.builder()
                .build(),
            mock(WorkflowChatMetrics.class), new AiHubSessionMemory(
                InMemorySessionRepository.builder()
                    .build(),
                null),
            guard, null, new InFlightAiHubRunRegistry());
    }
}

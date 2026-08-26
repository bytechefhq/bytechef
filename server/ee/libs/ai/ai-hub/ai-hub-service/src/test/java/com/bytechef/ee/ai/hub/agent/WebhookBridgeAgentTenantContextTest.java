/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
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

/**
 * {@link WebhookBridgeAgent} extends {@code LocalAgent} directly rather than {@link AiHubSpringAIAgent}, so it needs
 * its own tenant binding and its own guard against losing it. {@code AiHubRoutingAgent} routes every webhook-bridged
 * chat here, and the first statement of the bridged body is a database read.
 *
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

    /**
     * {@code chatService.findByThreadId} is the bridge's first database touch. Capturing the tenant inside the stub
     * asserts on the real {@code run} path, and returning empty makes the bridge take its "chat not found" early exit,
     * so the turn ends deterministically without a workflow execution.
     */
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

        when(guard.tryAdmit(anyLong())).thenReturn(WorkflowChatGuard.AdmissionResult.admit());

        return new WebhookBridgeAgent(
            mock(com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor.class), chatService,
            mock(WebhookResumeRegistry.class), tools.jackson.databind.json.JsonMapper.builder()
                .build(),
            mock(com.bytechef.automation.assetfile.service.AssetFileFacade.class), mock(WorkflowChatMetrics.class),
            mock(WorkflowChatJobRegistry.class),
            new AiHubSessionMemory(
                InMemorySessionRepository.builder()
                    .build(),
                null),
            guard, null);
    }
}

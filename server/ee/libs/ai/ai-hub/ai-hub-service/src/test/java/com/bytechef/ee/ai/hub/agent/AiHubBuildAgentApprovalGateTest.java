/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.bytechef.ee.ai.hub.approval.AiHubToolApproval;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalPolicy;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.config.AiHubAgentFixtures;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

/**
 * The approval gate passes a call straight through when the tool context cannot be attributed to a chat, so the gate
 * only works if the agent's own tool context always carries the thread, workspace and user. This drives the BUILD
 * agent's registered {@code deleteProject} callback with the tool context the agent builds for a turn and checks that
 * the call is held for approval instead of reaching the project facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiHubBuildAgentApprovalGateTest {

    private static final long CHAT_ID = 5L;
    private static final String THREAD_ID = "thread-1";
    private static final long USER_ID = 10L;
    private static final long WORKSPACE_ID = 7L;

    @AfterEach
    void resetTenantContext() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testDeleteProjectCalledWithTheAgentToolContextIsHeldForApproval() throws Exception {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        AiHubChat chat = new AiHubChat();

        chat.setId(CHAT_ID);
        chat.setThreadId(THREAD_ID);
        chat.setUserId(USER_ID);

        when(fixtures.chatService.findActiveByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));
        when(fixtures.policy.decide(WORKSPACE_ID)).thenReturn(new AiHubToolApprovalPolicy.Decision(Set.of(), Set.of()));
        when(fixtures.approvalService.findPending(CHAT_ID)).thenReturn(Optional.empty());
        when(fixtures.approvalService.createPending(any())).thenAnswer(invocation -> {
            AiHubToolApproval approval = invocation.getArgument(0);

            approval.setId(99L);

            return approval;
        });

        AiHubSpringAIAgent buildAgent = fixtures.buildAgent();

        Map<String, Object> toolContext = buildAgent.toolContext(newInput());

        ToolCallback deleteProject = buildAgent.toolCallbacks()
            .stream()
            .filter(toolCallback -> "deleteProject".equals(toolCallback.getToolDefinition()
                .name()))
            .findFirst()
            .orElseThrow();

        String result = deleteProject.call("{\"projectId\":42}", new ToolContext(toolContext));

        ArgumentCaptor<AiHubToolApproval> approvalCaptor = ArgumentCaptor.forClass(AiHubToolApproval.class);

        verify(fixtures.approvalService).createPending(approvalCaptor.capture());
        verify(fixtures.projectFacade, never()).deleteProject(anyLong());
        verify(fixtures.projectFacade, never()).getProject(anyLong());

        AiHubToolApproval approval = approvalCaptor.getValue();

        assertThat(approval.getStatus()).isEqualTo(AiHubToolApproval.Status.PENDING);
        assertThat(approval.getToolName()).isEqualTo("deleteProject");
        assertThat(approval.getChatId()).isEqualTo(CHAT_ID);
        assertThat(approval.getRequestedByUserId()).isEqualTo(USER_ID);
        assertThat(approval.getMode()).isEqualTo("BUILD");
        assertThat(result).contains("\"awaitingApproval\":true");
        assertThat(buildAgent.endsTurn(new ToolResponseMessage.ToolResponse("call-1", "deleteProject", result)))
            .isTrue();
    }

    private static RunAgentInput newInput() {
        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, WORKSPACE_ID);
        state.set(AiHubStateKeys.AUTHENTICATED_USER_ID, USER_ID);
        state.set(AiHubStateKeys.VERIFIED_THREAD_ID, THREAD_ID);
        state.set(AiHubStateKeys.VERIFIED_TENANT_ID, "public");

        UserMessage userMessage = new UserMessage();

        userMessage.setContent("Delete the Sales project");

        return new RunAgentInput(
            THREAD_ID, "run", state, List.of((BaseMessage) userMessage), List.of(), List.of(), null);
    }
}

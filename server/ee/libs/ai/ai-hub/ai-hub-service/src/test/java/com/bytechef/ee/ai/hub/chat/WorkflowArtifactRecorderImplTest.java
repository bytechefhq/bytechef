/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Unit tests for {@link WorkflowArtifactRecorderImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkflowArtifactRecorderImplTest {

    @Mock
    private AiHubChatArtifactService chatArtifactService;

    @Test
    void testDelegatesWhenConversationIdPresent() {
        WorkflowArtifactRecorderImpl recorder = new WorkflowArtifactRecorderImpl(chatArtifactService);

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(7L, 42L, 0L, "thread-1", null).toToolContext());

        recorder.recordWorkflowArtifact(toolContext, true, "wf-1", 9L, 55L, "My Flow");

        verify(chatArtifactService).recordWorkflowArtifact(
            eq("thread-1"), eq(42L), eq(AiHubChatArtifactKind.WORKFLOW_CREATED), eq("wf-1"), eq(9L), eq(55L),
            eq("My Flow"));
    }

    @Test
    void testMapsUpdateToWorkflowUpdatedKind() {
        WorkflowArtifactRecorderImpl recorder = new WorkflowArtifactRecorderImpl(chatArtifactService);

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(7L, 42L, 0L, "thread-1", null).toToolContext());

        recorder.recordWorkflowArtifact(toolContext, false, "wf-1", 9L, 55L, "My Flow");

        verify(chatArtifactService).recordWorkflowArtifact(
            eq("thread-1"), eq(42L), eq(AiHubChatArtifactKind.WORKFLOW_UPDATED), eq("wf-1"), eq(9L), eq(55L),
            eq("My Flow"));
    }

    @Test
    void testNoOpWhenNoConversationId() {
        WorkflowArtifactRecorderImpl recorder = new WorkflowArtifactRecorderImpl(chatArtifactService);

        // workspace/user present, but no conversation id -> not an AI Hub chat turn.
        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(7L, 42L, 0L, null, null).toToolContext());

        recorder.recordWorkflowArtifact(toolContext, true, "wf-1", 9L, 55L, "My Flow");

        verify(chatArtifactService, never()).recordWorkflowArtifact(
            any(), any(), any(), any(), anyLong(), any(), any());
    }

    @Test
    void testNoOpWhenToolContextNull() {
        WorkflowArtifactRecorderImpl recorder = new WorkflowArtifactRecorderImpl(chatArtifactService);

        recorder.recordWorkflowArtifact(null, true, "wf-1", 9L, 55L, "My Flow");

        verify(chatArtifactService, never()).recordWorkflowArtifact(
            any(), any(), any(), any(), anyLong(), any(), any());
    }

    @Test
    void testSwallowsServiceException() {
        WorkflowArtifactRecorderImpl recorder = new WorkflowArtifactRecorderImpl(chatArtifactService);

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(7L, 42L, 0L, "thread-1", null).toToolContext());

        doThrow(new RuntimeException("boom"))
            .when(chatArtifactService)
            .recordWorkflowArtifact(any(), any(), any(), any(), anyLong(), any(), any());

        assertThatCode(() -> recorder.recordWorkflowArtifact(toolContext, true, "wf-1", 9L, 55L, "My Flow"))
            .doesNotThrowAnyException();
    }
}

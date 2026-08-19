/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.tool.WorkflowArtifactRecorder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Enterprise implementation of the community {@link WorkflowArtifactRecorder} SPI. Extracts the AI Hub conversation
 * context from the forwarded tool context and records the persisted workflow as a chat artifact. A no-op when there is
 * no conversation id (the persist happened outside an AI Hub chat — Copilot in-editor panel, embedded autonomous
 * generation, or the plain editor).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class WorkflowArtifactRecorderImpl implements WorkflowArtifactRecorder {

    private static final Logger log = LoggerFactory.getLogger(WorkflowArtifactRecorderImpl.class);

    private final AiHubChatArtifactService chatArtifactService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowArtifactRecorderImpl(AiHubChatArtifactService chatArtifactService) {
        this.chatArtifactService = chatArtifactService;
    }

    @Override
    public void recordWorkflowArtifact(
        @Nullable ToolContext toolContext, boolean created, String workflowId, long projectId,
        @Nullable Long projectWorkflowId, String workflowName) {

        AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null || invocationContext.conversationId() == null) {
            return;
        }

        AiHubChatArtifactKind kind =
            created ? AiHubChatArtifactKind.WORKFLOW_CREATED : AiHubChatArtifactKind.WORKFLOW_UPDATED;

        try {
            chatArtifactService.recordWorkflowArtifact(
                invocationContext.conversationId(), invocationContext.userId(), kind, workflowId, projectId,
                projectWorkflowId, workflowName);
        } catch (RuntimeException exception) {
            // Best-effort: the workflow persist has already committed; never propagate.
            log.warn("Failed to record workflow artifact (workflowId={})", workflowId, exception);
        }
    }
}

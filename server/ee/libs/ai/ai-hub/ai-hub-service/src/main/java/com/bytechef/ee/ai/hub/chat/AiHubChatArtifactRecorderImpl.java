/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class AiHubChatArtifactRecorderImpl implements AiHubChatArtifactRecorder {

    private static final Logger log = LoggerFactory.getLogger(AiHubChatArtifactRecorderImpl.class);

    private final AiHubChatArtifactService chatArtifactService;

    @SuppressFBWarnings({
        "EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"
    })
    public AiHubChatArtifactRecorderImpl(
        AiHubChatArtifactService chatArtifactService) {

        this.chatArtifactService = chatArtifactService;
    }

    @Override
    public void record(
        String threadId, Long userId, String artifactKind, String artifactId, String artifactName) {

        record(threadId, userId, artifactKind, artifactId, artifactName, null);
    }

    @Override
    public void record(
        String threadId, Long userId, String artifactKind, String artifactId, String artifactName,
        @Nullable Map<String, Object> metadata) {

        if (userId == null) {
            log.warn(
                "AiHubChatArtifactRecorder.record called without a bound userId — skipping artifact record"
                    +
                    " (threadId={}, kind={}, artifactId={}, artifactName={}). The mutation has already committed; " +
                    "ensure the tool context carries a threadId before invoking the tool.",
                threadId, artifactKind, artifactId, artifactName);

            return;
        }

        AiHubChatArtifactKind kind;

        try {
            kind = AiHubChatArtifactKind.valueOf(artifactKind);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                "Unknown AiHubChatArtifactKind '" + artifactKind
                    + "' from tool callback (threadId=" + threadId
                    + ", artifactId=" + artifactId
                    + ", artifactName=" + artifactName + ")",
                exception);
        }

        chatArtifactService.record(threadId, userId, kind, artifactId, artifactName, metadata);
    }

    @Override
    public void recordWorkflowReference(
        String threadId, @Nullable Long userId, String workflowId, long projectId,
        @Nullable Long projectWorkflowId, String workflowName) {

        chatArtifactService.recordWorkflowArtifact(
            threadId, userId, AiHubChatArtifactKind.WORKFLOW_REFERENCED, workflowId, projectId, projectWorkflowId,
            workflowName);
    }

    @Override
    public void recordReference(
        String threadId, @Nullable Long userId, String artifactKind, String artifactId, String artifactName) {

        AiHubChatArtifactKind kind = AiHubChatArtifactKind.valueOf(artifactKind);

        chatArtifactService.recordReferenceByThread(threadId, userId, kind, artifactId, artifactName);
    }
}

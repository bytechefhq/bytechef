/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import com.bytechef.ee.ai.hub.tool.AiHubTaskArtifactRecorder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class AiHubTaskArtifactRecorderImpl implements AiHubTaskArtifactRecorder {

    private static final Logger log = LoggerFactory.getLogger(AiHubTaskArtifactRecorderImpl.class);

    private final AiHubTaskArtifactService taskArtifactService;
    private final @Nullable Counter missingUserIdCounter;

    @SuppressFBWarnings({
        "EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"
    })
    public AiHubTaskArtifactRecorderImpl(
        AiHubTaskArtifactService taskArtifactService,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.taskArtifactService = taskArtifactService;

        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        this.missingUserIdCounter = meterRegistry == null ? null : Counter
            .builder("bytechef.artifact.record.missing_userid_total")
            .description(
                "Number of artifact recordings skipped because the bound userId was null. Non-zero values mean a "
                    + "destructive mutation committed without an undo audit row — the user has no path to reverse it.")
            .register(meterRegistry);
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
            if (missingUserIdCounter != null) {
                missingUserIdCounter.increment();
            }

            log.warn(
                "AiHubTaskArtifactRecorder.record called without a bound userId — skipping artifact record"
                    +
                    " (threadId={}, kind={}, artifactId={}, artifactName={}). The mutation has already committed; " +
                    "ensure the tool context carries a threadId before invoking the tool.",
                threadId, artifactKind, artifactId, artifactName);

            return;
        }

        AiHubTaskArtifactKind kind;

        try {
            kind = AiHubTaskArtifactKind.valueOf(artifactKind);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                "Unknown AiHubTaskArtifactKind '" + artifactKind
                    + "' from tool callback (threadId=" + threadId
                    + ", artifactId=" + artifactId
                    + ", artifactName=" + artifactName + ")",
                exception);
        }

        taskArtifactService.record(threadId, userId, kind, artifactId, artifactName, metadata);
    }

    @Override
    public void recordWorkflowReference(
        String threadId, @Nullable Long userId, String workflowId, long projectId,
        @Nullable Long projectWorkflowId, String workflowName) {

        taskArtifactService.recordWorkflowArtifact(
            threadId, userId, AiHubTaskArtifactKind.WORKFLOW_REFERENCED, workflowId, projectId, projectWorkflowId,
            workflowName);
    }

    @Override
    public void recordReference(
        String threadId, @Nullable Long userId, String artifactKind, String artifactId, String artifactName) {

        AiHubTaskArtifactKind kind = AiHubTaskArtifactKind.valueOf(artifactKind);

        taskArtifactService.recordReferenceByThread(threadId, userId, kind, artifactId, artifactName);
    }
}

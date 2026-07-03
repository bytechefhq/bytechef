/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Minimal abstraction that allows asset-file tool callbacks to record task artifacts without coupling to the
 * {@code automation-ai-hub-api} module. Implementations are provided by the service layer and injected into the
 * callbacks at configuration time.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubTaskArtifactRecorder {

    /**
     * Records that a file was created during the current task turn.
     *
     * @param threadId     the AG-UI thread id that identifies the active task
     * @param userId       the id of the user who owns the task (may be {@code null} — will be treated as 0)
     * @param artifactKind the kind string (matches {@code AiHubTaskArtifactKind.name()})
     * @param artifactId   string representation of the created entity id
     * @param artifactName display name snapshot at creation time
     */
    void record(String threadId, Long userId, String artifactKind, String artifactId, String artifactName);

    /**
     * Same as {@link #record(String, Long, String, String, String)} but persists structured metadata as the artifact's
     * {@code metadata_json}. Used for kinds whose sidebar quick-open needs extra routing data — a workflow artifact,
     * for example, carries {@code projectId} + {@code projectWorkflowId} so the row is clickable.
     *
     * @param metadata routing metadata, or {@code null} for none
     */
    void record(
        String threadId, Long userId, String artifactKind, String artifactId, String artifactName,
        @Nullable Map<String, Object> metadata);

    /**
     * Records (or de-duplicates onto) a workflow artifact for a workflow the agent opened/referenced. Routes through
     * the dedup-aware {@code AiHubTaskArtifactService#recordWorkflowArtifact} so a referenced workflow collapses onto
     * an existing created/updated row for the same {@code (task, workflowId)} rather than producing a second sidebar
     * entry.
     *
     * @param threadId          the AG-UI thread id identifying the active task
     * @param userId            the owner of the task (may be {@code null} — treated as no binding)
     * @param workflowId        the workflow id (artifact id)
     * @param projectId         owning project id (routing metadata)
     * @param projectWorkflowId project-workflow id (routing metadata), may be {@code null}
     * @param workflowName      display-name snapshot
     */
    void recordWorkflowReference(
        String threadId, @Nullable Long userId, String workflowId, long projectId,
        @Nullable Long projectWorkflowId, String workflowName);
}

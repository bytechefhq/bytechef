/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Records and retrieves task artifact rows — the server-side log of every agent side-effect that occurred during a AI
 * Hub task turn.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubTaskArtifactService {

    /**
     * Records a new artifact for the task identified by {@code threadId + userId}. Silently returns without recording
     * when the task does not exist (e.g. the agent was invoked outside the AI Hub context).
     *
     * @param threadId     the AG-UI thread id that identifies the active task
     * @param userId       the id of the user who owns the task
     * @param kind         the artifact type
     * @param artifactId   the string id of the affected entity (file id, workflow id, row id, etc.)
     * @param artifactName display name snapshot taken at creation time
     * @param metadata     optional extra context; may be {@code null} or empty
     */
    void record(
        String threadId, long userId, AiHubTaskArtifactKind kind,
        String artifactId, String artifactName, @Nullable Map<String, Object> metadata);

    /**
     * Idempotently records a user-attached reference (file / workflow / data table / knowledge base) as a task
     * artifact. Differs from {@link #record(String, long, AiHubTaskArtifactKind, String, String, Map)} in three ways:
     * (1) keyed by {@code taskId} (not threadId), since the client already knows which task it's acting on; (2)
     * verifies caller ownership of the task via {@code requesterUserId} + {@code requesterWorkspaceId}; (3) idempotent
     * — re-recording the same {@code (taskId, kind, artifactId)} is a no-op so the user can re-attach a file in the
     * composer without proliferating sidebar rows.
     *
     * <p>
     * Throws {@link com.bytechef.ee.ai.hub.exception.ForbiddenException} on ownership / workspace mismatch and
     * {@link com.bytechef.ee.ai.hub.exception.NotFoundException} when the task does not exist. Returns the persisted
     * (or pre-existing) artifact so callers can render it in the sidebar without a follow-up fetch.
     */
    AiHubTaskArtifact recordReference(
        long taskId, long requesterWorkspaceId, long requesterUserId,
        AiHubTaskArtifactKind kind,
        String artifactId, String artifactName,
        @Nullable Map<String, Object> metadata);

    /**
     * Removes a previously-recorded reference artifact ({@code *_REFERENCED} kinds only). Idempotent: returns silently
     * when the row is already gone. Verifies the requester owns the parent task AND the task lives in
     * {@code requesterWorkspaceId} — same authorization shape as {@link #recordReference}.
     *
     * <p>
     * Throws {@link com.bytechef.ee.ai.hub.exception.ForbiddenException} on ownership / workspace mismatch and
     * {@link IllegalArgumentException} when the artifact is an agent-driven audit row (any non-{@code _REFERENCED}
     * kind) — those rows are immutable history and must not be deleted through the user-facing surface.
     * </p>
     */
    void deleteReference(long artifactId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Returns all artifacts for the given task ordered by {@code created_at DESC}. Verifies the task belongs to
     * {@code requesterUserId} AND lives in {@code requesterWorkspaceId}. Throws
     * {@link com.bytechef.ee.ai.hub.exception.ForbiddenException} on either mismatch and
     * {@link com.bytechef.ee.ai.hub.exception.NotFoundException} when the task does not exist.
     *
     * @param taskId               the task whose artifacts to retrieve
     * @param requesterWorkspaceId the workspace the task must live in
     * @param requesterUserId      the id of the user making the request
     * @return artifacts newest-first; never {@code null}
     */
    List<AiHubTaskArtifact> listByTask(
        long taskId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Returns the number of artifacts recorded for the given task. Does not perform an ownership check; callers must
     * ensure the task id is already validated.
     *
     * @param taskId the task to count artifacts for
     * @return the artifact count; 0 when there are none
     */
    long countByTask(long taskId);

    /**
     * Returns a filtered, paginated list of artifacts across all tasks in a workspace. Intended for the admin-only
     * audit viewer. The {@code environment} filter is the typical entry point for "what changed in PROD this week" —
     * when omitted, results span every environment, matching the legacy single-environment behaviour.
     *
     * @param workspaceId the workspace to query; must not be {@code null}
     * @param environment optional filter by the artifact's environment ordinal (DEVELOPMENT=0, STAGING=1,
     *                    PRODUCTION=2); applied directly against the denormalised {@code a.environment} column for fast
     *                    index probes without joining the task table
     * @param userId      optional filter by the owner of the task
     * @param kind        optional filter by artifact kind
     * @param from        optional inclusive lower bound on {@code created_at}
     * @param to          optional inclusive upper bound on {@code created_at}
     * @param page        zero-based page index
     * @param size        number of items per page
     * @return matching artifacts, newest first; never {@code null}
     */
    List<AiHubTaskArtifact> listByWorkspace(
        long workspaceId, @Nullable Integer environment, @Nullable Long userId,
        @Nullable AiHubTaskArtifactKind kind, @Nullable LocalDateTime from, @Nullable LocalDateTime to,
        int page, int size);

    /**
     * Returns the total count matching the same filter criteria as
     * {@link #listByWorkspace(long, Integer, Long, AiHubTaskArtifactKind, LocalDateTime, LocalDateTime, int, int)}.
     *
     * @param workspaceId the workspace to count in
     * @param environment optional environment filter; same semantics as on {@code listByWorkspace}
     * @param userId      optional user filter
     * @param kind        optional kind filter
     * @param from        optional from date filter
     * @param to          optional to date filter
     * @return total count; 0 when there are no matching artifacts
     */
    long countByWorkspace(
        long workspaceId, @Nullable Integer environment, @Nullable Long userId,
        @Nullable AiHubTaskArtifactKind kind, @Nullable LocalDateTime from, @Nullable LocalDateTime to);

    /**
     * Records a workflow artifact, idempotent on {@code (taskId, workflowId)} across the three workflow kinds. Resolves
     * the task from {@code threadId + userId}; logs and drops when no task exists or {@code userId} is null. When a
     * workflow row already exists for the task it is refreshed (name + routing metadata) and its existing kind is
     * preserved (a created row is never downgraded to referenced); otherwise a new row is inserted with the given
     * {@code kind}.
     *
     * @param threadId          the AG-UI thread id identifying the active task
     * @param userId            the owner of the task; null is treated as "no binding" and skips recording
     * @param kind              the workflow kind for a fresh insert (WORKFLOW_CREATED / WORKFLOW_UPDATED /
     *                          WORKFLOW_REFERENCED)
     * @param workflowId        the workflow id (artifact id)
     * @param projectId         owning project id; stored in routing metadata as a String
     * @param projectWorkflowId project-workflow id; stored in routing metadata as a Long; may be null
     * @param workflowName      display-name snapshot
     */
    void recordWorkflowArtifact(
        String threadId, @Nullable Long userId, AiHubTaskArtifactKind kind,
        String workflowId, long projectId, @Nullable Long projectWorkflowId, String workflowName);
}

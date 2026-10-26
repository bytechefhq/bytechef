/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Records and retrieves chat artifact rows — the server-side log of every agent side-effect that occurred during a AI
 * Hub chat turn.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubChatArtifactService {

    /**
     * Records a new artifact for the chat identified by {@code threadId + userId}. Silently returns without recording
     * when the chat does not exist (e.g. the agent was invoked outside the AI Hub context).
     *
     * @param threadId     the AG-UI thread id that identifies the active chat
     * @param userId       the id of the user who owns the chat
     * @param kind         the artifact type
     * @param artifactId   the string id of the affected entity (file id, workflow id, row id, etc.)
     * @param artifactName display name snapshot taken at creation time
     * @param metadata     optional extra context; may be {@code null} or empty
     */
    void record(
        String threadId, long userId, AiHubChatArtifactKind kind,
        String artifactId, String artifactName, @Nullable Map<String, Object> metadata);

    /**
     * Idempotently records a user-attached reference (file / workflow / data table / knowledge base) as a chat
     * artifact. Differs from {@link #record(String, long, AiHubChatArtifactKind, String, String, Map)} in three ways:
     * (1) keyed by {@code chatId} (not threadId), since the client already knows which chat it's acting on; (2)
     * verifies the requester may view the chat via {@code requesterUserId} + {@code requesterWorkspaceId}; (3)
     * idempotent — re-recording the same {@code (chatId, kind, artifactId)} is a no-op so the user can re-attach a file
     * in the composer without proliferating sidebar rows.
     *
     * <p>
     * Throws {@link com.bytechef.ee.ai.hub.exception.NotFoundException} when the chat does not exist or is not viewable
     * by the requester in {@code requesterWorkspaceId}. Returns the persisted (or pre-existing) artifact so callers can
     * render it in the sidebar without a follow-up fetch.
     */
    AiHubChatArtifact recordReference(
        long chatId, long requesterWorkspaceId, long requesterUserId,
        AiHubChatArtifactKind kind,
        String artifactId, String artifactName,
        @Nullable Map<String, Object> metadata);

    /**
     * Removes a previously-recorded {@code WORKFLOW_REFERENCED} or {@code AI_AGENT_REFERENCED} artifact. Idempotent:
     * returns silently when the row is already gone. Verifies the parent chat is viewable by the requester in
     * {@code requesterWorkspaceId}, same authorization shape as {@link #recordReference}.
     *
     * <p>
     * Throws {@link com.bytechef.ee.ai.hub.exception.NotFoundException} when the chat is not viewable and
     * {@link IllegalArgumentException} when the artifact is any other kind; agent-driven rows are immutable history and
     * must not be deleted through the user-facing surface.
     * </p>
     */
    void deleteReference(long artifactId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Returns the artifacts for the given chat ordered by {@code created_at DESC}, without workflow artifacts whose
     * workflow no longer exists. Verifies {@code requesterUserId} may view the chat in {@code requesterWorkspaceId}.
     * Throws {@link com.bytechef.ee.ai.hub.exception.NotFoundException} when the chat does not exist or is not
     * viewable.
     *
     * @param chatId               the chat whose artifacts to retrieve
     * @param requesterWorkspaceId the workspace the chat must live in
     * @param requesterUserId      the id of the user making the request
     * @return artifacts newest-first; never {@code null}
     */
    List<AiHubChatArtifact> listByChat(
        long chatId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Returns the number of artifacts recorded for the given chat. Does not perform an ownership check; callers must
     * ensure the chat id is already validated.
     *
     * @param chatId the chat to count artifacts for
     * @return the artifact count; 0 when there are none
     */
    long countByChat(long chatId);

    /**
     * Records a workflow artifact, idempotent on {@code (chatId, workflowId)} across the three workflow kinds. Resolves
     * the chat from {@code threadId + userId}; logs and drops when no chat exists or {@code userId} is null. When a
     * workflow row already exists for the chat it is refreshed (name + routing metadata) and its existing kind is
     * preserved (a created row is never downgraded to referenced); otherwise a new row is inserted with the given
     * {@code kind}.
     *
     * @param threadId          the AG-UI thread id identifying the active chat
     * @param userId            the owner of the chat; null is treated as "no binding" and skips recording
     * @param kind              the workflow kind for a fresh insert (WORKFLOW_CREATED / WORKFLOW_UPDATED /
     *                          WORKFLOW_REFERENCED)
     * @param workflowId        the workflow id (artifact id)
     * @param projectId         owning project id; stored in routing metadata as a String
     * @param projectWorkflowId project-workflow id; stored in routing metadata as a Long; may be null
     * @param workflowName      display-name snapshot
     */
    void recordWorkflowArtifact(
        String threadId, @Nullable Long userId, AiHubChatArtifactKind kind,
        String workflowId, long projectId, @Nullable Long projectWorkflowId, String workflowName);

    /**
     * Server robustness layer for reference-kind artifacts recorded from tool callbacks. Resolves the chat by
     * {@code (threadId, userId)} — unlike
     * {@link #recordReference(long, long, long, AiHubChatArtifactKind, String, String, Map)}, which is keyed by a
     * caller-known {@code chatId} — and dedups on {@code (chat, kind, artifactId)} via
     * {@code findFirstByChatIdAndKindAndArtifactId}, so it collapses onto an existing row instead of duplicating.
     * Returns {@code null} (no-op) when {@code userId} is {@code null} or no chat is found for the thread.
     *
     * @param threadId     the AG-UI thread id identifying the active chat
     * @param userId       the owner of the chat; {@code null} is treated as "no binding" and skips recording
     * @param kind         the artifact kind
     * @param artifactId   the string id of the referenced entity
     * @param artifactName display name snapshot taken at reference time
     * @return the existing or newly-saved artifact, or {@code null} when the recording was skipped
     */
    @Nullable
    AiHubChatArtifact recordReferenceByThread(
        String threadId, @Nullable Long userId, AiHubChatArtifactKind kind, String artifactId,
        String artifactName);
}

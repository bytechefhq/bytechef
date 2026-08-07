/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Service that manages AI Hub task metadata and delegates message rehydration to Spring AI's session store, keyed by
 * {@code AiHubTask.threadId} as the session id (tables {@code AI_SESSION} / {@code AI_SESSION_EVENT} under the default
 * jdbc backend; the backend is selected by {@code bytechef.ai.memory.provider}).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubTaskService {

    /**
     * Creates a new task record for the given workspace, user, environment, and thread. If a record with the same
     * threadId and userId already exists and the workspaceId matches, the existing record is returned (idempotent). The
     * environment ordinal is stamped onto the new row and used to partition listings; an idempotent hit on an existing
     * row does not retroactively change its environment.
     */
    AiHubTask create(long workspaceId, long userId, int environment, String threadId);

    /**
     * Creates a fresh workflow-chat task row on every call (always-new semantics, May 2026). Workflow chats are
     * persisted in the same {@code ai_hub_task} table as standard tasks but carry {@code kind = WORKFLOW_CHAT}, the
     * bound {@code workflowExecutionId}, and the parent {@code projectDeploymentId} for sidebar grouping. Each call
     * inserts a new row with a plain UUID threadId so session-store events are isolated per task; past tasks remain
     * reachable through the tasks list rather than being restored on re-click. The threadId carries no kind prefix —
     * routing relies on the {@code kind} column instead, which is the authoritative discriminator.
     *
     * <p>
     * The {@code title} is persisted as the new row's display title — the bridge agent doesn't fire the LLM-driven
     * {@code generateAiHubTaskTitle} loop, so without this initial title the task would render as "New AiHubTask"
     * forever. Pass {@code null} or blank to skip setting a title (the existing fallback to "New AiHubTask" applies
     * until the row is renamed).
     * </p>
     *
     * @param workspaceId         the owning workspace
     * @param userId              the owning user
     * @param environment         the environment ordinal the chat runs against
     * @param workflowExecutionId the workflow execution this chat is bound to (composite tenantId+UUID string)
     * @param projectDeploymentId the parent project deployment id (used by the sidebar grouping)
     * @param title               optional initial title for the new row
     * @return the newly-created task; never {@code null}
     */
    AiHubTask createWorkflowChat(
        long workspaceId, long userId, int environment, String workflowExecutionId, long projectDeploymentId,
        @Nullable String title);

    /**
     * Creates a fresh personal-agent task row on every call (always-new semantics, May 2026). Mirrors
     * {@link #createWorkflowChat} for the agent kind: each call inserts a new row with a plain UUID threadId so
     * session-store events are isolated per task. Past tasks with the same agent remain reachable through the tasks
     * list rather than being restored on re-click — the partial unique index that previously enforced one-row-per-agent
     * was dropped in {@code 20260505000001}. See {@link #createWorkflowChat} for why the threadId carries no kind
     * prefix.
     *
     * <p>
     * On first creation the {@code title} is persisted as the new row's display title. The LLM's
     * {@code generateAiHubTaskTitle} loop runs against personal-agent tasks once enough turns accumulate, so an initial
     * title here is the placeholder until the LLM regenerates a better one.
     * </p>
     *
     * @param workspaceId          the owning workspace
     * @param userId               the owning user
     * @param environment          the environment ordinal the chat runs against
     * @param aiHubPersonalAgentId the FK back to the {@code personal_agent} row this task runs against
     * @param title                optional initial title for the new row
     * @return the newly-created task; never {@code null}
     */
    AiHubTask createAiHubPersonalAgentChat(
        long workspaceId, long userId, int environment, long aiHubPersonalAgentId, @Nullable String title);

    /**
     * Lists tasks belonging to the given workspace + user + environment, filtered by status. Returns at most 100
     * results, ordered by last update descending. Environment is part of the partition so a user switching environments
     * sees a clean per-env task history.
     */
    List<AiHubTask>
        list(long workspaceId, long userId, int environment, AiHubTaskStatus status);

    /**
     * Loads the message history for a task from Spring AI's session store. Throws
     * {@link com.bytechef.ee.ai.hub.exception.ForbiddenException} if the task does not belong to the requester or does
     * not belong to {@code requesterWorkspaceId}, and {@link com.bytechef.ee.ai.hub.exception.NotFoundException} when
     * the task does not exist.
     */
    List<AiHubTaskMessage>
        loadMessages(long taskId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Applies a partial update ({@link AiHubTaskPatch}) to an existing task. Only non-null patch fields are applied.
     * Throws {@link com.bytechef.ee.ai.hub.exception.ForbiddenException} if the task does not belong to the requester
     * or does not belong to {@code requesterWorkspaceId}, and
     * {@link com.bytechef.ee.ai.hub.exception.NotFoundException} when the task does not exist.
     */
    AiHubTask patch(
        long taskId, long requesterWorkspaceId, long requesterUserId,
        AiHubTaskPatch taskPatch);

    /**
     * Hard-deletes a task, removes its events from the session store, and deletes the metadata row. Throws
     * {@link com.bytechef.ee.ai.hub.exception.ForbiddenException} if the task does not belong to the requester or does
     * not belong to {@code requesterWorkspaceId}, and {@link com.bytechef.ee.ai.hub.exception.NotFoundException} when
     * the task does not exist.
     */
    void delete(long taskId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Bulk-archives all {@code WORKFLOW_CHAT} tasks the user owns in the given workspace+environment that are currently
     * {@code ACTIVE}. Returns the number of rows that flipped to {@code ARCHIVED}. Idempotent: re-running after a
     * successful run returns 0 because nothing's left to archive.
     *
     * <p>
     * Designed for the "30 workflow chats clogging my sidebar" cleanup case. We archive (not delete) so the user can
     * unarchive individual tasks later; the workflow-chat re-click path also restores ARCHIVED rows automatically (see
     * {@code createWorkflowChat}).
     * </p>
     */
    int bulkArchiveWorkflowChatAiHubTasks(long workspaceId, long userId, int environment);

    /**
     * Truncates the chat-memory history for the given task, deleting all messages from the supplied index onward. Used
     * by the "edit and resend" UX: when the user edits an earlier user message, the client truncates the history at
     * that point and re-invokes {@code runAgent} with the edited message as the next turn — Spring AI's chat-memory
     * advisor then appends the new user message to the truncated history, producing a fresh branch from the edit point.
     *
     * <p>
     * Returns the number of rows deleted. Idempotent — calling with an index past the end of the history deletes zero
     * rows. Throws {@link com.bytechef.ee.ai.hub.exception.ForbiddenException} /
     * {@link com.bytechef.ee.ai.hub.exception.NotFoundException} on the same ownership invariants as {@link #patch}.
     * </p>
     *
     * <p>
     * <b>Why index-based and not timestamp-based:</b> the client renders messages in the same order this method reads
     * them ({@code ORDER BY timestamp ASC}), so a stable per-message index lets the client point at "the fourth
     * message" without shipping the timestamp back. Timestamps would also be ambiguous if two messages arrived in the
     * same millisecond, which is rare but possible under load.
     * </p>
     *
     * @param taskId               the task whose history to truncate
     * @param requesterWorkspaceId workspace of the calling user (ownership check)
     * @param requesterUserId      the calling user (ownership check)
     * @param fromMessageIndex     zero-based index; the message at this position AND all subsequent are deleted
     * @return the number of chat-memory rows that were deleted
     */
    int truncateMessagesFrom(
        long taskId, long requesterWorkspaceId, long requesterUserId, int fromMessageIndex);

    /**
     * Appends an assistant message to the task's chat memory. Used by the workflow-chat client to persist an
     * approval-resolution continuation — the resumed run's output streams to the client outside the
     * {@code WebhookBridgeAgent} turn model, so without this write the continuation text would vanish on reload. Blank
     * content is a no-op. Ownership invariants match {@link #truncateMessagesFrom}.
     *
     * @param taskId               the task to append to
     * @param requesterWorkspaceId workspace the requester claims; must own the task
     * @param requesterUserId      the requesting user; must own the task
     * @param content              the assistant message text
     */
    void appendAssistantMessage(long taskId, long requesterWorkspaceId, long requesterUserId, String content);

    /**
     * Cancels the in-flight workflow-chat turn for the given task. Returns {@code true} if a job was cancelled,
     * {@code false} when no job is currently running for this task (idempotent — the user may click stop after the
     * workflow has already completed; we report the no-op clearly).
     *
     * <p>
     * Resolves through {@code WorkflowChatJobRegistry} (the taskId-to-jobId mapping AgUiStreamBridge populates on the
     * workflow's {@code start} event) to find the right {@code JobFacade.stopJob} target. Throws
     * {@link com.bytechef.ee.ai.hub.exception.ForbiddenException}/
     * {@link com.bytechef.ee.ai.hub.exception.NotFoundException} on the same ownership invariants as {@link #patch}.
     * </p>
     */
    boolean cancelWorkflowChatTurn(long taskId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Cancels an in-flight LLM agent run for a STANDARD / PERSONAL_AGENT task. Sibling to
     * {@link #cancelWorkflowChatTurn}, which targets workflow-chat tasks bound to a workflow execution. Returns
     * {@code true} when a non-terminated run was cancelled, {@code false} when no run is in flight (idempotent — the
     * user may click stop after the run finished, and the client disambiguates via the boolean).
     *
     * <p>
     * Resolves through {@code InFlightAiHubRunRegistry}: marks the registry entry as terminated and completes the event
     * sink. The agent's reactive subscription may keep producing events server-side — the AGUI {@code agent.runAgent}
     * call doesn't expose a {@code Disposable} we can dispose — but subsequent events are dropped because the sink is
     * in a terminal state, and the client's mount-time probe sees the task as not-in-flight so the streaming UI is
     * dismissed. Throws {@link com.bytechef.ee.ai.hub.exception.ForbiddenException}/
     * {@link com.bytechef.ee.ai.hub.exception.NotFoundException} on the same ownership invariants as {@link #patch}.
     * </p>
     *
     * <p>
     * {@code runId} is the AG-UI runId of the turn being stopped, or {@code null} when the caller can't supply it. When
     * present it lets {@code InFlightAiHubRunRegistry} tombstone the run so a Stop that reaches the server before the
     * agent POST registers the run still terminates it.
     * </p>
     */
    boolean cancelAiHubRun(long taskId, long requesterWorkspaceId, long requesterUserId, String runId);

    /**
     * Loads the task for the given AG-UI thread id. Returns empty when no task exists — used by tool callbacks that
     * need the workspace / user owning the current turn without the caller having to supply userId.
     */
    Optional<AiHubTask> findByThreadId(String threadId);

    /**
     * Returns the workspace id from the given task's {@code workspace_id} column. Callers that need the workspace and
     * only have a task id in scope ({@code AiHubRoutingAgent}, metrics tags) call this helper rather than loading the
     * entity themselves. Throws {@code NotFoundException} if the task does not exist or its workspace is null.
     */
    long getWorkspaceId(long taskId);

    /**
     * Returns the {@code threadId} for the given task. The threadId is the AG-UI conversation id that pins chat memory
     * across turns; callers that have a taskId but need to drive {@code AiHubRoutingAgent} (which keys memory by
     * threadId) use this helper. Throws {@code NotFoundException} if no task exists with the given id.
     *
     * <p>
     * Privilege note: this method skips the workspace/user ownership check that {@link #getById(long, long, long)}
     * enforces. Callers must have already authorized the access through another mechanism (e.g. a single-use session
     * token gated by an authenticated REST endpoint). Used by the AI Hub voice WS handler, which validates the token at
     * upgrade time.
     */
    String getThreadId(long taskId);

    /**
     * Loads a task by id and verifies it belongs to {@code requesterUserId} AND lives in {@code requesterWorkspaceId}.
     * Used when the caller needs to read a task without applying any patch — avoids the leaky pattern of passing an
     * all-null {@link AiHubTaskPatch} just to round-trip a read through {@link #patch}. Throws
     * {@link com.bytechef.ee.ai.hub.exception.ForbiddenException}/
     * {@link com.bytechef.ee.ai.hub.exception.NotFoundException} on the same conditions as {@link #patch}.
     */
    AiHubTask getById(long taskId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Loads a task by its client-generated {@code threadId} and verifies the same ownership invariants as
     * {@link #getById}. The AI Hub client tracks tasks by their NanoID-shaped threadId (the AG-UI thread identifier —
     * see {@code useAiHubStore}); resolvers that take a {@code taskId: ID!} argument from the client receive that
     * string and use this method to map to the persistent row before authorization-sensitive operations.
     *
     * <p>
     * Probe-oracle defense matches {@link #getById}: "does not exist" and "exists in another workspace/user" both
     * surface as a generic not-found.
     * </p>
     */
    AiHubTask getByThreadId(String threadId, long requesterWorkspaceId, long requesterUserId);

    /**
     * A partial-update descriptor for {@link #patch}. Only non-null fields are applied. The all-null case is rejected
     * by the compact constructor — callers physically cannot construct a no-op patch, so the silent
     * "must-call-ensureNotEmpty" contract is replaced with a compile-time/runtime guarantee at the boundary.
     */
    record AiHubTaskPatch(
        @Nullable String title,
        @Nullable String lastPreview,
        @Nullable Integer messageCount,
        @Nullable AiHubTaskStatus status) {

        public AiHubTaskPatch {
            if (title == null && lastPreview == null && messageCount == null && status == null) {
                throw new IllegalArgumentException(
                    "AiHubTaskPatch requires at least one of title, lastPreview, messageCount, status");
            }
        }

        /**
         * @deprecated The compact constructor now enforces the same invariant. Calls to this method are no-ops kept
         *             only for binary compatibility with the controller. New code should rely on the constructor
         *             validation instead.
         */
        @Deprecated
        public void ensureNotEmpty() {
            // Validation moved to the compact constructor.
        }
    }

    /**
     * A single message row rehydrated from the session-memory transcript. {@code toolEventsJson} is a nullable JSON
     * array of the tool activity (calls with their argument JSON, results with their response payload) that occurred
     * after this row and before the next visible row — the client rebuilds tool-call cards and interactive tool-result
     * cards (e.g. askUserQuestion) from it on reload. It is deliberately an attachment to an existing visible row
     * rather than extra rows so the visible-row indexes that {@code truncateMessagesFrom} maps stay unchanged.
     */
    record AiHubTaskMessage(String role, String content, Instant timestamp, String toolEventsJson) {

        public AiHubTaskMessage(String role, String content, Instant timestamp) {
            this(role, content, timestamp, null);
        }
    }
}

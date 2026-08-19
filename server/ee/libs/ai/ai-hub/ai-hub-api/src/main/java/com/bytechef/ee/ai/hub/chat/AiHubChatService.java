/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Service that manages AI Hub chat metadata and delegates message rehydration to Spring AI's session store, keyed by
 * {@code AiHubChat.threadId} as the session id (tables {@code AI_SESSION} / {@code AI_SESSION_EVENT} under the default
 * jdbc backend; the backend is selected by {@code bytechef.ai.memory.provider}).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubChatService {

    /**
     * Creates a new chat record for the given workspace, user, environment, and thread. If a record with the same
     * threadId and userId already exists and the workspaceId matches, the existing record is returned (idempotent). The
     * environment ordinal is stamped onto the new row and used to partition listings; an idempotent hit on an existing
     * row does not retroactively change its environment.
     */
    AiHubChat create(long workspaceId, long userId, int environment, String threadId);

    /**
     * Creates a fresh workflow chat row on every call (always-new semantics, May 2026). Workflow chats are persisted in
     * the same {@code ai_hub_chat} table as standard chats but carry {@code kind = WORKFLOW_CHAT}, the bound
     * {@code workflowExecutionId}, and the parent {@code projectDeploymentId} for sidebar grouping. Each call inserts a
     * new row with a plain UUID threadId so session-store events are isolated per chat; past chats remain reachable
     * through the chats list rather than being restored on re-click. The threadId carries no kind prefix — routing
     * relies on the {@code kind} column instead, which is the authoritative discriminator.
     *
     * <p>
     * The {@code title} is persisted as the new row's display title — the bridge agent doesn't fire the LLM-driven
     * {@code generateAiHubChatTitle} loop, so without this initial title the chat would render as "New AiHubChat"
     * forever. Pass {@code null} or blank to skip setting a title (the existing fallback to "New AiHubChat" applies
     * until the row is renamed).
     * </p>
     *
     * @param workspaceId         the owning workspace
     * @param userId              the owning user
     * @param environment         the environment ordinal the chat runs against
     * @param workflowExecutionId the workflow execution this chat is bound to (composite tenantId+UUID string)
     * @param projectDeploymentId the parent project deployment id (used by the sidebar grouping)
     * @param title               optional initial title for the new row
     * @return the newly-created chat; never {@code null}
     */
    AiHubChat createWorkflowChat(
        long workspaceId, long userId, int environment, String workflowExecutionId, long projectDeploymentId,
        @Nullable String title);

    /**
     * Creates a fresh agent chat row on every call. Identical to {@link #createWorkflowChat} in every respect except
     * the persisted {@code kind} ({@code AGENT_CHAT}), which records that the user picked an AI Agent rather than one
     * of their own workflows — the bound workflow lives in the agent's hidden {@code __AI_AGENT__} project and is never
     * shown to them. Turns are served by the same webhook bridge.
     *
     * @param workspaceId         the owning workspace
     * @param userId              the owning user
     * @param environment         the environment ordinal the chat runs against
     * @param workflowExecutionId the agent workflow's execution this chat is bound to (composite tenantId+UUID string)
     * @param projectDeploymentId the agent's project deployment id
     * @param title               optional initial title for the new row; the agent's title, in practice
     * @return the newly-created chat; never {@code null}
     */
    AiHubChat createAgentChat(
        long workspaceId, long userId, int environment, String workflowExecutionId, long projectDeploymentId,
        @Nullable String title);

    /**
     * Lists chats belonging to the given workspace + user + environment, filtered by status. Returns at most 100
     * results, ordered by last update descending. Environment is part of the partition so a user switching environments
     * sees a clean per-env chat history.
     */
    List<AiHubChat>
        list(long workspaceId, long userId, int environment, AiHubChatStatus status);

    /**
     * Loads the message history for a chat from Spring AI's session store. Throws
     * {@link com.bytechef.ee.ai.hub.exception.ForbiddenException} if the chat does not belong to the requester or does
     * not belong to {@code requesterWorkspaceId}, and {@link com.bytechef.ee.ai.hub.exception.NotFoundException} when
     * the chat does not exist.
     */
    List<AiHubChatMessage>
        loadMessages(long chatId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Applies a partial update ({@link AiHubChatPatch}) to an existing chat. Only non-null patch fields are applied.
     * Throws {@link com.bytechef.ee.ai.hub.exception.ForbiddenException} if the chat does not belong to the requester
     * or does not belong to {@code requesterWorkspaceId}, and
     * {@link com.bytechef.ee.ai.hub.exception.NotFoundException} when the chat does not exist.
     */
    AiHubChat patch(
        long chatId, long requesterWorkspaceId, long requesterUserId,
        AiHubChatPatch chatPatch);

    /**
     * Hard-deletes a chat, removes its events from the session store, and deletes the metadata row. Throws
     * {@link com.bytechef.ee.ai.hub.exception.ForbiddenException} if the chat does not belong to the requester or does
     * not belong to {@code requesterWorkspaceId}, and {@link com.bytechef.ee.ai.hub.exception.NotFoundException} when
     * the chat does not exist.
     */
    void delete(long chatId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Bulk-archives all {@code WORKFLOW_CHAT} chats the user owns in the given workspace+environment that are currently
     * {@code ACTIVE}. Returns the number of rows that flipped to {@code ARCHIVED}. Idempotent: re-running after a
     * successful run returns 0 because nothing's left to archive.
     *
     * <p>
     * Designed for the "30 workflow chats clogging my sidebar" cleanup case. We archive (not delete) so the user can
     * unarchive individual chats later; the workflow-chat re-click path also restores ARCHIVED rows automatically (see
     * {@code createWorkflowChat}).
     * </p>
     */
    int bulkArchiveWorkflowChatAiHubChats(long workspaceId, long userId, int environment);

    /**
     * Truncates the chat-memory history for the given chat, deleting all messages from the supplied index onward. Used
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
     * @param chatId               the chat whose history to truncate
     * @param requesterWorkspaceId workspace of the calling user (ownership check)
     * @param requesterUserId      the calling user (ownership check)
     * @param fromMessageIndex     zero-based index; the message at this position AND all subsequent are deleted
     * @return the number of chat-memory rows that were deleted
     */
    int truncateMessagesFrom(
        long chatId, long requesterWorkspaceId, long requesterUserId, int fromMessageIndex);

    /**
     * Appends an assistant message to the chat's chat memory. Used by the workflow-chat client to persist an
     * approval-resolution continuation — the resumed run's output streams to the client outside the
     * {@code WebhookBridgeAgent} turn model, so without this write the continuation text would vanish on reload. Blank
     * content is a no-op. Ownership invariants match {@link #truncateMessagesFrom}.
     *
     * @param chatId               the chat to append to
     * @param requesterWorkspaceId workspace the requester claims; must own the chat
     * @param requesterUserId      the requesting user; must own the chat
     * @param content              the assistant message text
     */
    void appendAssistantMessage(long chatId, long requesterWorkspaceId, long requesterUserId, String content);

    /**
     * Cancels the in-flight workflow-chat turn for the given chat. Returns {@code true} if a job was cancelled,
     * {@code false} when no job is currently running for this chat (idempotent — the user may click stop after the
     * workflow has already completed; we report the no-op clearly).
     *
     * <p>
     * Resolves through {@code WorkflowChatJobRegistry} (the chatId-to-jobId mapping AgUiStreamBridge populates on the
     * workflow's {@code start} event) to find the right {@code JobFacade.stopJob} target. Throws
     * {@link com.bytechef.ee.ai.hub.exception.ForbiddenException}/
     * {@link com.bytechef.ee.ai.hub.exception.NotFoundException} on the same ownership invariants as {@link #patch}.
     * </p>
     */
    boolean cancelWorkflowChatTurn(long chatId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Cancels an in-flight LLM agent run for a STANDARD chat. Sibling to {@link #cancelWorkflowChatTurn}, which targets
     * workflow chats bound to a workflow execution. Returns {@code true} when a non-terminated run was cancelled,
     * {@code false} when no run is in flight (idempotent — the user may click stop after the run finished, and the
     * client disambiguates via the boolean).
     *
     * <p>
     * Resolves through {@code InFlightAiHubRunRegistry}: marks the registry entry as terminated and completes the event
     * sink. The agent's reactive subscription may keep producing events server-side — the AGUI {@code agent.runAgent}
     * call doesn't expose a {@code Disposable} we can dispose — but subsequent events are dropped because the sink is
     * in a terminal state, and the client's mount-time probe sees the chat as not-in-flight so the streaming UI is
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
    boolean cancelAiHubRun(long chatId, long requesterWorkspaceId, long requesterUserId, String runId);

    /**
     * Loads the chat for the given AG-UI thread id. Returns empty when no chat exists — used by tool callbacks that
     * need the workspace / user owning the current turn without the caller having to supply userId.
     */
    Optional<AiHubChat> findByThreadId(String threadId);

    /**
     * Returns the workspace id from the given chat's {@code workspace_id} column. Callers that need the workspace and
     * only have a chat id in scope ({@code AiHubRoutingAgent}, metrics tags) call this helper rather than loading the
     * entity themselves. Throws {@code NotFoundException} if the chat does not exist or its workspace is null.
     */
    long getWorkspaceId(long chatId);

    /**
     * Returns the {@code threadId} for the given chat. The threadId is the AG-UI conversation id that pins chat memory
     * across turns; callers that have a chatId but need to drive {@code AiHubRoutingAgent} (which keys memory by
     * threadId) use this helper. Throws {@code NotFoundException} if no chat exists with the given id.
     *
     * <p>
     * Privilege note: this method skips the workspace/user ownership check that {@link #getById(long, long, long)}
     * enforces. Callers must have already authorized the access through another mechanism (e.g. a single-use session
     * token gated by an authenticated REST endpoint). Used by the AI Hub voice WS handler, which validates the token at
     * upgrade time.
     */
    String getThreadId(long chatId);

    /**
     * Loads a chat by id and verifies it belongs to {@code requesterUserId} AND lives in {@code requesterWorkspaceId}.
     * Used when the caller needs to read a chat without applying any patch — avoids the leaky pattern of passing an
     * all-null {@link AiHubChatPatch} just to round-trip a read through {@link #patch}. Throws
     * {@link com.bytechef.ee.ai.hub.exception.ForbiddenException}/
     * {@link com.bytechef.ee.ai.hub.exception.NotFoundException} on the same conditions as {@link #patch}.
     */
    AiHubChat getById(long chatId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Loads a chat by its client-generated {@code threadId} and verifies the same ownership invariants as
     * {@link #getById}. The AI Hub client tracks chats by their NanoID-shaped threadId (the AG-UI thread identifier —
     * see {@code useAiHubStore}); resolvers that take a {@code chatId: ID!} argument from the client receive that
     * string and use this method to map to the persistent row before authorization-sensitive operations.
     *
     * <p>
     * Probe-oracle defense matches {@link #getById}: "does not exist" and "exists in another workspace/user" both
     * surface as a generic not-found.
     * </p>
     */
    AiHubChat getByThreadId(String threadId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Counts the chat's visible transcript and returns its genuinely latest message, in one pass over the session
     * store.
     *
     * <p>
     * Deliberately NOT expressed as {@code loadMessages(...).size()} plus its last element: {@link #loadMessages} stops
     * after the most recent {@code MESSAGE_LIMIT} visible rows because the UI never renders more, so on a long-lived
     * chat its last element is the 500th message, not the newest. A caller maintaining {@code message_count} and the
     * sidebar preview needs the real tail, and this returns one integer and one string rather than materialising the
     * whole transcript to derive them.
     * </p>
     *
     * <p>
     * Ownership is verified exactly as {@link #loadMessages} verifies it.
     * </p>
     */
    AiHubChatTranscriptSummary summarizeTranscript(long chatId, long requesterWorkspaceId, long requesterUserId);

    /**
     * A chat's transcript reduced to what a metadata refresh needs: how many visible messages it holds, and the text of
     * the most recent one ({@code null} when the transcript holds none).
     */
    record AiHubChatTranscriptSummary(int messageCount, @Nullable String lastMessageContent) {
    }

    /**
     * A partial-update descriptor for {@link #patch}. Only non-null fields are applied. The all-null case is rejected
     * by the compact constructor — callers physically cannot construct a no-op patch, so the silent
     * "must-call-ensureNotEmpty" contract is replaced with a compile-time/runtime guarantee at the boundary.
     */
    record AiHubChatPatch(
        @Nullable String title,
        @Nullable String lastPreview,
        @Nullable Integer messageCount,
        @Nullable AiHubChatStatus status) {

        public AiHubChatPatch {
            if (title == null && lastPreview == null && messageCount == null && status == null) {
                throw new IllegalArgumentException(
                    "AiHubChatPatch requires at least one of title, lastPreview, messageCount, status");
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
    record AiHubChatMessage(String role, String content, Instant timestamp, String toolEventsJson) {

        public AiHubChatMessage(String role, String content, Instant timestamp) {
            this(role, content, timestamp, null);
        }
    }
}

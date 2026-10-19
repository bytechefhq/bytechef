/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.jspecify.annotations.Nullable;

/**
 * Service that manages AI Hub chat metadata and delegates message rehydration to Spring AI's session store, keyed by
 * {@code AiHubChat.threadId} as the session id (tables {@code AI_SESSION} / {@code AI_SESSION_EVENT} under the default
 * jdbc backend; the backend is selected by {@code bytechef.ai.memory.provider}).
 *
 * <p>
 * Every method taking {@code requesterWorkspaceId} and {@code requesterUserId} verifies the chat lives in that
 * workspace and that the {@link AiHubChatAccessPolicy} admits the requester. A chat that does not exist, lives in
 * another workspace, or is not permitted for the requester is reported identically as a not-found so chat ids cannot be
 * enumerated.
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
     * Lists the requester's chats in the workspace and environment, most recently updated first. A {@code null} status
     * means every status.
     */
    List<AiHubChat> list(long workspaceId, long userId, int environment, @Nullable AiHubChatStatus status);

    /**
     * Returns the visible USER and ASSISTANT transcript of the chat, capped at the most recent slice the UI needs. Tool
     * activity between two visible rows is attached to the preceding row as a JSON blob.
     */
    List<AiHubChatMessage> loadMessages(long chatId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Applies the non-null fields of {@code chatPatch} to the chat.
     */
    AiHubChat patch(long chatId, long requesterWorkspaceId, long requesterUserId, AiHubChatPatch chatPatch);

    /**
     * Deletes the chat, its artifacts, and, after the delete commits, its session messages.
     */
    void delete(long chatId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Drops everything after the visible message with id {@code messageId}, tool events included, and the message
     * itself unless {@code keepMessage}, then supersedes every pending tool approval whose call it dropped. While a
     * turn runs in the chat it refuses with a conflict before dropping anything. The supersede is atomic with the drop
     * only when the session store shares the chat's transaction, as the JDBC store does; with any other store a failure
     * between the two leaves an approval pending, and the resolve-time check refuses it.
     *
     * @return the number of session events dropped, or empty when the chat never held a message with that id; a message
     *         an earlier edit or reload already dropped is refused with a conflict
     */
    OptionalInt truncateMessages(
        long chatId, long requesterWorkspaceId, long requesterUserId, String messageId, boolean keepMessage);

    /**
     * Whether the active session of {@code threadId} still holds the tool call an approval is for; a call an edit or
     * reload truncated away no longer counts. With a {@code toolCallId} only that call counts; without one, for an
     * approval recorded before tool call ids were, any call of {@code toolName} with {@code arguments} does, blank
     * arguments counting as an empty object. No access check: callers verify access themselves.
     */
    boolean containsActiveToolCall(
        String threadId, @Nullable String toolCallId, String toolName, String arguments);

    /**
     * Appends an assistant message to the chat's session. Blank content is ignored.
     */
    void appendAssistantMessage(long chatId, long requesterWorkspaceId, long requesterUserId, String content);

    /**
     * Stops the chat's in-flight agent run, for a requester who may participate in the chat. Returns {@code true} when
     * a live run was cancelled. {@code runId} is the AG-UI run id of the turn being stopped, or {@code null}; when
     * present it is tombstoned, so a Stop that reaches the server before the turn's POST registers the run still stops
     * it.
     */
    boolean cancelAiHubRun(long chatId, long requesterWorkspaceId, long requesterUserId, @Nullable String runId);

    /**
     * Looks a chat up by thread id with no access check; callers that act for a user must verify access themselves.
     */
    Optional<AiHubChat> findByThreadId(String threadId);

    /**
     * Looks a chat up by thread id and keeps it only when the requester may view it.
     */
    Optional<AiHubChat> findByThreadIdViewable(String threadId, long requesterUserId);

    /**
     * Batch form of {@link #findByThreadIdViewable}: one query for all thread ids, filtered row by row by the access
     * policy, keyed by thread id.
     */
    Map<String, AiHubChat> findAllByThreadIdViewable(Collection<String> threadIds, long requesterUserId);

    /**
     * Returns the id of the workspace owning the chat.
     */
    long getWorkspaceId(long chatId);

    /**
     * Returns the thread id of the chat.
     */
    String getThreadId(long chatId);

    /**
     * Loads the chat for a requester who may view it.
     */
    AiHubChat getById(long chatId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Loads the chat for a requester who may view it.
     */
    AiHubChat getViewable(long chatId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Loads the chat for a requester who may manage it.
     */
    AiHubChat getManageable(long chatId, long requesterWorkspaceId, long requesterUserId);

    /**
     * Partial update of a chat. Every field is optional, but at least one must be set.
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
    }

    /**
     * One visible transcript row. {@code id} is the id of its session event; a user row sent through the hub keeps the
     * AG-UI message id it was sent with.
     */
    record AiHubChatMessage(
        String id, String role, String content, Instant timestamp, @Nullable String toolEventsJson) {

        public AiHubChatMessage(String id, String role, String content, Instant timestamp) {
            this(id, role, content, timestamp, null);
        }
    }
}

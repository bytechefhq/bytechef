/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat.repository;

import com.bytechef.ee.ai.hub.chat.AiHubChat;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for {@link AiHubChat} metadata rows. Workspace-aware queries filter on the nullable
 * {@code ai_hub_chat.workspace_id} column, which loaded rows carry directly. A chat with a null workspace matches no
 * workspace-scoped query.
 *
 * <p>
 * Message contents remain in Spring AI's session store, keyed by {@code thread_id}, and are not managed here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubChatRepository extends CrudRepository<AiHubChat, Long> {

    @Query("""
        SELECT cct.* FROM ai_hub_chat cct
        WHERE cct.workspace_id = :workspaceId
          AND cct.user_id = :userId
          AND cct.environment = :environment
          AND cct.status = :status
        ORDER BY cct.updated_at DESC
        LIMIT :limit
        """)
    List<AiHubChat> findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(
        long workspaceId, long userId, int environment, int status, int limit);

    /**
     * Same as {@link #findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc} but across every status
     * except {@code excludedStatus}, for the chat list without a status filter.
     */
    @Query("""
        SELECT cct.* FROM ai_hub_chat cct
        WHERE cct.workspace_id = :workspaceId
          AND cct.user_id = :userId
          AND cct.environment = :environment
          AND cct.status <> :excludedStatus
        ORDER BY cct.updated_at DESC
        LIMIT :limit
        """)
    List<AiHubChat> findByWorkspaceIdAndUserIdAndEnvironmentAndStatusNotOrderByUpdatedAtDesc(
        long workspaceId, long userId, int environment, int excludedStatus, int limit);

    Optional<AiHubChat> findByThreadIdAndUserId(String threadId, long userId);

    Optional<AiHubChat> findByThreadId(String threadId);

    /**
     * Batch counterpart of {@link #findByThreadId}: resolves many thread ids in one query. Rows the caller may not view
     * are filtered afterwards, in the service, by the same access policy the single-row lookup applies.
     * {@code thread_id} carries a global UNIQUE constraint, so this returns at most one row per id.
     */
    List<AiHubChat> findAllByThreadIdIn(Collection<String> threadIds);

    /**
     * Sets the title and clears {@code auto_titled}, touching no other column, unless the row's current status is
     * {@code deletedStatus}.
     *
     * @return the number of rows updated, {@code 0} when the chat is deleted
     */
    @Modifying
    @Query("""
        UPDATE ai_hub_chat SET title = :title, auto_titled = false, updated_at = :updatedAt
        WHERE id = :id AND status <> :deletedStatus
        """)
    int updateTitleUnlessDeleted(long id, String title, int deletedStatus, LocalDateTime updatedAt);

    /**
     * Sets the last preview, touching no other column, unless the row's current status is {@code deletedStatus}.
     *
     * @return the number of rows updated, {@code 0} when the chat is deleted
     */
    @Modifying
    @Query("""
        UPDATE ai_hub_chat SET last_preview = :lastPreview, updated_at = :updatedAt
        WHERE id = :id AND status <> :deletedStatus
        """)
    int updateLastPreviewUnlessDeleted(long id, String lastPreview, int deletedStatus, LocalDateTime updatedAt);

    /**
     * Sets the message count, touching no other column, unless the row's current status is {@code deletedStatus}.
     *
     * @return the number of rows updated, {@code 0} when the chat is deleted
     */
    @Modifying
    @Query("""
        UPDATE ai_hub_chat SET message_count = :messageCount, updated_at = :updatedAt
        WHERE id = :id AND status <> :deletedStatus
        """)
    int updateMessageCountUnlessDeleted(long id, int messageCount, int deletedStatus, LocalDateTime updatedAt);

    /**
     * Sets {@code updated_at}, touching no other column, unless the row's current status is {@code deletedStatus}.
     *
     * @return the number of rows updated, {@code 0} when the chat is deleted
     */
    @Modifying
    @Query("""
        UPDATE ai_hub_chat SET updated_at = :updatedAt
        WHERE id = :id AND status <> :deletedStatus
        """)
    int updateUpdatedAtUnlessDeleted(long id, int deletedStatus, LocalDateTime updatedAt);

    /**
     * Sets the status ordinal, touching no other column, unless the row's current status is {@code deletedStatus}.
     *
     * @return the number of rows updated, {@code 0} when the chat is already deleted
     */
    @Modifying
    @Query("""
        UPDATE ai_hub_chat SET status = :status, updated_at = :updatedAt
        WHERE id = :id AND status <> :deletedStatus
        """)
    int updateStatusUnlessDeleted(long id, int status, int deletedStatus, LocalDateTime updatedAt);
}

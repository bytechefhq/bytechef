/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat.repository;

import com.bytechef.ee.ai.hub.chat.AiHubChat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
     * Same as {@link #findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc} but across ALL statuses —
     * used when a caller (e.g. the tool-binding lookup) needs every chat regardless of active/archived state.
     */
    @Query("""
        SELECT cct.* FROM ai_hub_chat cct
        WHERE cct.workspace_id = :workspaceId
          AND cct.user_id = :userId
          AND cct.environment = :environment
        ORDER BY cct.updated_at DESC
        LIMIT :limit
        """)
    List<AiHubChat> findByWorkspaceIdAndUserIdAndEnvironmentOrderByUpdatedAtDesc(
        long workspaceId, long userId, int environment, int limit);

    Optional<AiHubChat> findByThreadIdAndUserId(String threadId, long userId);

    Optional<AiHubChat> findByThreadId(String threadId);

    /**
     * Batch counterpart of {@link #findByThreadId}, for the {@code /status} poll. That endpoint is handed every thread
     * id the sidebar knows about on a 20-second interval, and resolving them one at a time made the poll's cost N
     * round-trips per tick where the {@code /in-flight} endpoint it replaced cost none. Rows the caller may not view
     * are filtered afterwards, in the service, by the same access policy the single-row lookup applies —
     * {@code thread_id} carries a global UNIQUE constraint, so this returns at most one row per id.
     */
    List<AiHubChat> findAllByThreadIdIn(Collection<String> threadIds);
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task.repository;

import com.bytechef.ee.ai.hub.task.AiHubTask;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for {@link AiHubTask} metadata rows. Workspace-aware queries JOIN through
 * {@code workspace_ai_hub_task}; the entity itself is workspace-agnostic. Service consumers must populate
 * {@code task.workspaceId} from {@link WorkspaceAiHubTaskRepository} after load.
 *
 * <p>
 * Message contents remain in Spring AI's {@code SPRING_AI_CHAT_MEMORY} table and are not managed here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubTaskRepository extends CrudRepository<AiHubTask, Long> {

    @Query("""
        SELECT cct.* FROM ai_hub_task cct
        JOIN workspace_ai_hub_task wcct ON wcct.ai_hub_task_id = cct.id
        WHERE wcct.workspace_id = :workspaceId
          AND cct.user_id = :userId
          AND cct.environment = :environment
          AND cct.status = :status
        ORDER BY cct.updated_at DESC
        LIMIT :limit
        """)
    List<AiHubTask> findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(
        long workspaceId, long userId, int environment, int status, int limit);

    /**
     * Same as {@link #findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc} but across ALL statuses —
     * used when a caller (e.g. the tool-binding lookup) needs every task regardless of active/archived state.
     */
    @Query("""
        SELECT cct.* FROM ai_hub_task cct
        JOIN workspace_ai_hub_task wcct ON wcct.ai_hub_task_id = cct.id
        WHERE wcct.workspace_id = :workspaceId
          AND cct.user_id = :userId
          AND cct.environment = :environment
        ORDER BY cct.updated_at DESC
        LIMIT :limit
        """)
    List<AiHubTask> findByWorkspaceIdAndUserIdAndEnvironmentOrderByUpdatedAtDesc(
        long workspaceId, long userId, int environment, int limit);

    Optional<AiHubTask> findByThreadIdAndUserId(String threadId, long userId);

    Optional<AiHubTask> findByThreadId(String threadId);

    /**
     * Lists every active workflow-chat task owned by the user in the given workspace+environment. Used by the
     * bulk-archive cleanup path; the result list is fed back through the regular {@code save} loop so each row gets the
     * standard updatedAt bump and validation, rather than running a single UPDATE statement that bypasses entity
     * lifecycle hooks.
     *
     * <p>
     * Returns at most {@code limit} rows so a workspace with thousands of stale tasks doesn't materialise the whole
     * list at once. The bulk-archive caller passes a generous cap (matching the sidebar list cap) — running the archive
     * twice catches any overflow.
     * </p>
     */
    @Query("""
        SELECT cct.* FROM ai_hub_task cct
        JOIN workspace_ai_hub_task wcct ON wcct.ai_hub_task_id = cct.id
        WHERE wcct.workspace_id = :workspaceId
          AND cct.user_id = :userId
          AND cct.environment = :environment
          AND cct.kind = :kind
          AND cct.status = :status
        LIMIT :limit
        """)
    List<AiHubTask> findByWorkspaceIdAndUserIdAndEnvironmentAndKindAndStatus(
        long workspaceId, long userId, int environment, int kind, int status, int limit);
}

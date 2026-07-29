/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.prompt.repository;

import com.bytechef.ee.platform.ai.prompt.AiPrompt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * CRUD on {@code ai_prompt}, plus the two workspace-scoped lookups that automation-ai-prompt's
 * {@code WorkspaceAiPromptService} exposes. Both filter {@code ai_prompt.workspace_id} directly; a workspace-less
 * prompt (null column) is invisible to them, which is the intended behavior.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiPromptRepository extends ListCrudRepository<AiPrompt, Long> {

    List<AiPrompt> findAllByWorkspaceId(Long workspaceId);

    /**
     * Null-tolerant lookup by name within a workspace: a workspace-level prompt carries a null {@code project_id} and
     * must match a null {@code projectId} argument, which plain SQL equality would not do. That is why this one keeps a
     * hand-written query rather than becoming a derived finder.
     */
    @Query("""
        SELECT ai_prompt.*
        FROM ai_prompt
        WHERE ai_prompt.workspace_id = :workspaceId
            AND (
                (ai_prompt.project_id IS NULL AND :projectId IS NULL)
                OR ai_prompt.project_id = :projectId
            )
            AND ai_prompt.name = :name
        """)
    Optional<AiPrompt> findByWorkspaceIdAndProjectIdAndName(
        @Param("workspaceId") Long workspaceId, @Param("projectId") Long projectId, @Param("name") String name);
}

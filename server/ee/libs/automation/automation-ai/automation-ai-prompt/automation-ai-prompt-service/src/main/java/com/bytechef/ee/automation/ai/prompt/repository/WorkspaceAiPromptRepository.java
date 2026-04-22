/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.prompt.repository;

import com.bytechef.ee.automation.ai.prompt.domain.WorkspaceAiPrompt;
import com.bytechef.ee.platform.ai.prompt.AiPrompt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Workspace ↔ {@code ai_prompt} membership repository plus workspace-aware lookups joining through
 * {@code workspace_ai_prompt}. Mirrors {@code WorkspaceAiEvalRuleRepository} in automation-ai-eval.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceAiPromptRepository extends ListCrudRepository<WorkspaceAiPrompt, Long> {

    Optional<WorkspaceAiPrompt> findByAiPromptId(long aiPromptId);

    @Query("""
        SELECT ai_prompt.*
        FROM ai_prompt
        JOIN workspace_ai_prompt
            ON workspace_ai_prompt.ai_prompt_id = ai_prompt.id
        WHERE workspace_ai_prompt.workspace_id = :workspaceId
        """)
    List<AiPrompt> findAllPromptsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("""
        SELECT ai_prompt.*
        FROM ai_prompt
        JOIN workspace_ai_prompt
            ON workspace_ai_prompt.ai_prompt_id = ai_prompt.id
        WHERE workspace_ai_prompt.workspace_id = :workspaceId
            AND (
                (ai_prompt.project_id IS NULL AND :projectId IS NULL)
                OR ai_prompt.project_id = :projectId
            )
            AND ai_prompt.name = :name
        """)
    Optional<AiPrompt> findPromptByWorkspaceIdAndProjectIdAndName(
        @Param("workspaceId") Long workspaceId, @Param("projectId") Long projectId, @Param("name") String name);
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.dataset.repository;

import com.bytechef.ee.automation.ai.eval.dataset.domain.WorkspaceAiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Workspace ↔ {@code ai_eval_dataset} membership repository plus workspace-aware lookups joining through
 * {@code workspace_ai_eval_dataset}. Mirrors {@code WorkspaceAiPromptRepository} in automation-ai-prompt.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceAiEvalDatasetRepository extends ListCrudRepository<WorkspaceAiEvalDataset, Long> {

    Optional<WorkspaceAiEvalDataset> findByAiEvalDatasetId(long aiEvalDatasetId);

    @Query("""
        SELECT ai_eval_dataset.*
        FROM ai_eval_dataset
        JOIN workspace_ai_eval_dataset
            ON workspace_ai_eval_dataset.ai_eval_dataset_id = ai_eval_dataset.id
        WHERE workspace_ai_eval_dataset.workspace_id = :workspaceId
        """)
    List<AiEvalDataset> findAllDatasetsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("""
        SELECT ai_eval_dataset.*
        FROM ai_eval_dataset
        JOIN workspace_ai_eval_dataset
            ON workspace_ai_eval_dataset.ai_eval_dataset_id = ai_eval_dataset.id
        WHERE workspace_ai_eval_dataset.workspace_id = :workspaceId
            AND ai_eval_dataset.name = :name
        """)
    Optional<AiEvalDataset> findDatasetByWorkspaceIdAndName(
        @Param("workspaceId") Long workspaceId, @Param("name") String name);
}

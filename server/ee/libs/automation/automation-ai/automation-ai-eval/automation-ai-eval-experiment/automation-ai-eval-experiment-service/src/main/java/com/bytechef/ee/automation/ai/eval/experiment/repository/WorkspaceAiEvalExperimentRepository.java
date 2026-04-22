/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.repository;

import com.bytechef.ee.automation.ai.eval.experiment.domain.WorkspaceAiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Workspace ↔ {@code ai_eval_experiment} membership repository plus workspace-aware lookups joining through
 * {@code workspace_ai_eval_experiment}. Mirrors {@code WorkspaceAiEvalDatasetRepository} in automation-ai-eval-dataset.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceAiEvalExperimentRepository extends ListCrudRepository<WorkspaceAiEvalExperiment, Long> {

    Optional<WorkspaceAiEvalExperiment> findByAiEvalExperimentId(long aiEvalExperimentId);

    @Query("""
        SELECT ai_eval_experiment.*
        FROM ai_eval_experiment
        JOIN workspace_ai_eval_experiment
            ON workspace_ai_eval_experiment.ai_eval_experiment_id = ai_eval_experiment.id
        WHERE workspace_ai_eval_experiment.workspace_id = :workspaceId
        """)
    List<AiEvalExperiment> findAllExperimentsByWorkspaceId(@Param("workspaceId") Long workspaceId);
}

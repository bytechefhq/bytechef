/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.repository;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * CRUD on {@code ai_eval_dataset}, including the two workspace-scoped reads that {@code WorkspaceAiEvalDatasetService}
 * in automation-ai-eval-dataset exposes.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalDatasetRepository extends ListCrudRepository<AiEvalDataset, Long> {

    /**
     * Datasets owned by one workspace. A dataset whose {@code workspace_id} is null belongs to no workspace and is
     * invisible here, which is the intended behavior for a workspace-scoped listing.
     */
    List<AiEvalDataset> findAllByWorkspaceId(Long workspaceId);

    Optional<AiEvalDataset> findByWorkspaceIdAndName(Long workspaceId, String name);
}

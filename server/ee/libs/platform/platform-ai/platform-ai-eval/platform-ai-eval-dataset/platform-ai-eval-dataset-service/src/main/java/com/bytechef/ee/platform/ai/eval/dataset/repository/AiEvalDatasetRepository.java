/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.repository;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Workspace-agnostic CRUD on {@code ai_eval_dataset}. Workspace-aware queries (joining through
 * {@code workspace_ai_eval_dataset}) live on {@code WorkspaceAiEvalDatasetRepository} in automation-ai-eval-dataset.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalDatasetRepository extends ListCrudRepository<AiEvalDataset, Long> {
}

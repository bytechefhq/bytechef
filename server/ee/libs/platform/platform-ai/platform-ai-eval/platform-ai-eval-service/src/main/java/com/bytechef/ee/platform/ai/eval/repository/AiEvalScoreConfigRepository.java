/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.repository;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Workspace-agnostic CRUD on {@code ai_eval_score_config}. Workspace-aware queries live on
 * {@code WorkspaceAiEvalScoreConfigRepository} in automation-ai-eval.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalScoreConfigRepository extends ListCrudRepository<AiEvalScoreConfig, Long> {
}

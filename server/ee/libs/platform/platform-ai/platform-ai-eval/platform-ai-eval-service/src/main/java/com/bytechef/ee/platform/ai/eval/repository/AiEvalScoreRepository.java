/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.repository;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Workspace-agnostic CRUD on {@code ai_eval_score}. Workspace-aware queries (by workspace, by name, trend) live on
 * {@code WorkspaceAiEvalScoreRepository} in automation-ai-eval.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalScoreRepository extends ListCrudRepository<AiEvalScore, Long> {

    List<AiEvalScore> findAllByTraceId(Long traceId);
}

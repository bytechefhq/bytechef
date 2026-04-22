/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.repository;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Workspace-agnostic CRUD on {@code ai_eval_rule}. Workspace-aware queries (joining through
 * {@code workspace_ai_eval_rule}) live on {@code WorkspaceAiEvalRuleRepository} in automation-ai-eval.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalRuleRepository extends ListCrudRepository<AiEvalRule, Long> {

    List<AiEvalRule> findAllByEnabledTrueAndTarget(int target);
}

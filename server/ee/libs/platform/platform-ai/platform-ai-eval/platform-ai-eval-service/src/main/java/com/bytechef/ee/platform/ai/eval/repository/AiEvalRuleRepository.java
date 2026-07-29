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
 * CRUD on {@code ai_eval_rule}, including the three workspace-scoped reads that {@code WorkspaceAiEvalRuleService} in
 * automation-ai-eval exposes.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalRuleRepository extends ListCrudRepository<AiEvalRule, Long> {

    List<AiEvalRule> findAllByEnabledTrueAndTarget(int target);

    /**
     * Rules owned by one workspace. A rule whose {@code workspace_id} is null belongs to no workspace and is invisible
     * here, which is the intended behavior for a workspace-scoped listing. The same holds for the two narrower finders
     * below.
     */
    List<AiEvalRule> findAllByWorkspaceId(Long workspaceId);

    List<AiEvalRule> findAllByWorkspaceIdAndEnabled(Long workspaceId, boolean enabled);

    List<AiEvalRule> findAllByWorkspaceIdAndEnabledTrueAndTarget(Long workspaceId, int target);
}

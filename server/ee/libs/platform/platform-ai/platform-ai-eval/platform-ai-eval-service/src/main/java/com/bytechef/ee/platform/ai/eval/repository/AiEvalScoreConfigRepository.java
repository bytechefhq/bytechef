/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.repository;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * CRUD on {@code ai_eval_score_config}, including the two workspace-scoped reads that
 * {@code WorkspaceAiEvalScoreConfigService} in automation-ai-eval exposes.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalScoreConfigRepository extends ListCrudRepository<AiEvalScoreConfig, Long> {

    /**
     * Configs owned by one workspace. A config whose {@code workspace_id} is null belongs to no workspace and is
     * invisible here, which is the intended behavior for a workspace-scoped listing.
     */
    List<AiEvalScoreConfig> findAllByWorkspaceId(Long workspaceId);

    /**
     * At most one row can match: {@code uk_ai_eval_score_config_workspace_name} makes {@code (workspace_id, name)}
     * unique.
     */
    Optional<AiEvalScoreConfig> findByWorkspaceIdAndName(Long workspaceId, String name);
}

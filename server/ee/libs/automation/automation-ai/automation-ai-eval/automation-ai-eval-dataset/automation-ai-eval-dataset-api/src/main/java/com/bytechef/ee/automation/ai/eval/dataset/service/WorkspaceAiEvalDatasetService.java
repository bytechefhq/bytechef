/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.dataset.service;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import java.util.List;
import java.util.Optional;

/**
 * Workspace-scoped operations on {@link AiEvalDataset}. Mirrors {@code WorkspaceAiEvalRuleService} in
 * automation-ai-eval — the platform service ({@code AiEvalDatasetService}) handles workspace-agnostic CRUD; this
 * service owns the dataset's {@code workspace_id} binding and the workspace-scoped queries over it.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceAiEvalDatasetService {

    AiEvalDataset createInWorkspace(AiEvalDataset dataset, long workspaceId);

    Long getWorkspaceId(long datasetId);

    List<AiEvalDataset> findAllByWorkspace(Long workspaceId);

    Optional<AiEvalDataset> findByWorkspaceAndName(Long workspaceId, String name);
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.service;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;

/**
 * Workspace-agnostic CRUD on {@link AiEvalDataset}. Archival is a soft-delete — {@code archivedDate} is stamped but the
 * row is retained so existing dataset items and eval runs remain traceable. Workspace-scoped queries (by workspace,
 * by-workspace+name) live in {@code com.bytechef.ee.automation.ai.eval.dataset.service.WorkspaceAiEvalDatasetService} —
 * mirrors the {@code platform-connection.ConnectionService} /
 * {@code automation-configuration.WorkspaceConnectionService} split.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalDatasetService {

    AiEvalDataset create(AiEvalDataset dataset);

    void archive(long id);

    AiEvalDataset getDataset(long id);

    AiEvalDataset update(AiEvalDataset dataset);
}

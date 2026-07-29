/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.service;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import java.util.List;

/**
 * Workspace-scoped operations on {@link AiEvalExperiment}. Mirrors {@code WorkspaceAiEvalDatasetService} — the platform
 * service ({@code AiEvalExperimentService}) handles workspace-agnostic CRUD + lifecycle; this service owns the
 * experiment's {@code workspace_id} binding and the workspace-scoped queries over it.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceAiEvalExperimentService {

    AiEvalExperiment createInWorkspace(AiEvalExperiment experiment, long workspaceId);

    Long getWorkspaceId(long experimentId);

    List<AiEvalExperiment> findAllByWorkspace(Long workspaceId);
}

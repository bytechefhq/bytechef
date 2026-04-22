/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.service;

import com.bytechef.ee.automation.ai.eval.experiment.domain.WorkspaceAiEvalExperiment;
import com.bytechef.ee.automation.ai.eval.experiment.repository.WorkspaceAiEvalExperimentRepository;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workspace-aware operations on {@link AiEvalExperiment}. Delegates entity-level CRUD + lifecycle to the platform
 * {@link AiEvalExperimentService}; owns the workspace membership row in {@code workspace_ai_eval_experiment} and
 * workspace-scoped queries that join through it. Mirrors {@code WorkspaceAiEvalDatasetServiceImpl}.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
class WorkspaceAiEvalExperimentServiceImpl implements WorkspaceAiEvalExperimentService {

    private final AiEvalExperimentService aiEvalExperimentService;
    private final WorkspaceAiEvalExperimentRepository workspaceAiEvalExperimentRepository;

    WorkspaceAiEvalExperimentServiceImpl(
        AiEvalExperimentService aiEvalExperimentService,
        WorkspaceAiEvalExperimentRepository workspaceAiEvalExperimentRepository) {

        this.aiEvalExperimentService = aiEvalExperimentService;
        this.workspaceAiEvalExperimentRepository = workspaceAiEvalExperimentRepository;
    }

    @Override
    public AiEvalExperiment createInWorkspace(AiEvalExperiment experiment, long workspaceId) {
        Validate.notNull(experiment, "experiment must not be null");
        Validate.isTrue(experiment.getId() == null, "experiment id must be null for creation");

        AiEvalExperiment saved = aiEvalExperimentService.create(experiment);

        workspaceAiEvalExperimentRepository.save(new WorkspaceAiEvalExperiment(saved.getId(), workspaceId));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long experimentId) {
        return workspaceAiEvalExperimentRepository.findByAiEvalExperimentId(experimentId)
            .map(WorkspaceAiEvalExperiment::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalExperiment> findAllByWorkspace(Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        return workspaceAiEvalExperimentRepository.findAllExperimentsByWorkspaceId(workspaceId);
    }
}

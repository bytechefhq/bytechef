/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.service;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.repository.AiEvalExperimentRepository;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
class WorkspaceAiEvalExperimentServiceImpl implements WorkspaceAiEvalExperimentService {

    private final AiEvalExperimentRepository aiEvalExperimentRepository;
    private final AiEvalExperimentService aiEvalExperimentService;

    WorkspaceAiEvalExperimentServiceImpl(
        AiEvalExperimentRepository aiEvalExperimentRepository, AiEvalExperimentService aiEvalExperimentService) {

        this.aiEvalExperimentRepository = aiEvalExperimentRepository;
        this.aiEvalExperimentService = aiEvalExperimentService;
    }

    @Override
    public AiEvalExperiment createInWorkspace(AiEvalExperiment experiment, long workspaceId) {
        Validate.notNull(experiment, "experiment must not be null");
        Validate.isTrue(experiment.getId() == null, "experiment id must be null for creation");

        experiment.setWorkspaceId(workspaceId);

        return aiEvalExperimentService.create(experiment);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long experimentId) {
        return aiEvalExperimentRepository.findById(experimentId)
            .map(AiEvalExperiment::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalExperiment> findAllByWorkspace(Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        return aiEvalExperimentRepository.findAllByWorkspaceId(workspaceId);
    }
}

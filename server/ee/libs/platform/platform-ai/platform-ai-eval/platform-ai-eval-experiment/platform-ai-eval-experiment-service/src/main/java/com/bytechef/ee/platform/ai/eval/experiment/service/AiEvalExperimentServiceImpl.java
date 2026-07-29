/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.service;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentStatus;
import com.bytechef.ee.platform.ai.eval.experiment.repository.AiEvalExperimentRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workspace-agnostic CRUD + lifecycle implementation for {@link AiEvalExperiment}. Setting the experiment's
 * {@code workspaceId} and any workspace-scoped query is owned by
 * {@code com.bytechef.ee.automation.ai.eval.experiment.service.WorkspaceAiEvalExperimentServiceImpl}.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
class AiEvalExperimentServiceImpl implements AiEvalExperimentService {

    private final AiEvalExperimentRepository aiEvalExperimentRepository;

    AiEvalExperimentServiceImpl(AiEvalExperimentRepository aiEvalExperimentRepository) {
        this.aiEvalExperimentRepository = aiEvalExperimentRepository;
    }

    @Override
    public AiEvalExperiment create(AiEvalExperiment experiment) {
        Validate.notNull(experiment, "experiment must not be null");
        Validate.isTrue(experiment.getId() == null, "experiment id must be null for creation");

        return aiEvalExperimentRepository.save(experiment);
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvalExperiment getExperiment(long id) {
        return aiEvalExperimentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiEvalExperiment not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalExperiment> findAllByDatasetVersion(Long datasetVersionId) {
        return aiEvalExperimentRepository.findAllByDatasetVersionId(datasetVersionId);
    }

    @Override
    public AiEvalExperiment update(AiEvalExperiment experiment) {
        Validate.notNull(experiment, "experiment must not be null");
        Validate.notNull(experiment.getId(), "experiment id must not be null for update");

        // Reject updates against deleted rows — cheaper than letting optimistic-locking blow up at save time.
        if (!aiEvalExperimentRepository.existsById(experiment.getId())) {
            throw new IllegalArgumentException("AiEvalExperiment not found with id: " + experiment.getId());
        }

        return aiEvalExperimentRepository.save(experiment);
    }

    @Override
    public AiEvalExperiment markRunning(long experimentId) {
        AiEvalExperiment experiment = getExperiment(experimentId);

        experiment.start();

        return aiEvalExperimentRepository.save(experiment);
    }

    @Override
    public AiEvalExperiment markFinished(long experimentId, boolean anyRunFailed) {
        AiEvalExperiment experiment = getExperiment(experimentId);

        if (anyRunFailed) {
            experiment.fail();
        } else {
            experiment.complete();
        }

        return aiEvalExperimentRepository.save(experiment);
    }

    @Override
    public AiEvalExperiment requestStop(long experimentId) {
        AiEvalExperiment experiment = getExperiment(experimentId);

        experiment.requestStop();

        return aiEvalExperimentRepository.save(experiment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalExperiment> findRunningOlderThan(Instant threshold) {
        return aiEvalExperimentRepository.findAllByStatusAndStartedDateBefore(
            AiEvalExperimentStatus.RUNNING.ordinal(), threshold);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalExperiment> findPendingOlderThan(Instant threshold) {
        return aiEvalExperimentRepository.findAllByStatusAndCreatedDateBefore(
            AiEvalExperimentStatus.PENDING.ordinal(), threshold);
    }
}

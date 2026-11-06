/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.service;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRunStatus;
import com.bytechef.ee.platform.ai.eval.experiment.repository.AiEvalExperimentRunRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiEvalExperimentRunServiceImpl implements AiEvalExperimentRunService {

    private final AiEvalExperimentRunRepository aiEvalExperimentRunRepository;

    AiEvalExperimentRunServiceImpl(AiEvalExperimentRunRepository aiEvalExperimentRunRepository) {
        this.aiEvalExperimentRunRepository = aiEvalExperimentRunRepository;
    }

    @Override
    public AiEvalExperimentRun create(AiEvalExperimentRun run) {
        Validate.notNull(run, "run must not be null");
        Validate.isTrue(run.getId() == null, "run id must be null for creation");

        return aiEvalExperimentRunRepository.save(run);
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvalExperimentRun getRun(long id) {
        return aiEvalExperimentRunRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiEvalExperimentRun not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalExperimentRun> findAllByExperiment(Long experimentId) {
        return aiEvalExperimentRunRepository.findAllByExperimentIdOrderByIdAsc(experimentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiEvalExperimentRun> findByTraceId(Long traceId) {
        if (traceId == null) {
            return Optional.empty();
        }

        return aiEvalExperimentRunRepository.findByTraceId(traceId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByExperiment(Long experimentId) {
        return aiEvalExperimentRunRepository.countByExperimentId(experimentId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByExperimentAndStatus(Long experimentId, AiEvalExperimentRunStatus status) {
        Validate.notNull(status, "status must not be null");

        return aiEvalExperimentRunRepository.countByExperimentIdAndStatus(experimentId, status.ordinal());
    }

    @Override
    public AiEvalExperimentRun update(AiEvalExperimentRun run) {
        Validate.notNull(run, "run must not be null");
        Validate.notNull(run.getId(), "run id must not be null for update");

        if (!aiEvalExperimentRunRepository.existsById(run.getId())) {
            throw new IllegalArgumentException("AiEvalExperimentRun not found with id: " + run.getId());
        }

        return aiEvalExperimentRunRepository.save(run);
    }

    @Override
    public AiEvalExperimentRun markRunning(long runId) {
        AiEvalExperimentRun run = getRun(runId);

        run.markRunning();

        return aiEvalExperimentRunRepository.save(run);
    }

    @Override
    public AiEvalExperimentRun complete(long runId, Long traceId, Integer latencyMs, BigDecimal cost) {
        AiEvalExperimentRun run = getRun(runId);

        run.complete(traceId, latencyMs, cost);

        return aiEvalExperimentRunRepository.save(run);
    }

    @Override
    public AiEvalExperimentRun fail(long runId, String errorMessage) {
        AiEvalExperimentRun run = getRun(runId);

        run.fail(errorMessage);

        return aiEvalExperimentRunRepository.save(run);
    }
}

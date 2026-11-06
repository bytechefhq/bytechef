/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.service;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRunStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalExperimentRunService {

    AiEvalExperimentRun create(AiEvalExperimentRun run);

    AiEvalExperimentRun getRun(long id);

    List<AiEvalExperimentRun> findAllByExperiment(Long experimentId);

    Optional<AiEvalExperimentRun> findByTraceId(Long traceId);

    long countByExperiment(Long experimentId);

    long countByExperimentAndStatus(Long experimentId, AiEvalExperimentRunStatus status);

    AiEvalExperimentRun update(AiEvalExperimentRun run);

    AiEvalExperimentRun markRunning(long runId);

    AiEvalExperimentRun complete(long runId, Long traceId, Integer latencyMs, BigDecimal cost);

    AiEvalExperimentRun fail(long runId, String errorMessage);
}

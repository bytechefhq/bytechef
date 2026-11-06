/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.service;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import java.time.Instant;
import java.util.List;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalExperimentService {

    AiEvalExperiment create(AiEvalExperiment experiment);

    AiEvalExperiment getExperiment(long id);

    List<AiEvalExperiment> findAllByDatasetVersion(Long datasetVersionId);

    AiEvalExperiment update(AiEvalExperiment experiment);

    AiEvalExperiment markRunning(long experimentId);

    AiEvalExperiment markFinished(long experimentId, boolean anyRunFailed);

    AiEvalExperiment requestStop(long experimentId);

    List<AiEvalExperiment> findRunningOlderThan(Instant threshold);

    List<AiEvalExperiment> findPendingOlderThan(Instant threshold);
}

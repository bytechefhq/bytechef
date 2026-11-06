/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.repository;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import java.time.Instant;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalExperimentRepository extends ListCrudRepository<AiEvalExperiment, Long> {

    List<AiEvalExperiment> findAllByDatasetVersionId(Long datasetVersionId);

    List<AiEvalExperiment> findAllByStatusAndStartedDateBefore(int status, Instant threshold);

    List<AiEvalExperiment> findAllByStatusAndCreatedDateBefore(int status, Instant threshold);

    List<AiEvalExperiment> findAllByWorkspaceId(Long workspaceId);
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.repository;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalExperimentRunRepository extends ListCrudRepository<AiEvalExperimentRun, Long> {

    List<AiEvalExperimentRun> findAllByExperimentIdOrderByIdAsc(Long experimentId);

    long countByExperimentId(Long experimentId);

    long countByExperimentIdAndStatus(Long experimentId, int status);

    Optional<AiEvalExperimentRun> findByTraceId(Long traceId);
}

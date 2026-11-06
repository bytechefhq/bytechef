/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade;

import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalExperimentRunView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalExperimentView;
import java.util.List;

/**
 * @version ee
 * @author Ivica Cardic
 */
public interface AiEvalExperimentQueryFacade {

    List<AiEvalExperimentView> getAiEvalExperiments(Long workspaceId);

    AiEvalExperimentRunView fetchAiEvalExperimentRunByTraceId(Long traceId);

    List<AiEvalExperimentRunView> getAiEvalExperimentRuns(Long experimentId);
}

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
 * Facade for the operator console's experiment queries. Hosts their workspace-boundary guards so they apply to every
 * caller of the facade rather than only the GraphQL entry point, and keeps them off the shared
 * {@code AiEvalExperimentService} / {@code AiEvalExperimentRunService} which the experiment executor relies on.
 *
 * <p>
 * Sibling of {@link AiEvalExperimentComparisonFacade}, which already carries the {@code experimentComparison} query's
 * guard. The facade lives in the GraphQL module for the same reason: these views assemble GraphQL DTOs that are not
 * visible to the experiment service module.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiEvalExperimentQueryFacade {

    List<AiEvalExperimentView> getAiEvalExperiments(Long workspaceId);

    AiEvalExperimentRunView fetchAiEvalExperimentRunByTraceId(Long traceId);

    List<AiEvalExperimentRunView> getAiEvalExperimentRuns(Long experimentId);
}

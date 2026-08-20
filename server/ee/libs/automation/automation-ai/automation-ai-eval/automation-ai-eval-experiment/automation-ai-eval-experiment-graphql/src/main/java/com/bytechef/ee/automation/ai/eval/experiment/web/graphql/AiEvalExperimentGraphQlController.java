/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalExperimentRunView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalExperimentView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ExperimentComparisonView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade.AiEvalExperimentComparisonFacade;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade.AiEvalExperimentQueryFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Exposes the experiment GraphQL queries. Every query's authorization is enforced on a facade, never here: the
 * {@code experimentComparison} query's {@code ADMIN} guard lives on {@link AiEvalExperimentComparisonFacade}, and the
 * remaining queries' workspace-boundary guards live on {@link AiEvalExperimentQueryFacade}. The latter used to be
 * written out in this class's method bodies, where they were invisible to any audit scanning for {@code @PreAuthorize}.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiEvalExperimentGraphQlController {

    private final AiEvalExperimentComparisonFacade aiEvalExperimentComparisonFacade;
    private final AiEvalExperimentQueryFacade aiEvalExperimentQueryFacade;

    @SuppressFBWarnings("EI")
    AiEvalExperimentGraphQlController(
        AiEvalExperimentComparisonFacade aiEvalExperimentComparisonFacade,
        AiEvalExperimentQueryFacade aiEvalExperimentQueryFacade) {

        this.aiEvalExperimentComparisonFacade = aiEvalExperimentComparisonFacade;
        this.aiEvalExperimentQueryFacade = aiEvalExperimentQueryFacade;
    }

    @QueryMapping
    public List<AiEvalExperimentView> aiEvalExperiments(@Argument Long workspaceId) {
        return aiEvalExperimentQueryFacade.getAiEvalExperiments(workspaceId);
    }

    @QueryMapping
    public AiEvalExperimentRunView aiEvalExperimentRunByTraceId(@Argument Long traceId) {
        return aiEvalExperimentQueryFacade.fetchAiEvalExperimentRunByTraceId(traceId);
    }

    @QueryMapping
    public List<AiEvalExperimentRunView> aiEvalExperimentRuns(@Argument Long experimentId) {
        return aiEvalExperimentQueryFacade.getAiEvalExperimentRuns(experimentId);
    }

    @QueryMapping
    public ExperimentComparisonView experimentComparison(@Argument List<Long> experimentIds) {
        return aiEvalExperimentComparisonFacade.experimentComparison(experimentIds);
    }
}

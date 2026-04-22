/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade;

import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ExperimentComparisonView;
import java.util.List;

/**
 * Facade for the admin-only {@code experimentComparison} query. Hosts the {@code ADMIN} authorization guard so it
 * applies to every caller of the facade rather than only the GraphQL entry point, and keeps it off the shared
 * {@code AiEvalExperimentService}/{@code AiEvalScoreService} which the experiment executor relies on. The facade lives
 * in the GraphQL module because the comparison view assembles GraphQL DTOs that are not visible to the experiment
 * service module.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiEvalExperimentComparisonFacade {

    ExperimentComparisonView experimentComparison(List<Long> experimentIds);
}

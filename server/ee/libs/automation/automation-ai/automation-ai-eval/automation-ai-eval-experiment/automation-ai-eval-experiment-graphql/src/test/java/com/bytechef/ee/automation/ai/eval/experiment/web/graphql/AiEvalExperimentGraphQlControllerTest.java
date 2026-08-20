/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.eval.experiment.service.WorkspaceAiEvalExperimentService;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalExperimentRunView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalExperimentView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ExperimentComparisonView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade.AiEvalExperimentComparisonFacade;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade.AiEvalExperimentQueryFacade;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins that every experiment query reaches its rows through a facade, which is where this codebase puts authorization.
 * The workspace-boundary guards themselves are pinned by {@code AiEvalExperimentQueryFacadeImplTest} and the
 * {@code ADMIN} guard by {@code AiEvalExperimentComparisonFacadeAuthorizationTest}.
 *
 * <p>
 * The controller carries no gate of its own and is not meant to. Asserting the delegations is not enough on its own:
 * {@code testControllerHoldsNoAuthorizationOrDataCollaborators} is what makes a revert to a locally assembled listing
 * with a locally written {@code permissionService} check fail here rather than pass, since such a listing has to
 * reintroduce those collaborators as fields.
 *
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalExperimentGraphQlControllerTest {

    private static final long EXPERIMENT_ID = 10L;
    private static final long TRACE_ID = 501L;
    private static final long WORKSPACE_ID = 1L;

    private final AiEvalExperimentComparisonFacade aiEvalExperimentComparisonFacade =
        mock(AiEvalExperimentComparisonFacade.class);
    private final AiEvalExperimentQueryFacade aiEvalExperimentQueryFacade = mock(AiEvalExperimentQueryFacade.class);

    private final AiEvalExperimentGraphQlController aiEvalExperimentGraphQlController =
        new AiEvalExperimentGraphQlController(aiEvalExperimentComparisonFacade, aiEvalExperimentQueryFacade);

    @Test
    void testAiEvalExperimentsReadsThroughTheGuardedFacade() {
        List<AiEvalExperimentView> expected = List.of();

        when(aiEvalExperimentQueryFacade.getAiEvalExperiments(WORKSPACE_ID)).thenReturn(expected);

        assertThat(aiEvalExperimentGraphQlController.aiEvalExperiments(WORKSPACE_ID)).isSameAs(expected);

        verify(aiEvalExperimentQueryFacade).getAiEvalExperiments(WORKSPACE_ID);
    }

    @Test
    void testAiEvalExperimentRunByTraceIdReadsThroughTheGuardedFacade() {
        AiEvalExperimentRunView expected = new AiEvalExperimentRunView(
            1L, EXPERIMENT_ID, 101L, TRACE_ID, "COMPLETED", 120, null, null, null);

        when(aiEvalExperimentQueryFacade.fetchAiEvalExperimentRunByTraceId(TRACE_ID)).thenReturn(expected);

        assertThat(aiEvalExperimentGraphQlController.aiEvalExperimentRunByTraceId(TRACE_ID)).isSameAs(expected);

        verify(aiEvalExperimentQueryFacade).fetchAiEvalExperimentRunByTraceId(TRACE_ID);
    }

    @Test
    void testAiEvalExperimentRunsReadsThroughTheGuardedFacade() {
        List<AiEvalExperimentRunView> expected = List.of();

        when(aiEvalExperimentQueryFacade.getAiEvalExperimentRuns(EXPERIMENT_ID)).thenReturn(expected);

        assertThat(aiEvalExperimentGraphQlController.aiEvalExperimentRuns(EXPERIMENT_ID)).isSameAs(expected);

        verify(aiEvalExperimentQueryFacade).getAiEvalExperimentRuns(EXPERIMENT_ID);
    }

    @Test
    void testExperimentComparisonDelegatesToFacade() {
        ExperimentComparisonView expected = new ExperimentComparisonView(List.of(), List.of(), List.of());

        when(aiEvalExperimentComparisonFacade.experimentComparison(List.of(EXPERIMENT_ID))).thenReturn(expected);

        assertThat(aiEvalExperimentGraphQlController.experimentComparison(List.of(EXPERIMENT_ID))).isSameAs(expected);
    }

    @Test
    void testControllerHoldsNoAuthorizationOrDataCollaborators() {
        assertThat(Arrays.stream(AiEvalExperimentGraphQlController.class.getDeclaredFields())
            .map(Field::getType))
                .as("authorization belongs on the facades; the controller must assemble nothing out of raw services")
                .doesNotContain(
                    PermissionService.class, AiEvalExperimentService.class, AiEvalExperimentRunService.class,
                    WorkspaceAiEvalExperimentService.class);
    }
}

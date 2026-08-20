/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalExperimentRunView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalExperimentView;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRunStatus;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pins the list-and-runs queries that moved off {@code AiEvalExperimentGraphQlController}'s method bodies onto this
 * facade: workspace-boundary enforcement and denormalized run-count assembly. The comparison-view assembly and its
 * {@code ADMIN} guard live on the sibling {@link AiEvalExperimentComparisonFacade}; those tests are in
 * {@code AiEvalExperimentComparisonFacadeImplTest} and {@code AiEvalExperimentComparisonFacadeAuthorizationTest}.
 *
 * <p>
 * {@code testAiEvalExperimentsRejectsCallerWithoutWorkspaceViewerRole} and
 * {@code testAiEvalExperimentRunsRejectsCrossWorkspaceExperiment} go red if the corresponding guard is deleted: each
 * asserts a rejection where the unguarded code would have returned rows.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiEvalExperimentQueryFacadeImplTest {

    private static final long EXPERIMENT_A_ID = 10L;
    private static final long EXPERIMENT_B_ID = 20L;
    private static final long WORKSPACE_ID = 1L;
    private static final long DATASET_ITEM_1 = 101L;
    private static final long DATASET_ITEM_2 = 102L;
    private static final long TRACE_ID = 501L;

    @Mock
    private AiEvalExperimentService aiEvalExperimentService;

    @Mock
    private com.bytechef.ee.automation.ai.eval.experiment.service.WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService;

    @Mock
    private AiEvalExperimentRunService aiEvalExperimentRunService;

    @Mock
    private PermissionService permissionService;

    private AiEvalExperimentQueryFacade facade;

    @BeforeEach
    void setUp() {
        facade = new AiEvalExperimentQueryFacadeImpl(
            aiEvalExperimentService, workspaceAiEvalExperimentService, aiEvalExperimentRunService, permissionService);

        // Default to allowing access; cross-workspace tests override this for the offending workspace. Lenient because
        // the null-id test paths never reach the permission check.
        lenient()
            .when(permissionService.hasWorkspaceRole(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.eq("VIEWER")))
            .thenReturn(true);
    }

    @Test
    void testAiEvalExperimentsReturnsListWithDenormalizedRunCounts() {
        AiEvalExperiment experimentA = newExperiment(EXPERIMENT_A_ID, "gpt-4");
        AiEvalExperiment experimentB = newExperiment(EXPERIMENT_B_ID, "gpt-4o");

        when(workspaceAiEvalExperimentService.findAllByWorkspace(WORKSPACE_ID))
            .thenReturn(List.of(experimentA, experimentB));

        when(aiEvalExperimentRunService.countByExperiment(EXPERIMENT_A_ID)).thenReturn(3L);
        when(aiEvalExperimentRunService.countByExperiment(EXPERIMENT_B_ID)).thenReturn(2L);
        when(
            aiEvalExperimentRunService.countByExperimentAndStatus(EXPERIMENT_A_ID, AiEvalExperimentRunStatus.COMPLETED))
                .thenReturn(2L);
        when(aiEvalExperimentRunService.countByExperimentAndStatus(EXPERIMENT_A_ID, AiEvalExperimentRunStatus.FAILED))
            .thenReturn(1L);
        when(
            aiEvalExperimentRunService.countByExperimentAndStatus(EXPERIMENT_B_ID, AiEvalExperimentRunStatus.COMPLETED))
                .thenReturn(2L);
        when(aiEvalExperimentRunService.countByExperimentAndStatus(EXPERIMENT_B_ID, AiEvalExperimentRunStatus.FAILED))
            .thenReturn(0L);

        List<AiEvalExperimentView> views = facade.getAiEvalExperiments(WORKSPACE_ID);

        assertThat(views).hasSize(2);
        assertThat(views.get(0)
            .id()).isEqualTo(EXPERIMENT_A_ID);
        assertThat(views.get(0)
            .totalRuns()).isEqualTo(3L);
        assertThat(views.get(0)
            .completedRuns()).isEqualTo(2L);
        assertThat(views.get(0)
            .failedRuns()).isEqualTo(1L);
        assertThat(views.get(1)
            .id()).isEqualTo(EXPERIMENT_B_ID);
        assertThat(views.get(1)
            .totalRuns()).isEqualTo(2L);
    }

    @Test
    void testAiEvalExperimentsReturnsEmptyListWhenWorkspaceIdIsNull() {
        // Defense-in-depth check: a null workspaceId from a malformed GraphQL request must NOT reach the
        // permission service (which could throw or, depending on implementation, return false and emit a
        // misleading WORKSPACE/null boundary exception). Returning an empty list early matches the
        // null-id behaviour on the singleton lookups elsewhere in the gateway.
        List<AiEvalExperimentView> views = facade.getAiEvalExperiments(null);

        assertThat(views).isEmpty();
    }

    @Test
    void testAiEvalExperimentsRejectsCallerWithoutWorkspaceViewerRole() {
        // A caller who is a workspace member but lacks the VIEWER role on the requested workspace must NOT receive
        // an empty list (which would be indistinguishable from "workspace exists but has no experiments"); they must
        // hit the uniform AiScoreWorkspaceBoundaryException so existence cannot be probed cross-workspace.
        long foreignWorkspaceId = 99L;

        when(permissionService.hasWorkspaceRole(foreignWorkspaceId, "VIEWER")).thenReturn(false);

        assertThatThrownBy(() -> facade.getAiEvalExperiments(foreignWorkspaceId))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessageContaining("workspace " + foreignWorkspaceId);
    }

    @Test
    void testAiEvalExperimentRunsReturnsRunsForVisibleExperiment() {
        AiEvalExperiment experiment = newExperiment(EXPERIMENT_A_ID, "gpt-4");

        AiEvalExperimentRun runOne = completedRun(1L, EXPERIMENT_A_ID, DATASET_ITEM_1, 501L, 120, "0.01");
        AiEvalExperimentRun runTwo = completedRun(2L, EXPERIMENT_A_ID, DATASET_ITEM_2, 502L, 140, "0.02");

        when(aiEvalExperimentService.getExperiment(EXPERIMENT_A_ID)).thenReturn(experiment);
        when(aiEvalExperimentRunService.findAllByExperiment(EXPERIMENT_A_ID)).thenReturn(List.of(runOne, runTwo));

        List<AiEvalExperimentRunView> runs = facade.getAiEvalExperimentRuns(EXPERIMENT_A_ID);

        assertThat(runs).hasSize(2);
        assertThat(runs.get(0)
            .id()).isEqualTo(1L);
        assertThat(runs.get(0)
            .traceId()).isEqualTo(501L);
        assertThat(runs.get(0)
            .status()).isEqualTo("COMPLETED");
        assertThat(runs.get(0)
            .latencyMs()).isEqualTo(120);
        assertThat(runs.get(0)
            .cost()).isEqualByComparingTo("0.01");
        assertThat(runs.get(1)
            .id()).isEqualTo(2L);
    }

    @Test
    void testAiEvalExperimentRunsReturnsEmptyListWhenExperimentIdIsNull() {
        List<AiEvalExperimentRunView> runs = facade.getAiEvalExperimentRuns(null);

        assertThat(runs).isEmpty();
    }

    @Test
    void testAiEvalExperimentRunsCollapsesNotFoundOntoBoundaryException() {
        // Uniform-error contract for the runs query: a missing experiment id must NOT throw the
        // service-layer IllegalArgumentException ("Experiment X not found"). Distinguishable error
        // messages between "exists but in another workspace" and "does not exist" let workspace-A
        // probe workspace-B's experiment ids.
        long missingId = 9_999L;

        when(aiEvalExperimentService.getExperiment(missingId))
            .thenThrow(new IllegalArgumentException("Experiment " + missingId + " not found"));

        assertThatThrownBy(() -> facade.getAiEvalExperimentRuns(missingId))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessageContaining("experiment " + missingId);
    }

    @Test
    void testAiEvalExperimentRunsRejectsCrossWorkspaceExperiment() {
        long foreignWorkspaceId = 99L;

        AiEvalExperiment foreignExperiment = newExperimentInWorkspace(EXPERIMENT_A_ID, "gpt-4", foreignWorkspaceId);

        when(aiEvalExperimentService.getExperiment(EXPERIMENT_A_ID)).thenReturn(foreignExperiment);
        when(permissionService.hasWorkspaceRole(foreignWorkspaceId, "VIEWER")).thenReturn(false);

        assertThatThrownBy(() -> facade.getAiEvalExperimentRuns(EXPERIMENT_A_ID))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessageContaining("experiment " + EXPERIMENT_A_ID)
            .hasMessageNotContaining("workspace " + foreignWorkspaceId);
    }

    @Test
    void testAiEvalExperimentRunsReturnsSameErrorShapeForNotFoundAndCrossWorkspace() {
        // Structural symmetry: stripping the id from each message must yield identical strings — a
        // regression that distinguishes the two paths (e.g. one path appends "(not found)" while the
        // other appends a workspace hint) would still pass startsWith() but fails this isEqualTo +
        // strip-id symmetric check.
        long missingId = 9_999L;
        long foreignId = EXPERIMENT_A_ID;
        long foreignWorkspaceId = 99L;

        AiEvalExperiment foreignExperiment = newExperimentInWorkspace(foreignId, "gpt-4", foreignWorkspaceId);

        when(aiEvalExperimentService.getExperiment(missingId))
            .thenThrow(new IllegalArgumentException("Experiment " + missingId + " not found"));
        when(aiEvalExperimentService.getExperiment(foreignId)).thenReturn(foreignExperiment);
        when(permissionService.hasWorkspaceRole(foreignWorkspaceId, "VIEWER")).thenReturn(false);

        Throwable missingThrown =
            org.assertj.core.api.Assertions.catchThrowable(() -> facade.getAiEvalExperimentRuns(missingId));
        Throwable foreignThrown =
            org.assertj.core.api.Assertions.catchThrowable(() -> facade.getAiEvalExperimentRuns(foreignId));

        assertThat(missingThrown).isInstanceOf(AiScoreWorkspaceBoundaryException.class);
        assertThat(foreignThrown).isInstanceOf(AiScoreWorkspaceBoundaryException.class);
        assertThat(missingThrown.getMessage()
            .replace(Long.toString(missingId), "<id>"))
                .isEqualTo(foreignThrown.getMessage()
                    .replace(Long.toString(foreignId), "<id>"));
    }

    @Test
    void testAiEvalExperimentRunByTraceIdReturnsRunForVisibleExperiment() {
        AiEvalExperimentRun run = completedRun(1L, EXPERIMENT_A_ID, DATASET_ITEM_1, TRACE_ID, 120, "0.01");
        AiEvalExperiment experiment = newExperiment(EXPERIMENT_A_ID, "gpt-4");

        when(aiEvalExperimentRunService.findByTraceId(TRACE_ID)).thenReturn(Optional.of(run));
        when(aiEvalExperimentService.getExperiment(EXPERIMENT_A_ID)).thenReturn(experiment);

        AiEvalExperimentRunView view = facade.fetchAiEvalExperimentRunByTraceId(TRACE_ID);

        assertThat(view).isNotNull();
        assertThat(view.traceId()).isEqualTo(TRACE_ID);
    }

    @Test
    void testAiEvalExperimentRunByTraceIdHidesRunFromAnotherWorkspace() {
        // The cross-link must not leak the existence of a foreign experiment run behind a trace id. Unlike the two
        // list queries this one hides by returning null rather than throwing, so the assertion has to be "no row",
        // not "rejected" — dropping the guard here would surface the foreign run instead.
        long foreignWorkspaceId = 99L;

        AiEvalExperimentRun run = completedRun(1L, EXPERIMENT_A_ID, DATASET_ITEM_1, TRACE_ID, 120, "0.01");
        AiEvalExperiment foreignExperiment = newExperimentInWorkspace(EXPERIMENT_A_ID, "gpt-4", foreignWorkspaceId);

        when(aiEvalExperimentRunService.findByTraceId(TRACE_ID)).thenReturn(Optional.of(run));
        when(aiEvalExperimentService.getExperiment(EXPERIMENT_A_ID)).thenReturn(foreignExperiment);
        when(permissionService.hasWorkspaceRole(foreignWorkspaceId, "VIEWER")).thenReturn(false);

        assertThat(facade.fetchAiEvalExperimentRunByTraceId(TRACE_ID)).isNull();
    }

    @Test
    void testAiEvalExperimentRunByTraceIdReturnsNullWhenTraceIdIsNull() {
        assertThat(facade.fetchAiEvalExperimentRunByTraceId(null)).isNull();
    }

    private AiEvalExperiment newExperiment(long id, String model) {
        return newExperimentInWorkspace(id, model, WORKSPACE_ID);
    }

    private AiEvalExperiment newExperimentInWorkspace(long id, String model, long workspaceId) {
        AiEvalExperiment experiment = new AiEvalExperiment(1L);

        experiment.setModel(model);

        ReflectionTestUtils.setField(experiment, "id", id);

        lenient()
            .when(workspaceAiEvalExperimentService.getWorkspaceId(id))
            .thenReturn(workspaceId);

        return experiment;
    }

    private static AiEvalExperimentRun completedRun(
        long runId, long experimentId, long datasetItemId, Long traceId, Integer latencyMs, String cost) {

        AiEvalExperimentRun run = new AiEvalExperimentRun(experimentId, datasetItemId);

        run.markRunning();
        run.complete(traceId, latencyMs, new BigDecimal(cost));

        ReflectionTestUtils.setField(run, "id", runId);

        return run;
    }
}

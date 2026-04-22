/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.eval.experiment.service.WorkspaceAiEvalExperimentService;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AggregateScoreDelta;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ExperimentComparisonRow;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ExperimentComparisonView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ExperimentRunPoint;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ExperimentScoreAverage;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ExperimentSummary;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreSource;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRunStatus;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalScoreService;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pins the comparison-view shape produced by {@link AiEvalExperimentComparisonFacadeImpl}: two experiments sharing
 * three dataset items produce two summaries, three rows, and a numeric-score aggregate per score name. Moved off the
 * controller test when the comparison assembly and its {@code ADMIN} guard relocated to the facade.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiEvalExperimentComparisonFacadeImplTest {

    private static final long EXPERIMENT_A_ID = 10L;
    private static final long EXPERIMENT_B_ID = 20L;
    private static final long WORKSPACE_ID = 1L;
    private static final long DATASET_ITEM_1 = 101L;
    private static final long DATASET_ITEM_2 = 102L;
    private static final long DATASET_ITEM_3 = 103L;

    @Mock
    private AiEvalExperimentService aiEvalExperimentService;

    @Mock
    private WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService;

    @Mock
    private AiEvalExperimentRunService aiEvalExperimentRunService;

    @Mock
    private AiEvalScoreService aiEvalScoreService;

    @Mock
    private PermissionService permissionService;

    private AiEvalExperimentComparisonFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new AiEvalExperimentComparisonFacadeImpl(
            aiEvalExperimentService, workspaceAiEvalExperimentService, aiEvalExperimentRunService,
            aiEvalScoreService, permissionService);

        lenient()
            .when(permissionService.hasWorkspaceRole(anyLong(), eq("VIEWER")))
            .thenReturn(true);
    }

    @Test
    void testExperimentComparisonReturnsExpectedShape() {
        AiEvalExperiment experimentA = newExperiment(EXPERIMENT_A_ID, "gpt-4");
        AiEvalExperiment experimentB = newExperiment(EXPERIMENT_B_ID, "gpt-4o");

        AiEvalExperimentRun runA1 = completedRun(1L, EXPERIMENT_A_ID, DATASET_ITEM_1, 501L, 120, "0.01");
        AiEvalExperimentRun runA2 = completedRun(2L, EXPERIMENT_A_ID, DATASET_ITEM_2, 502L, 140, "0.02");
        AiEvalExperimentRun runA3 = completedRun(3L, EXPERIMENT_A_ID, DATASET_ITEM_3, 503L, 100, "0.015");

        AiEvalExperimentRun runB1 = completedRun(4L, EXPERIMENT_B_ID, DATASET_ITEM_1, 601L, 80, "0.02");
        AiEvalExperimentRun runB2 = completedRun(5L, EXPERIMENT_B_ID, DATASET_ITEM_2, 602L, 90, "0.03");
        AiEvalExperimentRun runB3 = completedRun(6L, EXPERIMENT_B_ID, DATASET_ITEM_3, 603L, 70, "0.025");

        when(aiEvalExperimentService.getExperiment(EXPERIMENT_A_ID)).thenReturn(experimentA);
        when(aiEvalExperimentService.getExperiment(EXPERIMENT_B_ID)).thenReturn(experimentB);

        when(aiEvalExperimentRunService.findAllByExperiment(EXPERIMENT_A_ID))
            .thenReturn(List.of(runA1, runA2, runA3));
        when(aiEvalExperimentRunService.findAllByExperiment(EXPERIMENT_B_ID))
            .thenReturn(List.of(runB1, runB2, runB3));

        when(aiEvalExperimentRunService.countByExperiment(EXPERIMENT_A_ID)).thenReturn(3L);
        when(aiEvalExperimentRunService.countByExperiment(EXPERIMENT_B_ID)).thenReturn(3L);
        when(aiEvalExperimentRunService.countByExperimentAndStatus(anyLong(), eq(AiEvalExperimentRunStatus.COMPLETED)))
            .thenReturn(3L);
        when(aiEvalExperimentRunService.countByExperimentAndStatus(anyLong(), eq(AiEvalExperimentRunStatus.FAILED)))
            .thenReturn(0L);

        // Each trace has the same NUMERIC score name "accuracy" so we can verify aggregation.
        when(aiEvalScoreService.getScoresByTrace(501L)).thenReturn(List.of(numericScore(501L, "accuracy", "0.9")));
        when(aiEvalScoreService.getScoresByTrace(502L)).thenReturn(List.of(numericScore(502L, "accuracy", "0.8")));
        when(aiEvalScoreService.getScoresByTrace(503L)).thenReturn(List.of(numericScore(503L, "accuracy", "0.7")));
        when(aiEvalScoreService.getScoresByTrace(601L)).thenReturn(List.of(numericScore(601L, "accuracy", "0.95")));
        when(aiEvalScoreService.getScoresByTrace(602L)).thenReturn(List.of(numericScore(602L, "accuracy", "0.85")));
        when(aiEvalScoreService.getScoresByTrace(603L)).thenReturn(List.of(numericScore(603L, "accuracy", "0.9")));

        ExperimentComparisonView view = facade.experimentComparison(
            List.of(EXPERIMENT_A_ID, EXPERIMENT_B_ID));

        assertThat(view.experiments()).hasSize(2);
        assertThat(view.rows()).hasSize(3);

        ExperimentSummary summaryA = view.experiments()
            .getFirst();

        assertThat(summaryA.id()).isEqualTo(EXPERIMENT_A_ID);
        assertThat(summaryA.model()).isEqualTo("gpt-4");
        assertThat(summaryA.totalRuns()).isEqualTo(3L);
        assertThat(summaryA.completedRuns()).isEqualTo(3L);
        assertThat(summaryA.failedRuns()).isEqualTo(0L);
        assertThat(summaryA.totalCost()).isEqualByComparingTo("0.045");
        assertThat(summaryA.averageLatencyMs()).isEqualTo(120);

        ExperimentComparisonRow firstRow = view.rows()
            .getFirst();

        assertThat(firstRow.datasetItemId()).isEqualTo(DATASET_ITEM_1);
        assertThat(firstRow.runsByExperiment()).hasSize(2);

        ExperimentRunPoint pointA = firstRow.runsByExperiment()
            .getFirst();

        assertThat(pointA.experimentId()).isEqualTo(EXPERIMENT_A_ID);
        assertThat(pointA.traceId()).isEqualTo(501L);
        assertThat(pointA.status()).isEqualTo("COMPLETED");
        assertThat(pointA.scores()).hasSize(1);
        assertThat(pointA.scores()
            .getFirst()
            .name()).isEqualTo("accuracy");

        assertThat(view.aggregateScoreDeltas()).hasSize(1);

        AggregateScoreDelta delta = view.aggregateScoreDeltas()
            .getFirst();

        assertThat(delta.scoreName()).isEqualTo("accuracy");
        assertThat(delta.deltas()).hasSize(2);

        ExperimentScoreAverage averageA = delta.deltas()
            .stream()
            .filter(entry -> entry.experimentId() == EXPERIMENT_A_ID)
            .findFirst()
            .orElseThrow();

        assertThat(averageA.count()).isEqualTo(3L);
        assertThat(averageA.average()
            .doubleValue()).isEqualTo(0.8, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void testExperimentComparisonSkipsBooleanAndCategoricalForAggregates() {
        AiEvalExperiment experiment = newExperiment(EXPERIMENT_A_ID, "gpt-4");

        AiEvalExperimentRun run = completedRun(1L, EXPERIMENT_A_ID, DATASET_ITEM_1, 501L, 100, "0.01");

        when(aiEvalExperimentService.getExperiment(EXPERIMENT_A_ID)).thenReturn(experiment);
        when(aiEvalExperimentRunService.findAllByExperiment(EXPERIMENT_A_ID)).thenReturn(List.of(run));
        when(aiEvalExperimentRunService.countByExperiment(EXPERIMENT_A_ID)).thenReturn(1L);
        when(aiEvalExperimentRunService.countByExperimentAndStatus(anyLong(), any()))
            .thenReturn(0L);

        when(aiEvalScoreService.getScoresByTrace(501L)).thenReturn(List.of(
            AiEvalScore.bool(501L, "passed", AiEvalScoreSource.MANUAL, true),
            AiEvalScore.categorical(501L, "sentiment", AiEvalScoreSource.MANUAL, "positive"),
            numericScore(501L, "accuracy", "0.9")));

        ExperimentComparisonView view = facade.experimentComparison(List.of(EXPERIMENT_A_ID));

        assertThat(view.rows()).hasSize(1);
        assertThat(view.rows()
            .getFirst()
            .runsByExperiment()
            .getFirst()
            .scores()).hasSize(3);

        // Only the NUMERIC score appears in aggregates.
        assertThat(view.aggregateScoreDeltas()).hasSize(1);
        assertThat(view.aggregateScoreDeltas()
            .getFirst()
            .scoreName()).isEqualTo("accuracy");
    }

    @Test
    void testEmptyExperimentIdsReturnsEmptyView() {
        ExperimentComparisonView view = facade.experimentComparison(List.of());

        assertThat(view.experiments()).isEmpty();
        assertThat(view.rows()).isEmpty();
        assertThat(view.aggregateScoreDeltas()).isEmpty();
    }

    @Test
    void testExperimentComparisonRejectsCrossWorkspaceExperiment() {
        // Caller is an admin (the @PreAuthorize gate is satisfied) but is NOT a member of workspace 99 — passing an
        // experiment id from that workspace must be rejected before any data is read.
        long crossWorkspaceId = 99L;
        AiEvalExperiment crossWorkspaceExperiment =
            newExperimentInWorkspace(EXPERIMENT_A_ID, "gpt-4", crossWorkspaceId);

        when(aiEvalExperimentService.getExperiment(EXPERIMENT_A_ID)).thenReturn(crossWorkspaceExperiment);
        when(permissionService.hasWorkspaceRole(crossWorkspaceId, "VIEWER")).thenReturn(false);

        assertThatThrownBy(() -> facade.experimentComparison(List.of(EXPERIMENT_A_ID)))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessageContaining("experiment " + EXPERIMENT_A_ID)
            .hasMessageNotContaining("workspace " + crossWorkspaceId);
    }

    @Test
    void testExperimentComparisonReturnsSameErrorShapeForNotFoundAndCrossWorkspace() {
        long missingId = 9_999L;
        long foreignId = EXPERIMENT_A_ID;
        long foreignWorkspaceId = 99L;

        AiEvalExperiment foreignExperiment = newExperimentInWorkspace(foreignId, "gpt-4", foreignWorkspaceId);

        when(aiEvalExperimentService.getExperiment(missingId))
            .thenThrow(new IllegalArgumentException("Experiment " + missingId + " not found"));
        when(aiEvalExperimentService.getExperiment(foreignId)).thenReturn(foreignExperiment);
        when(permissionService.hasWorkspaceRole(foreignWorkspaceId, "VIEWER")).thenReturn(false);

        Throwable missingThrown =
            org.assertj.core.api.Assertions.catchThrowable(() -> facade.experimentComparison(List.of(missingId)));
        Throwable foreignThrown =
            org.assertj.core.api.Assertions.catchThrowable(() -> facade.experimentComparison(List.of(foreignId)));

        assertThat(missingThrown).isInstanceOf(AiScoreWorkspaceBoundaryException.class);
        assertThat(foreignThrown).isInstanceOf(AiScoreWorkspaceBoundaryException.class);

        assertThat(missingThrown.getMessage())
            .isEqualTo("Caller is not authorized for experiment " + missingId);
        assertThat(foreignThrown.getMessage())
            .isEqualTo("Caller is not authorized for experiment " + foreignId);

        assertThat(missingThrown.getMessage()
            .replace(Long.toString(missingId), "<id>"))
                .isEqualTo(foreignThrown.getMessage()
                    .replace(Long.toString(foreignId), "<id>"));
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

    private static AiEvalScore numericScore(long traceId, String name, String value) {
        return AiEvalScore.numeric(traceId, name, AiEvalScoreSource.LLM_JUDGE, new BigDecimal(value));
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.eval.experiment.service.WorkspaceAiEvalExperimentService;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AggregateScoreDelta;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ExperimentComparisonRow;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ExperimentComparisonView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ExperimentRunPoint;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ExperimentScoreAverage;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ExperimentSummary;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.ScorePoint;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreValue;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRunStatus;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalScoreService;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetType;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiEvalExperimentComparisonFacadeImpl implements AiEvalExperimentComparisonFacade {

    private static final String MINIMUM_VIEWER_ROLE = "VIEWER";

    private final AiEvalExperimentService aiEvalExperimentService;
    private final WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService;
    private final AiEvalExperimentRunService aiEvalExperimentRunService;
    private final AiEvalScoreService aiEvalScoreService;
    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    AiEvalExperimentComparisonFacadeImpl(
        AiEvalExperimentService aiEvalExperimentService,
        WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService,
        AiEvalExperimentRunService aiEvalExperimentRunService,
        AiEvalScoreService aiEvalScoreService, PermissionService permissionService) {

        this.aiEvalExperimentService = aiEvalExperimentService;
        this.workspaceAiEvalExperimentService = workspaceAiEvalExperimentService;
        this.aiEvalExperimentRunService = aiEvalExperimentRunService;
        this.aiEvalScoreService = aiEvalScoreService;
        this.permissionService = permissionService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ExperimentComparisonView experimentComparison(List<Long> experimentIds) {
        if (experimentIds == null || experimentIds.isEmpty()) {
            return new ExperimentComparisonView(List.of(), List.of(), List.of());
        }

        List<AiEvalExperiment> experiments = new ArrayList<>(experimentIds.size());

        for (Long experimentId : experimentIds) {
            AiEvalExperiment experiment;

            try {
                experiment = aiEvalExperimentService.getExperiment(experimentId);
            } catch (IllegalArgumentException notFound) {
                throw AiScoreWorkspaceBoundaryException.forTarget(
                    null, AiScoreTargetType.EXPERIMENT, experimentId, notFound);
            }

            Long workspaceId = workspaceAiEvalExperimentService.getWorkspaceId(experiment.getId());

            if (workspaceId == null || !permissionService.hasWorkspaceRole(workspaceId, MINIMUM_VIEWER_ROLE)) {
                throw AiScoreWorkspaceBoundaryException.forTarget(
                    workspaceId, AiScoreTargetType.EXPERIMENT, experimentId);
            }

            experiments.add(experiment);
        }

        Map<Long, List<AiEvalExperimentRun>> runsByExperiment = new LinkedHashMap<>();

        for (AiEvalExperiment experiment : experiments) {
            runsByExperiment.put(
                experiment.getId(), aiEvalExperimentRunService.findAllByExperiment(experiment.getId()));
        }

        Map<Long, List<AiEvalScore>> scoresByTrace = new HashMap<>();

        List<ExperimentSummary> summaries = experiments.stream()
            .map(experiment -> buildSummary(experiment, runsByExperiment.get(experiment.getId())))
            .toList();

        Set<Long> allDatasetItemIds = new TreeSet<>();

        for (List<AiEvalExperimentRun> runs : runsByExperiment.values()) {
            for (AiEvalExperimentRun run : runs) {
                allDatasetItemIds.add(run.getDatasetItemId());
            }
        }

        List<ExperimentComparisonRow> rows = allDatasetItemIds.stream()
            .map(itemId -> buildRow(itemId, experiments, runsByExperiment, scoresByTrace))
            .toList();

        List<AggregateScoreDelta> aggregates = buildAggregateDeltas(runsByExperiment, scoresByTrace);

        return new ExperimentComparisonView(summaries, rows, aggregates);
    }

    private ExperimentSummary buildSummary(AiEvalExperiment experiment, List<AiEvalExperimentRun> runs) {
        long total = aiEvalExperimentRunService.countByExperiment(experiment.getId());
        long completed = aiEvalExperimentRunService.countByExperimentAndStatus(
            experiment.getId(), AiEvalExperimentRunStatus.COMPLETED);
        long failed = aiEvalExperimentRunService.countByExperimentAndStatus(
            experiment.getId(), AiEvalExperimentRunStatus.FAILED);

        BigDecimal totalCost = runs.stream()
            .map(AiEvalExperimentRun::getCost)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long latencySum = 0L;
        int latencyCount = 0;

        for (AiEvalExperimentRun run : runs) {
            Integer latency = run.getLatencyMs();

            if (latency != null) {
                latencySum += latency;
                latencyCount++;
            }
        }

        Integer averageLatencyMs = latencyCount == 0 ? null : (int) (latencySum / latencyCount);

        return new ExperimentSummary(
            experiment.getId(), experiment.getModel(), total, completed, failed, totalCost, averageLatencyMs);
    }

    private ExperimentComparisonRow buildRow(
        Long datasetItemId, List<AiEvalExperiment> experiments, Map<Long, List<AiEvalExperimentRun>> runsByExperiment,
        Map<Long, List<AiEvalScore>> scoresByTrace) {

        List<ExperimentRunPoint> points = new ArrayList<>();

        for (AiEvalExperiment experiment : experiments) {
            AiEvalExperimentRun matchingRun = null;

            for (AiEvalExperimentRun run : runsByExperiment.get(experiment.getId())) {
                if (Objects.equals(run.getDatasetItemId(), datasetItemId)) {
                    matchingRun = run;

                    break;
                }
            }

            if (matchingRun == null) {
                continue;
            }

            List<ScorePoint> scorePoints = loadScores(matchingRun.getTraceId(), scoresByTrace).stream()
                .map(this::toScorePoint)
                .toList();

            points.add(new ExperimentRunPoint(
                experiment.getId(), matchingRun.getId(), matchingRun.getTraceId(),
                matchingRun.getStatus()
                    .name(),
                matchingRun.getCost(), matchingRun.getLatencyMs(), scorePoints));
        }

        return new ExperimentComparisonRow(datasetItemId, points);
    }

    private List<AiEvalScore> loadScores(Long traceId, Map<Long, List<AiEvalScore>> scoresByTrace) {
        if (traceId == null) {
            return List.of();
        }

        return scoresByTrace.computeIfAbsent(traceId, aiEvalScoreService::getScoresByTrace);
    }

    private ScorePoint toScorePoint(AiEvalScore score) {
        AiEvalScoreDataType dataType = score.getDataType();
        AiEvalScoreValue typedValue = score.getTypedValue();

        BigDecimal numericValue;
        String stringValue;

        if (typedValue instanceof AiEvalScoreValue.Numeric numeric) {
            numericValue = numeric.value();
            stringValue = null;
        } else if (typedValue instanceof AiEvalScoreValue.Bool bool) {
            numericValue = bool.asNumeric();
            stringValue = bool.asString();
        } else if (typedValue instanceof AiEvalScoreValue.Categorical categorical) {
            numericValue = null;
            stringValue = categorical.label();
        } else {
            numericValue = null;
            stringValue = null;
        }

        return new ScorePoint(
            score.getName(), numericValue, stringValue, dataType == null ? null : dataType.name());
    }

    private List<AggregateScoreDelta> buildAggregateDeltas(
        Map<Long, List<AiEvalExperimentRun>> runsByExperiment, Map<Long, List<AiEvalScore>> scoresByTrace) {

        Map<String, Map<Long, List<BigDecimal>>> valuesByNameByExperiment = new LinkedHashMap<>();

        for (Map.Entry<Long, List<AiEvalExperimentRun>> entry : runsByExperiment.entrySet()) {
            Long experimentId = entry.getKey();

            for (AiEvalExperimentRun run : entry.getValue()) {
                List<AiEvalScore> scores = loadScores(run.getTraceId(), scoresByTrace);

                for (AiEvalScore score : scores) {
                    if (score.getDataType() != AiEvalScoreDataType.NUMERIC) {
                        continue;
                    }

                    if (score.getTypedValue() instanceof AiEvalScoreValue.Numeric numeric) {
                        valuesByNameByExperiment
                            .computeIfAbsent(score.getName(), key -> new LinkedHashMap<>())
                            .computeIfAbsent(experimentId, key -> new ArrayList<>())
                            .add(numeric.value());
                    }
                }
            }
        }

        List<AggregateScoreDelta> deltas = new ArrayList<>();

        for (Map.Entry<String, Map<Long, List<BigDecimal>>> scoreEntry : valuesByNameByExperiment.entrySet()) {
            String scoreName = scoreEntry.getKey();
            List<ExperimentScoreAverage> averages = new ArrayList<>();

            for (Map.Entry<Long, List<BigDecimal>> perExperiment : scoreEntry.getValue()
                .entrySet()) {

                List<BigDecimal> values = perExperiment.getValue();

                BigDecimal sum = values.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal average = sum.divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL64);

                averages.add(new ExperimentScoreAverage(perExperiment.getKey(), average, values.size()));
            }

            deltas.add(new AggregateScoreDelta(scoreName, Collections.unmodifiableList(averages)));
        }

        return deltas;
    }
}

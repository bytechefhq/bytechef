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
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalExperimentRunView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalExperimentView;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRunStatus;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetType;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiEvalExperimentQueryFacadeImpl implements AiEvalExperimentQueryFacade {

    private static final String MINIMUM_VIEWER_ROLE = "VIEWER";

    private final AiEvalExperimentService aiEvalExperimentService;
    private final WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService;
    private final AiEvalExperimentRunService aiEvalExperimentRunService;
    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    AiEvalExperimentQueryFacadeImpl(
        AiEvalExperimentService aiEvalExperimentService,
        WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService,
        AiEvalExperimentRunService aiEvalExperimentRunService, PermissionService permissionService) {

        this.aiEvalExperimentService = aiEvalExperimentService;
        this.workspaceAiEvalExperimentService = workspaceAiEvalExperimentService;
        this.aiEvalExperimentRunService = aiEvalExperimentRunService;
        this.permissionService = permissionService;
    }

    @Override
    public List<AiEvalExperimentView> getAiEvalExperiments(Long workspaceId) {
        if (workspaceId == null) {
            return List.of();
        }

        if (!permissionService.hasWorkspaceRole(workspaceId, MINIMUM_VIEWER_ROLE)) {
            throw AiScoreWorkspaceBoundaryException.forTarget(
                workspaceId, AiScoreTargetType.WORKSPACE, workspaceId);
        }

        List<AiEvalExperiment> experiments = workspaceAiEvalExperimentService.findAllByWorkspace(workspaceId);

        return experiments.stream()
            .map(this::buildView)
            .toList();
    }

    @Override
    public AiEvalExperimentRunView fetchAiEvalExperimentRunByTraceId(Long traceId) {
        if (traceId == null) {
            return null;
        }

        return aiEvalExperimentRunService.findByTraceId(traceId)
            .map(run -> {
                AiEvalExperiment experiment = aiEvalExperimentService.getExperiment(run.getExperimentId());

                Long workspaceId = workspaceAiEvalExperimentService.getWorkspaceId(experiment.getId());

                if (workspaceId == null || !permissionService.hasWorkspaceRole(workspaceId, MINIMUM_VIEWER_ROLE)) {
                    return null;
                }

                return toRunView(run);
            })
            .orElse(null);
    }

    @Override
    public List<AiEvalExperimentRunView> getAiEvalExperimentRuns(Long experimentId) {
        if (experimentId == null) {
            return List.of();
        }

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

        return aiEvalExperimentRunService.findAllByExperiment(experimentId)
            .stream()
            .map(AiEvalExperimentQueryFacadeImpl::toRunView)
            .toList();
    }

    private AiEvalExperimentView buildView(AiEvalExperiment experiment) {
        long total = aiEvalExperimentRunService.countByExperiment(experiment.getId());
        long completed = aiEvalExperimentRunService.countByExperimentAndStatus(
            experiment.getId(), AiEvalExperimentRunStatus.COMPLETED);
        long failed = aiEvalExperimentRunService.countByExperimentAndStatus(
            experiment.getId(), AiEvalExperimentRunStatus.FAILED);

        return new AiEvalExperimentView(
            experiment.getId(),
            experiment.getDatasetVersionId(),
            experiment.getPromptVersionId(),
            experiment.getModel(),
            experiment.getMetadata(),
            experiment.getStatus()
                .name(),
            experiment.isStopRequested(),
            total,
            completed,
            failed,
            experiment.getCreatedDate(),
            experiment.getStartedDate(),
            experiment.getCompletedDate());
    }

    private static AiEvalExperimentRunView toRunView(AiEvalExperimentRun run) {
        return new AiEvalExperimentRunView(
            run.getId(),
            run.getExperimentId(),
            run.getDatasetItemId(),
            run.getTraceId(),
            run.getStatus()
                .name(),
            run.getLatencyMs(),
            run.getCost(),
            run.getErrorMessage(),
            run.getCreatedDate());
    }
}

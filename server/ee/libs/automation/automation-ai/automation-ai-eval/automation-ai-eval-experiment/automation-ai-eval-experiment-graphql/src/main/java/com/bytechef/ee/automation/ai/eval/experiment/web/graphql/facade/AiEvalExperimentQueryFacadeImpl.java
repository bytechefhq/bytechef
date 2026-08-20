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
 * Implementation of {@link AiEvalExperimentQueryFacade}. Carries the workspace-boundary guards that used to sit in
 * {@code AiEvalExperimentGraphQlController}'s method bodies, so they are enforced for every caller of the facade rather
 * than only the GraphQL entry point.
 *
 * <p>
 * These are role-RANK checks, not scope-token checks, and they stay that way:
 * {@link PermissionService#hasWorkspaceRole} compares against a minimum role by rank, which no {@code hasPermission}
 * scope token expresses. A separate migration will replace role-rank checks repo-wide; this relocation deliberately
 * does no part of it.
 *
 * <p>
 * Each check also guards on a null workspace id. That null is not a request parameter: it comes from
 * {@code WorkspaceAiEvalExperimentService.getWorkspaceId}, i.e. an experiment row with no workspace relation. A
 * {@code #workspaceId}-keyed annotation could not express that half, which is the other reason these guards stay
 * written out.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiEvalExperimentQueryFacadeImpl implements AiEvalExperimentQueryFacade {

    /**
     * Minimum workspace role required to view experiments. Must stay in lockstep with
     * {@code com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole#VIEWER}. Adding an EE module dep
     * here to use the typed enum directly via {@code hasWorkspaceRoleTyped} would cycle the GraphQL module on EE-only
     * configuration, so the value is duplicated as a string literal.
     *
     * <p>
     * <strong>Important:</strong> renaming {@code WorkspaceRole.VIEWER} will <em>not</em> surface as a compile-time
     * break here. The reflective {@code AiEvalExperimentQueryFacadeViewerRolePinTest#testMinimumViewerRoleMatchesEnum}
     * unit test pins this constant against {@code WorkspaceRole.VIEWER.name()} so the drift is caught at test time
     * instead.
     */
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

    /**
     * Lists experiments for the given workspace with denormalized run-count fields. The caller must have at least
     * {@code MINIMUM_VIEWER_ROLE} on the workspace; failure raises {@link AiScoreWorkspaceBoundaryException} with the
     * uniform "Caller is not authorized for X" message — workspaces the caller cannot see surface as 403, not as an
     * empty list, so existence cannot be probed.
     *
     * <p>
     * Run counts are computed via {@code countByExperimentAndStatus} per experiment — N+1 against the runs table is
     * accepted at this list boundary because the operator console paginates client-side and the alternative (a GROUP BY
     * join) would force a custom repository method for a non-hot path.
     */
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

    /**
     * Reverse lookup from a trace id to its source experiment run, for the operator-console Traces tab cross-link.
     * Returns {@code null} when the trace was not generated by an experiment replay (the common case for production API
     * traffic), so the UI can render the link conditionally without a separate "is this an experiment trace" probe. The
     * workspace-boundary check walks up via the run's experiment — same depth-2 pattern used by
     * {@code AiEvalDatasetQueryFacade.getAiEvalDatasetItems}.
     */
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
                    // Cross-workspace traces stay hidden: the trace itself is already enforced by the trace
                    // controller's workspace check, so reaching here with a foreign run is a sign of a row drift,
                    // not a probe. Return null rather than throw — the trace-detail UI uses the absence of a
                    // run to decide whether to show the cross-link, and a thrown error here would surface as a
                    // toast on a perfectly valid trace view.
                    return null;
                }

                return toRunView(run);
            })
            .orElse(null);
    }

    /**
     * Lists runs for a single experiment after a workspace-boundary check on the experiment. Same uniform-error rule as
     * {@code experimentComparison}: a missing experiment id and a wrong-workspace experiment id produce the same
     * {@link AiScoreWorkspaceBoundaryException} payload to prevent existence probing.
     */
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

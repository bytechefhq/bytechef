/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.public_.web.rest;

import com.bytechef.ee.automation.ai.eval.dataset.service.WorkspaceAiEvalDatasetService;
import com.bytechef.ee.automation.ai.eval.experiment.dto.CreateExperimentRequest;
import com.bytechef.ee.automation.ai.eval.experiment.dto.ExperimentRunView;
import com.bytechef.ee.automation.ai.eval.experiment.dto.ExperimentSummaryView;
import com.bytechef.ee.automation.ai.eval.experiment.dto.ExperimentView;
import com.bytechef.ee.automation.ai.eval.experiment.executor.AiEvalExperimentExecutor;
import com.bytechef.ee.automation.ai.eval.experiment.service.WorkspaceAiEvalExperimentService;
import com.bytechef.ee.automation.ai.gateway.public_.workspace.AiGatewayWorkspaceHeaderResolver;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetVersionService;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRunStatus;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetNotFoundException;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetType;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * REST surface for experiment creation, status polling, and run listing. All endpoints require the
 * {@code X-ByteChef-Workspace-Id} header — missing or non-numeric values produce an HTTP 400. Cross-workspace access
 * raises {@link AiScoreWorkspaceBoundaryException} which the gateway's exception handler maps to HTTP 403 (same pattern
 * as {@code AiEvalDatasetController} and the external scores API).
 *
 * <p>
 * {@link #create} returns HTTP 202 because the replay runs asynchronously via {@link AiEvalExperimentExecutor#execute}.
 * Clients poll {@link #getSummary} or {@link #listRuns} for progress.
 *
 * @author Ivica Cardic
 * @version ee
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/ai-gateway/v1/experiments")
@SuppressFBWarnings("EI")
class AiEvalExperimentController {

    private static final Logger log = LoggerFactory.getLogger(AiEvalExperimentController.class);

    private final AiEvalExperimentService aiEvalExperimentService;
    private final AiEvalExperimentRunService aiEvalExperimentRunService;
    private final WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService;
    private final AiEvalExperimentExecutor aiEvalExperimentExecutor;
    private final AiEvalDatasetService aiEvalDatasetService;
    private final WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService;
    private final AiEvalDatasetVersionService aiEvalDatasetVersionService;
    private final AiGatewayRateLimitChecker aiGatewayRateLimitChecker;
    private final AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver;
    private final ObjectMapper objectMapper;

    AiEvalExperimentController(
        AiEvalExperimentService aiEvalExperimentService,
        WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService,
        AiEvalExperimentRunService aiEvalExperimentRunService,
        AiEvalExperimentExecutor aiEvalExperimentExecutor,
        AiEvalDatasetService aiEvalDatasetService,
        WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService,
        AiEvalDatasetVersionService aiEvalDatasetVersionService,
        @Nullable AiGatewayRateLimitChecker aiGatewayRateLimitChecker,
        AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver,
        ObjectMapper objectMapper) {

        this.aiEvalExperimentService = aiEvalExperimentService;
        this.workspaceAiEvalExperimentService = workspaceAiEvalExperimentService;
        this.aiEvalExperimentRunService = aiEvalExperimentRunService;
        this.aiEvalExperimentExecutor = aiEvalExperimentExecutor;
        this.aiEvalDatasetService = aiEvalDatasetService;
        this.workspaceAiEvalDatasetService = workspaceAiEvalDatasetService;
        this.aiEvalDatasetVersionService = aiEvalDatasetVersionService;
        this.aiGatewayRateLimitChecker = aiGatewayRateLimitChecker;
        this.workspaceHeaderResolver = workspaceHeaderResolver;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Object> create(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @RequestBody CreateExperimentRequest request) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        ResponseEntity<Object> rateLimitResponse = checkRateLimit(workspaceId);

        if (rateLimitResponse != null) {
            return rateLimitResponse;
        }

        // The dataset version is the only resource the request body can use to cross workspaces — without verifying
        // its parent dataset's workspace, a caller in workspace A could pin an experiment to workspace B's
        // dataset version and replay (or read) those items.
        AiEvalDatasetVersion datasetVersion = requireDatasetVersion(
            aiEvalDatasetVersionService, workspaceId, request.datasetVersionId());
        AiEvalDataset dataset = requireDataset(aiEvalDatasetService, workspaceId, datasetVersion.getDatasetId());

        assertSameWorkspaceForDataset(workspaceId, dataset, request.datasetVersionId());

        AiEvalExperiment experiment = new AiEvalExperiment(request.datasetVersionId());

        experiment.setPromptVersionId(request.promptVersionId());
        experiment.setModel(request.model());
        experiment.setMetadata(serializeMetadata(request.metadata()));

        AiEvalExperiment created = workspaceAiEvalExperimentService.createInWorkspace(experiment, workspaceId);

        // Fire-and-forget: @Async replay runs in the background. Clients poll /experiments/{id}.
        // We catch only the dispatch-class failures: TaskRejectedException (executor pool saturated before hand-off)
        // and BeanCreationException (proxy creation failure during partial-config load). Any other RuntimeException
        // leaking out of an @Async-proxied method indicates a real bug in the executor weaving and must NOT be
        // silently rebucketed as "dispatch failure" — that would force-FAIL an experiment whose async run is still
        // alive and risk a duplicate FAIL transition when the legitimate run completes.
        try {
            aiEvalExperimentExecutor.execute(created.getId());
        } catch (TaskRejectedException | BeanCreationException dispatchFailure) {
            log.error(
                "Failed to dispatch experiment {} for async execution; marking FAILED so it does not strand in PENDING",
                created.getId(), dispatchFailure);

            try {
                aiEvalExperimentService.markFinished(created.getId(), true);
            } catch (RuntimeException finalizationFailure) {
                log.error(
                    "Could not mark experiment {} as FAILED after dispatch failure — row stranded in PENDING",
                    created.getId(), finalizationFailure);
            }

            throw dispatchFailure;
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ExperimentView.from(created, workspaceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getSummary(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @PathVariable("id") Long id) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        ResponseEntity<Object> rateLimitResponse = checkRateLimit(workspaceId);

        if (rateLimitResponse != null) {
            return rateLimitResponse;
        }

        AiEvalExperiment experiment = requireExperiment(aiEvalExperimentService, workspaceId, id);

        assertSameWorkspace(workspaceId, experiment);

        long totalRuns = aiEvalExperimentRunService.countByExperiment(id);
        long completedRuns =
            aiEvalExperimentRunService.countByExperimentAndStatus(id, AiEvalExperimentRunStatus.COMPLETED);
        long failedRuns = aiEvalExperimentRunService.countByExperimentAndStatus(id, AiEvalExperimentRunStatus.FAILED);

        return ResponseEntity.ok(new ExperimentSummaryView(
            experiment.getId(),
            workspaceId,
            experiment.getDatasetVersionId(),
            experiment.getPromptVersionId(),
            experiment.getModel(),
            experiment.getStatus(),
            totalRuns,
            completedRuns,
            failedRuns,
            experiment.getCreatedDate(),
            experiment.getStartedDate(),
            experiment.getCompletedDate()));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Object> stop(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @PathVariable("id") Long id) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        ResponseEntity<Object> rateLimitResponse = checkRateLimit(workspaceId);

        if (rateLimitResponse != null) {
            return rateLimitResponse;
        }

        AiEvalExperiment experiment = requireExperiment(aiEvalExperimentService, workspaceId, id);

        assertSameWorkspace(workspaceId, experiment);

        AiEvalExperiment stopped = aiEvalExperimentService.requestStop(id);

        return ResponseEntity.accepted()
            .body(Map.of("id", stopped.getId(), "stopRequested", true));
    }

    @GetMapping("/{id}/runs")
    public ResponseEntity<Object> listRuns(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @PathVariable("id") Long id) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        ResponseEntity<Object> rateLimitResponse = checkRateLimit(workspaceId);

        if (rateLimitResponse != null) {
            return rateLimitResponse;
        }

        AiEvalExperiment experiment = requireExperiment(aiEvalExperimentService, workspaceId, id);

        assertSameWorkspace(workspaceId, experiment);

        return ResponseEntity.ok(
            aiEvalExperimentRunService.findAllByExperiment(id)
                .stream()
                .map(ExperimentRunView::from)
                .toList());
    }

    private static ResponseEntity<Object> missingWorkspaceResponse() {
        return ResponseEntity.badRequest()
            .body(Map.of(
                "error", Map.of(
                    "message", "Request must include header 'X-ByteChef-Workspace-Id' with a numeric workspace id.",
                    "type", "missing_workspace_id")));
    }

    /**
     * Enforces the per-workspace control-plane rate limit if the checker bean is present (it is wired only when
     * {@code bytechef.ai.gateway.rate-limiting.enabled=true}). Returns {@code null} when the request is allowed or when
     * rate-limiting is disabled; returns a 429 {@link ResponseEntity} with a {@code Retry-After} header when the
     * workspace is over its budget.
     */
    @Nullable
    private ResponseEntity<Object> checkRateLimit(Long workspaceId) {
        if (aiGatewayRateLimitChecker == null) {
            return null;
        }

        AiGatewayRateLimitResult result = aiGatewayRateLimitChecker.checkWorkspaceRequest(workspaceId, "experiments");

        if (!result.allowed()) {
            long retryAfterSeconds = Math.max(0L, (result.resetAtEpochMs() - System.currentTimeMillis()) / 1000L);

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(retryAfterSeconds))
                .body(Map.of("error", Map.of(
                    "type", "rate_limit_exceeded",
                    "message", "Rate limit exceeded; retry after " + retryAfterSeconds + " seconds.")));
        }

        return null;
    }

    /**
     * Loads an experiment by id, translating the service-layer "not found" {@link IllegalArgumentException} into an
     * {@link AiScoreTargetNotFoundException} so the gateway exception handler maps it to HTTP 404 with
     * {@code error.type = target_not_found} (instead of the default 400 mapping for {@link IllegalArgumentException}).
     * The IAE is preserved as cause so a validation bug masquerading as a 404 surfaces in server-side logs instead of
     * leaving operators chasing a ghost "missing experiment" ticket.
     *
     * <p>
     * The {@code workspaceId} of the caller is propagated to the structured fields on the exception so missing-target
     * spikes are scopable by tenant in the security log, matching the contract on
     * {@link AiScoreTargetNotFoundException#forTarget(Long, AiScoreTargetType, Long, Throwable)}.
     */
    private static AiEvalExperiment requireExperiment(AiEvalExperimentService service, Long workspaceId, Long id) {
        try {
            return service.getExperiment(id);
        } catch (IllegalArgumentException notFound) {
            throw AiScoreTargetNotFoundException.forTarget(workspaceId, AiScoreTargetType.EXPERIMENT, id, notFound);
        }
    }

    /**
     * Loads a dataset by id, translating the service-layer "not found" {@link IllegalArgumentException} into an
     * {@link AiScoreTargetNotFoundException}. See {@link #requireExperiment} for the rationale.
     */
    private static AiEvalDataset requireDataset(AiEvalDatasetService service, Long workspaceId, Long id) {
        try {
            return service.getDataset(id);
        } catch (IllegalArgumentException notFound) {
            throw AiScoreTargetNotFoundException.forTarget(workspaceId, AiScoreTargetType.DATASET, id, notFound);
        }
    }

    /**
     * Loads a dataset version by id, translating the service-layer "not found" {@link IllegalArgumentException} into an
     * {@link AiScoreTargetNotFoundException}. See {@link #requireExperiment} for the rationale.
     */
    private static AiEvalDatasetVersion requireDatasetVersion(
        AiEvalDatasetVersionService service, Long workspaceId, Long id) {

        try {
            return service.getVersion(id);
        } catch (IllegalArgumentException notFound) {
            throw AiScoreTargetNotFoundException.forTarget(
                workspaceId, AiScoreTargetType.DATASET_VERSION, id, notFound);
        }
    }

    /**
     * Cross-workspace guard. Uses the uniform "Caller is not authorized for X" message — distinguishable messages would
     * let workspace-A enumerate workspace-B experiment ids by reading the 403 body. Structured fields on the exception
     * (workspaceId / targetType / targetId) carry the diagnostic context server-side for log appenders.
     */
    private void assertSameWorkspace(Long callerWorkspaceId, AiEvalExperiment experiment) {
        Long experimentWorkspaceId = workspaceAiEvalExperimentService.getWorkspaceId(experiment.getId());

        if (!callerWorkspaceId.equals(experimentWorkspaceId)) {
            throw AiScoreWorkspaceBoundaryException.forTarget(
                callerWorkspaceId, AiScoreTargetType.EXPERIMENT, experiment.getId());
        }
    }

    /**
     * Cross-workspace guard for dataset version usage. See {@link #assertSameWorkspace} for the uniform-message
     * rationale.
     */
    private void assertSameWorkspaceForDataset(
        Long callerWorkspaceId, AiEvalDataset dataset, Long datasetVersionId) {

        Long datasetWorkspaceId = workspaceAiEvalDatasetService.getWorkspaceId(dataset.getId());

        if (!callerWorkspaceId.equals(datasetWorkspaceId)) {
            throw AiScoreWorkspaceBoundaryException.forTarget(
                callerWorkspaceId, AiScoreTargetType.DATASET_VERSION, datasetVersionId);
        }
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Metadata is not serializable as JSON", exception);
        }
    }
}

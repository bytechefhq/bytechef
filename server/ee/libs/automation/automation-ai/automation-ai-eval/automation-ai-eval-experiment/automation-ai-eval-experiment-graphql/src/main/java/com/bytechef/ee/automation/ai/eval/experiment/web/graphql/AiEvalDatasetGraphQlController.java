/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.eval.dataset.service.WorkspaceAiEvalDatasetService;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetItemView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetVersionView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetView;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetItem;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetItemService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetVersionService;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetType;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Read-only GraphQL surface for the operator console's Datasets tab. Lists workspace-scoped datasets, drills into their
 * versions, and lists items per version. All write operations remain on the public REST surface
 * ({@code AiEvalDatasetController}) — the operator console is a read-only viewer; dataset curation happens via SDK /
 * CI.
 *
 * <p>
 * Co-located in the experiment-graphql module rather than a sibling dataset-graphql module because the operator console
 * drills from a dataset into the experiments that ran against it, and pinning dataset GraphQL alongside experiment
 * GraphQL keeps that cross-resource lookup query path on the same module classpath.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiEvalDatasetGraphQlController {

    /**
     * Mirrors the {@code MINIMUM_VIEWER_ROLE} constant on {@code AiEvalExperimentGraphQlController}. Duplicated as a
     * string literal because pulling in the EE typed enum here would cycle the GraphQL module on EE-only configuration.
     * Renaming {@code WorkspaceRole.VIEWER} will not break this at compile time — pin it via the same reflective
     * contract test pattern used by {@code AiEvalExperimentGraphQlControllerViewerRolePinTest}.
     */
    private static final String MINIMUM_VIEWER_ROLE = "VIEWER";

    private final AiEvalDatasetService aiEvalDatasetService;
    private final WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService;
    private final AiEvalDatasetVersionService aiEvalDatasetVersionService;
    private final AiEvalDatasetItemService aiEvalDatasetItemService;
    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    AiEvalDatasetGraphQlController(
        AiEvalDatasetService aiEvalDatasetService,
        WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService,
        AiEvalDatasetVersionService aiEvalDatasetVersionService,
        AiEvalDatasetItemService aiEvalDatasetItemService, PermissionService permissionService) {

        this.aiEvalDatasetService = aiEvalDatasetService;
        this.workspaceAiEvalDatasetService = workspaceAiEvalDatasetService;
        this.aiEvalDatasetVersionService = aiEvalDatasetVersionService;
        this.aiEvalDatasetItemService = aiEvalDatasetItemService;
        this.permissionService = permissionService;
    }

    @QueryMapping
    public List<AiEvalDatasetView> aiEvalDatasets(@Argument Long workspaceId) {
        if (workspaceId == null) {
            return List.of();
        }

        if (!permissionService.hasWorkspaceRole(workspaceId, MINIMUM_VIEWER_ROLE)) {
            throw AiScoreWorkspaceBoundaryException.forTarget(
                workspaceId, AiScoreTargetType.WORKSPACE, workspaceId);
        }

        Long ws = workspaceId;

        return workspaceAiEvalDatasetService.findAllByWorkspace(workspaceId)
            .stream()
            .map(dataset -> toView(dataset, ws))
            .toList();
    }

    @QueryMapping
    public List<AiEvalDatasetVersionView> aiEvalDatasetVersions(@Argument Long datasetId) {
        if (datasetId == null) {
            return List.of();
        }

        AiEvalDataset dataset = requireWorkspaceVisibleDataset(datasetId);

        List<AiEvalDatasetVersion> versions = aiEvalDatasetVersionService.findAllByDataset(dataset.getId());

        return versions.stream()
            .map((AiEvalDatasetVersion version) -> new AiEvalDatasetVersionView(
                version.getId(),
                version.getDatasetId(),
                version.getLabel(),
                version.isFrozen(),
                aiEvalDatasetItemService.countByVersion(version.getId()),
                version.getCreatedDate()))
            .toList();
    }

    @QueryMapping
    public List<AiEvalDatasetItemView> aiEvalDatasetItems(@Argument Long versionId) {
        if (versionId == null) {
            return List.of();
        }

        AiEvalDatasetVersion version = requireWorkspaceVisibleVersion(versionId);

        return aiEvalDatasetItemService.getItemsByVersion(version.getId())
            .stream()
            .map(AiEvalDatasetGraphQlController::toItemView)
            .toList();
    }

    /**
     * Loads a dataset and verifies the caller has at least viewer-level access to its workspace. Missing-id and
     * wrong-workspace cases produce the same {@link AiScoreWorkspaceBoundaryException} payload to prevent existence
     * probing across workspaces (mirrors the pattern on
     * {@code AiEvalExperimentGraphQlController.experimentComparison}).
     */
    private AiEvalDataset requireWorkspaceVisibleDataset(Long datasetId) {
        AiEvalDataset dataset;

        try {
            dataset = aiEvalDatasetService.getDataset(datasetId);
        } catch (IllegalArgumentException notFound) {
            throw AiScoreWorkspaceBoundaryException.forTarget(
                null, AiScoreTargetType.DATASET, datasetId, notFound);
        }

        Long workspaceId = workspaceAiEvalDatasetService.getWorkspaceId(dataset.getId());

        if (workspaceId == null || !permissionService.hasWorkspaceRole(workspaceId, MINIMUM_VIEWER_ROLE)) {
            throw AiScoreWorkspaceBoundaryException.forTarget(
                workspaceId, AiScoreTargetType.DATASET, datasetId);
        }

        return dataset;
    }

    /**
     * Loads a dataset version, then walks up to its parent dataset for the workspace check — versions do not carry a
     * workspace id directly, so the boundary enforcement piggybacks on the dataset's. Uniform-error rule applies (same
     * payload for missing version and wrong-workspace version).
     */
    private AiEvalDatasetVersion requireWorkspaceVisibleVersion(Long versionId) {
        AiEvalDatasetVersion version;

        try {
            version = aiEvalDatasetVersionService.getVersion(versionId);
        } catch (IllegalArgumentException notFound) {
            throw AiScoreWorkspaceBoundaryException.forTarget(
                null, AiScoreTargetType.DATASET_VERSION, versionId, notFound);
        }

        AiEvalDataset dataset;

        try {
            dataset = aiEvalDatasetService.getDataset(version.getDatasetId());
        } catch (IllegalArgumentException notFound) {
            throw AiScoreWorkspaceBoundaryException.forTarget(
                null, AiScoreTargetType.DATASET_VERSION, versionId, notFound);
        }

        Long workspaceId = workspaceAiEvalDatasetService.getWorkspaceId(dataset.getId());

        if (workspaceId == null || !permissionService.hasWorkspaceRole(workspaceId, MINIMUM_VIEWER_ROLE)) {
            throw AiScoreWorkspaceBoundaryException.forTarget(
                workspaceId, AiScoreTargetType.DATASET_VERSION, versionId);
        }

        return version;
    }

    private static AiEvalDatasetView toView(AiEvalDataset dataset, Long workspaceId) {
        return new AiEvalDatasetView(
            dataset.getId(),
            workspaceId,
            dataset.getName(),
            dataset.getDescription(),
            dataset.getTags(),
            dataset.getCreatedDate(),
            dataset.getArchivedDate());
    }

    private static AiEvalDatasetItemView toItemView(AiEvalDatasetItem item) {
        return new AiEvalDatasetItemView(
            item.getId(),
            item.getDatasetVersionId(),
            item.getInput(),
            item.getExpectedOutput(),
            item.getMetadata(),
            item.getSourceTraceId(),
            item.getCreatedDate());
    }
}

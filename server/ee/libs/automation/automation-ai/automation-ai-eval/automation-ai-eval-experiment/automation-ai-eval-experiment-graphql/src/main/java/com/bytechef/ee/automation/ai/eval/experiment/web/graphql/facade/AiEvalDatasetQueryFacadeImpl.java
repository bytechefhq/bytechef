/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade;

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
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiEvalDatasetQueryFacadeImpl implements AiEvalDatasetQueryFacade {

    private static final String MINIMUM_VIEWER_ROLE = "VIEWER";

    private final AiEvalDatasetService aiEvalDatasetService;
    private final WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService;
    private final AiEvalDatasetVersionService aiEvalDatasetVersionService;
    private final AiEvalDatasetItemService aiEvalDatasetItemService;
    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    AiEvalDatasetQueryFacadeImpl(
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

    @Override
    public List<AiEvalDatasetView> getAiEvalDatasets(Long workspaceId) {
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

    @Override
    public List<AiEvalDatasetVersionView> getAiEvalDatasetVersions(Long datasetId) {
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

    @Override
    public List<AiEvalDatasetItemView> getAiEvalDatasetItems(Long versionId) {
        if (versionId == null) {
            return List.of();
        }

        AiEvalDatasetVersion version = requireWorkspaceVisibleVersion(versionId);

        return aiEvalDatasetItemService.getItemsByVersion(version.getId())
            .stream()
            .map(AiEvalDatasetQueryFacadeImpl::toItemView)
            .toList();
    }

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

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.connection.ConnectionEnvironmentMapper;
import com.bytechef.ee.automation.promotion.connection.PromotionConnectionScope;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SyncResult;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionProjectPreview;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("projectDeploymentPromotionHandler")
@ConditionalOnEEVersion
public class ProjectDeploymentPromotionHandler implements EnvironmentPromotionHandler {

    private final ConnectionEnvironmentMapper connectionEnvironmentMapper;
    private final ConnectionService connectionService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentPromoter projectDeploymentPromoter;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentPromotionHandler(
        ConnectionEnvironmentMapper connectionEnvironmentMapper, ConnectionService connectionService,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentPromoter projectDeploymentPromoter,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService) {

        this.connectionEnvironmentMapper = connectionEnvironmentMapper;
        this.connectionService = connectionService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentPromoter = projectDeploymentPromoter;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    @Override
    public PromotionResourceType getResourceType() {
        return PromotionResourceType.PROJECT_DEPLOYMENT;
    }

    @Override
    @PreAuthorize("hasPermission(@promotionAuthorizer.projectIdOfProjectDeployment(#sourceId), 'Project', 'DEPLOYMENT_PUSH')")
    @Transactional(readOnly = true)
    public EnvironmentPromotionPreview preview(long sourceId, Environment targetEnvironment) {
        ProjectDeployment source = loadSource(sourceId, targetEnvironment);
        long projectId = getProjectId(source);
        Project project = projectService.getProject(projectId);

        Optional<ProjectDeployment> existingTarget = fetchTarget(source, targetEnvironment);
        List<SourceBinding> sourceBindings = projectDeploymentPromoter.collectSourceBindings(source);

        Map<Long, Long> existingTargetBindings = Map.of();
        Integer targetProjectVersion = null;
        List<String> warnings = new ArrayList<>();

        if (existingTarget.isPresent()) {
            ProjectDeployment target = existingTarget.get();

            existingTargetBindings = projectDeploymentPromoter.existingTargetBindings(source, target);
            targetProjectVersion = target.getProjectVersion();
        } else {
            warnings.add("The promoted deployment is created disabled; enable it after reviewing its connections.");
        }

        Map<Long, Long> suggestedMappings = connectionEnvironmentMapper.suggest(
            project.getWorkspaceId(), PromotionConnectionScope.sourceConnectionIds(sourceBindings), targetEnvironment);

        return new EnvironmentPromotionPreview(
            PromotionResourceType.PROJECT_DEPLOYMENT, sourceId, source.getEnvironment(), targetEnvironment,
            existingTarget.map(ProjectDeployment::getId)
                .orElse(null),
            existingTarget.map(ProjectDeployment::getName)
                .orElse(null),
            List.of(new PromotionProjectPreview(projectId, project.getName(), source.getProjectVersion(),
                targetProjectVersion)),
            PromotionPreviews.connectionMappings(sourceBindings, existingTargetBindings, suggestedMappings,
                connectionService),
            warnings);
    }

    @Override
    @PreAuthorize("hasPermission(@promotionAuthorizer.projectIdOfProjectDeployment(#sourceId), 'Project', 'DEPLOYMENT_PUSH')")
    @Transactional
    public EnvironmentPromotionResult promote(
        long sourceId, Environment targetEnvironment, Map<Long, Long> connectionMappings) {

        ProjectDeployment source = loadSource(sourceId, targetEnvironment);
        long projectId = getProjectId(source);
        Project project = projectService.getProject(projectId);

        long workspaceId = project.getWorkspaceId();

        projectDeploymentPromoter.validatePromotable(projectId, source.getProjectVersion());

        List<SourceBinding> sourceBindings = projectDeploymentPromoter.collectSourceBindings(source);
        Set<Long> sourceConnectionIds = PromotionConnectionScope.sourceConnectionIds(sourceBindings);

        PromotionConnectionScope.checkMappedConnectionsBelongToSource(sourceConnectionIds, connectionMappings);

        Map<Long, Long> requestedMappings = connectionEnvironmentMapper.validate(
            workspaceId, targetEnvironment, connectionMappings);
        Map<Long, Long> suggestedMappings = connectionEnvironmentMapper.suggest(
            workspaceId, sourceConnectionIds, targetEnvironment);

        Optional<ProjectDeployment> existingTarget = fetchTarget(source, targetEnvironment);
        boolean created = existingTarget.isEmpty();
        ProjectDeployment target =
            existingTarget.orElseGet(() -> createTarget(source, projectId, targetEnvironment));

        SyncResult syncResult =
            projectDeploymentPromoter.sync(source, target, requestedMappings, suggestedMappings, created);

        long targetId = Objects.requireNonNull(target.getId(), "id");

        return new EnvironmentPromotionResult(targetId, created, null, syncResult.unresolvedConnectionIds());
    }

    private ProjectDeployment createTarget(ProjectDeployment source, long projectId, Environment targetEnvironment) {
        checkNoNameConflict(source, projectId, targetEnvironment);

        ProjectDeployment target = new ProjectDeployment();

        target.setDescription(source.getDescription());
        target.setEnabled(false);
        target.setEnvironment(targetEnvironment);
        target.setName(source.getName());
        target.setProjectId(projectId);
        target.setProjectVersion(source.getProjectVersion());
        target.setUuid(source.getUuid());

        long targetId = projectDeploymentFacade.createProjectDeployment(target, List.of(), List.of());

        return projectDeploymentService.getProjectDeployment(targetId);
    }

    private void checkNoNameConflict(ProjectDeployment source, long projectId, Environment targetEnvironment) {
        List<ProjectDeployment> targetEnvironmentProjectDeployments =
            projectDeploymentService.getProjectDeployments(null, targetEnvironment, projectId, null, null);

        for (ProjectDeployment targetEnvironmentProjectDeployment : targetEnvironmentProjectDeployments) {
            if (Objects.equals(targetEnvironmentProjectDeployment.getName(), source.getName())) {
                throw new ConfigurationException(
                    "A project deployment named '%s' already exists in %s".formatted(
                        source.getName(), targetEnvironment),
                    EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT);
            }
        }
    }

    private Optional<ProjectDeployment> fetchTarget(ProjectDeployment source, Environment targetEnvironment) {
        return projectDeploymentService.fetchProjectDeployment(
            Objects.requireNonNull(source.getUuid(), "uuid"), targetEnvironment);
    }

    private ProjectDeployment loadSource(long sourceId, Environment targetEnvironment) {
        ProjectDeployment source = projectDeploymentService.getProjectDeployment(sourceId);

        checkNotSynthetic(source);

        if (source.getEnvironment() == targetEnvironment) {
            throw new ConfigurationException(
                "Project deployment id=%s is already in %s".formatted(sourceId, targetEnvironment),
                EnvironmentPromotionErrorType.SAME_ENVIRONMENT);
        }

        return source;
    }

    private void checkNotSynthetic(ProjectDeployment source) {
        String owningSurface = owningSurfaceName(Objects.requireNonNull(source.getId(), "id"));

        if (owningSurface != null) {
            throw new ConfigurationException(
                "Project deployment id=%s backs an %s; promote the %s itself instead".formatted(
                    source.getId(), owningSurface, owningSurface),
                EnvironmentPromotionErrorType.SYNTHETIC_DEPLOYMENT_NOT_PROMOTABLE);
        }
    }

    @Nullable
    private String owningSurfaceName(long projectDeploymentId) {
        if (isOwnedBy(projectDeploymentId, ProjectDeploymentOwner.API_COLLECTION)) {
            return "API collection";
        }

        if (isOwnedBy(projectDeploymentId, ProjectDeploymentOwner.MCP_SERVER)) {
            return "MCP server";
        }

        if (isOwnedBy(projectDeploymentId, ProjectDeploymentOwner.A2A_SERVER)) {
            return "A2A server";
        }

        return null;
    }

    private boolean isOwnedBy(long projectDeploymentId, ProjectDeploymentOwner projectDeploymentOwner) {
        Set<Long> ownedProjectDeploymentIds = projectDeploymentFacade.getOwnedProjectDeploymentIds(
            List.of(projectDeploymentId), projectDeploymentOwner);

        return ownedProjectDeploymentIds.contains(projectDeploymentId);
    }

    private static long getProjectId(ProjectDeployment projectDeployment) {
        return Objects.requireNonNull(projectDeployment.getProjectId(), "projectId");
    }
}

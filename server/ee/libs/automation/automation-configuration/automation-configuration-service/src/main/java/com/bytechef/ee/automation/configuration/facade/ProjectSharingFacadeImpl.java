/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.configuration.audit.ProjectAuditEvents;
import com.bytechef.ee.automation.configuration.audit.ProjectAuditMapper;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ProjectSharingFacadeImpl implements ProjectSharingFacade {

    private static final String PROJECT = ProjectVisibilityFilter.PROJECT;

    private final ProjectService projectService;
    private final ResourceGrantService resourceGrantService;
    private final ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public ProjectSharingFacadeImpl(
        ProjectService projectService, ResourceGrantService resourceGrantService,
        ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry, WorkspaceUserService workspaceUserService) {

        this.projectService = projectService;
        this.resourceGrantService = resourceGrantService;
        this.resourceVisibilityPolicyRegistry = resourceVisibilityPolicyRegistry;
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@permissionService.isResourceOwner('Project', #projectId) || " +
        "@permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')")
    public List<Long> getProjectGrants(long workspaceId, long projectId) {
        validateProjectBelongsToWorkspace(workspaceId, projectId);

        return resourceGrantService.getGrantedUserIds(PROJECT, projectId);
    }

    @Override
    @Audited(event = ProjectAuditEvents.PROJECT_ACCESS_GRANTED, mapper = ProjectAuditMapper.class)
    @PreAuthorize("@permissionService.isResourceOwner('Project', #projectId) || " +
        "@permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')")
    public void grantProjectAccess(long workspaceId, long projectId, long userId) {
        validateProjectBelongsToWorkspace(workspaceId, projectId);
        validateGranteeIsWorkspaceMember(workspaceId, userId);

        resourceGrantService.grant(PROJECT, projectId, userId);
    }

    @Override
    @Audited(event = ProjectAuditEvents.PROJECT_ACCESS_REVOKED, mapper = ProjectAuditMapper.class)
    @PreAuthorize("@permissionService.isResourceOwner('Project', #projectId) || " +
        "@permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')")
    public void revokeProjectAccess(long workspaceId, long projectId, long userId) {
        validateProjectBelongsToWorkspace(workspaceId, projectId);

        resourceGrantService.revoke(PROJECT, projectId, userId);
    }

    @Override
    @Audited(event = ProjectAuditEvents.PROJECT_VISIBILITY_CHANGED, mapper = ProjectAuditMapper.class)
    @PreAuthorize("@permissionService.isResourceOwner('Project', #projectId) || " +
        "@permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')")
    public void setProjectVisibility(long workspaceId, long projectId, ResourceVisibility visibility) {
        if (!resourceVisibilityPolicyRegistry.supports(PROJECT, visibility)) {
            throw new ConfigurationException(
                "Project does not support %s visibility".formatted(visibility),
                ProjectErrorType.UNSUPPORTED_VISIBILITY);
        }

        validateProjectBelongsToWorkspace(workspaceId, projectId);

        projectService.updateVisibility(projectId, visibility);
    }

    private void validateGranteeIsWorkspaceMember(long workspaceId, long userId) {
        if (!workspaceUserService.isWorkspaceMember(userId, workspaceId)) {
            throw new ConfigurationException(
                "User id=%s is not a member of workspace id=%s".formatted(userId, workspaceId),
                ProjectErrorType.INVALID_PROJECT);
        }
    }

    private void validateProjectBelongsToWorkspace(long workspaceId, long projectId) {
        Project project = projectService.fetchProject(projectId)
            .orElseThrow(() -> invalidProject(workspaceId, projectId));

        if (!Objects.equals(project.getWorkspaceId(), workspaceId)) {
            throw invalidProject(workspaceId, projectId);
        }
    }

    private static ConfigurationException invalidProject(long workspaceId, long projectId) {
        return new ConfigurationException(
            "Project id=%s does not belong to workspace id=%s".formatted(projectId, workspaceId),
            ProjectErrorType.INVALID_PROJECT);
    }
}

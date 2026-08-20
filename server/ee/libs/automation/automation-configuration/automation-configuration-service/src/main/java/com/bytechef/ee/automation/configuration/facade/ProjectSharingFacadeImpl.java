/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.audit.ProjectAuditEvent;
import com.bytechef.automation.configuration.audit.ProjectAuditPublisher;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
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

    private final ProjectAuditPublisher projectAuditPublisher;
    private final ProjectService projectService;
    private final ResourceGrantService resourceGrantService;
    private final ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public ProjectSharingFacadeImpl(
        ProjectAuditPublisher projectAuditPublisher, ProjectService projectService,
        ResourceGrantService resourceGrantService, ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry,
        WorkspaceUserService workspaceUserService) {

        this.projectAuditPublisher = projectAuditPublisher;
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
    @PreAuthorize("@permissionService.isResourceOwner('Project', #projectId) || " +
        "@permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')")
    public void grantProjectAccess(long workspaceId, long projectId, long userId) {
        validateProjectBelongsToWorkspace(workspaceId, projectId);
        validateGranteeIsWorkspaceMember(workspaceId, userId);

        resourceGrantService.grant(PROJECT, projectId, userId);

        projectAuditPublisher.publish(
            ProjectAuditEvent.PROJECT_ACCESS_GRANTED, projectId, Map.of("targetUserId", userId));
    }

    @Override
    @PreAuthorize("@permissionService.isResourceOwner('Project', #projectId) || " +
        "@permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')")
    public void revokeProjectAccess(long workspaceId, long projectId, long userId) {
        validateProjectBelongsToWorkspace(workspaceId, projectId);

        // No membership check on the way out: someone removed from the workspace must still be revocable, and
        // revoking a grant that is not there is already a no-op.
        resourceGrantService.revoke(PROJECT, projectId, userId);

        projectAuditPublisher.publish(
            ProjectAuditEvent.PROJECT_ACCESS_REVOKED, projectId, Map.of("targetUserId", userId));
    }

    @Override
    @PreAuthorize("@permissionService.isResourceOwner('Project', #projectId) || " +
        "@permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')")
    public void setProjectVisibility(long workspaceId, long projectId, ResourceVisibility visibility) {
        // Deliberately the outer of two identical checks: ProjectServiceImpl.updateVisibility runs the same registry
        // lookup and throws the same typed error, so this one adds no protection the write does not already have. It
        // is kept for where it sits rather than for what it rejects -- ahead of validateProjectBelongsToWorkspace, so
        // an unsupported rung is answered as an unsupported rung whatever workspace the project is in, and ahead of
        // the audit event. The check inside the service is the one that covers every OTHER writer of the column:
        // ProjectMapper maps ORGANIZATION to THROW_EXCEPTION, so an unsupported rung persisted by any path would make
        // every subsequent read of this project throw. This is the only path that can CHANGE the column after
        // creation; the create path writes it too, and runs the same check in ProjectFacadeImpl
        // .applyCreateVisibility.
        if (!resourceVisibilityPolicyRegistry.supports(PROJECT, visibility)) {
            throw new ConfigurationException(
                "Project does not support %s visibility".formatted(visibility),
                ProjectErrorType.UNSUPPORTED_VISIBILITY);
        }

        validateProjectBelongsToWorkspace(workspaceId, projectId);

        // Unlike connections there is no "not while deployed" rule: a project's deployments inherit its visibility, so
        // narrowing leaves nothing dangling.
        projectService.updateVisibility(projectId, visibility);

        projectAuditPublisher.publish(
            ProjectAuditEvent.PROJECT_VISIBILITY_CHANGED, projectId, Map.of("toVisibility", visibility.name()));
    }

    private void validateGranteeIsWorkspaceMember(long workspaceId, long userId) {
        if (workspaceUserService.fetchWorkspaceUser(userId, workspaceId)
            .isEmpty()) {

            // Deliberately the same error as an unknown project: a grantor must not be able to enumerate user ids by
            // distinguishing "no such user" from "not a member of this workspace".
            throw new ConfigurationException(
                "User id=%s is not a member of workspace id=%s".formatted(userId, workspaceId),
                ProjectErrorType.INVALID_PROJECT);
        }
    }

    /**
     * An unknown project and a project in someone else's workspace raise the identical exception, from one factory
     * rather than from two copies that could drift. {@code getProject} is deliberately not used here: it throws
     * {@code NoSuchElementException} for an unknown id, which would escape this facade as an unhandled 500 instead of
     * the typed error the caller can act on.
     */
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

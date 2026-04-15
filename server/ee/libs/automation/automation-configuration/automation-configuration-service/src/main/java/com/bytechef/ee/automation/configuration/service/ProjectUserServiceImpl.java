/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.ProjectUser;
import com.bytechef.ee.automation.configuration.exception.ProjectUserErrorType;
import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.ee.automation.configuration.security.constant.ProjectRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ProjectUserServiceImpl implements ProjectUserService {

    private final @Nullable CustomRoleScopeResolver customRoleScopeResolver;
    private final PermissionService permissionService;
    private final ProjectUserRepository projectUserRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings("EI")
    public ProjectUserServiceImpl(
        @Nullable CustomRoleScopeResolver customRoleScopeResolver, PermissionService permissionService,
        ProjectUserRepository projectUserRepository, ProjectRepository projectRepository,
        WorkspaceUserRepository workspaceUserRepository) {

        this.customRoleScopeResolver = customRoleScopeResolver;
        this.permissionService = permissionService;
        this.projectUserRepository = projectUserRepository;
        this.projectRepository = projectRepository;
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCustomRoleId(long customRoleId) {
        return projectUserRepository.countByCustomRoleId(customRoleId);
    }

    @Override
    @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_MANAGE_USERS')")
    public ProjectUser addProjectUser(long projectId, long userId, int projectRoleOrdinal) {
        validateRoleNotExceedsCallerLevel(projectId, projectRoleOrdinal);

        Optional<ProjectUser> existingProjectUser = projectUserRepository.findByProjectIdAndUserId(projectId, userId);

        if (existingProjectUser.isPresent()) {
            throw new ConfigurationException(
                "User " + userId + " is already a member of project " + projectId,
                ProjectUserErrorType.ALREADY_MEMBER);
        }

        long workspaceId = projectRepository.findById(projectId)
            .orElseThrow(() -> new ConfigurationException(
                "Project " + projectId + " not found", ProjectUserErrorType.PROJECT_NOT_FOUND))
            .getWorkspaceId();

        if (workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .isEmpty()) {
            throw new ConfigurationException(
                "User " + userId + " is not a member of the workspace containing project " + projectId,
                ProjectUserErrorType.NOT_WORKSPACE_MEMBER);
        }

        ProjectUser saved = projectUserRepository.save(
            ProjectUser.forBuiltInRole(projectId, userId, ProjectRole.values()[projectRoleOrdinal]));

        // Grant paths are especially sensitive to eviction failure: the cache may already hold an empty-set DENY for
        // (userId, projectId) from a pre-membership probe, and if the afterCommit eviction throws (see
        // ProjectScopeCacheService.safeEvictSingle \u2014 failures swallow, count, and log) the user will be denied
        // everything until the cache TTL expires. Operators watching
        // bytechef_permission_cache_eviction_failure{scope=single}
        // must treat a counter tick here as "a new member cannot access their project" and force-evict manually.
        permissionService.evictProjectScopeCache(userId, projectId);

        return saved;
    }

    @Override
    @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_MANAGE_USERS')")
    public void deleteProjectUser(long projectId, long userId) {
        projectUserRepository.findByProjectIdAndUserId(projectId, userId)
            .ifPresent(projectUser -> {
                if (isEffectiveAdmin(projectUser)) {
                    validateNotLastEffectiveAdmin(projectId);
                }
            });

        projectUserRepository.deleteByProjectIdAndUserId(projectId, userId);

        permissionService.evictProjectScopeCache(userId, projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectUser> fetchProjectUser(long projectId, long userId) {
        return projectUserRepository.findByProjectIdAndUserId(projectId, userId);
    }

    @Override
    @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_VIEW_USERS')")
    @Transactional(readOnly = true)
    public List<ProjectUser> getProjectUsers(long projectId) {
        return projectUserRepository.findAllByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectUser> getUserProjectMemberships(long userId) {
        return projectUserRepository.findAllByUserId(userId);
    }

    @Override
    public void validateCascadeRemovalDoesNotOrphanAdminProjects(long userId, List<Long> projectIds) {
        // Workspace ADMIN has already been authorized by the caller's @PreAuthorize. No extra permission check here,
        // because a cascade at workspace level may touch projects where the caller does not personally hold
        // PROJECT_MANAGE_USERS.
        if (projectIds.isEmpty()) {
            return;
        }

        // Single round-trip instead of 2·N queries (findByProjectIdAndUserId + countEffectiveAdmins per project).
        // Matters for workspace admins with many project memberships — a cascade delete used to issue 1000 queries
        // for a user with 500 projects.
        List<Long> orphanedProjectIds = projectUserRepository.findOrphanRiskProjectIds(
            userId, projectIds, ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name());

        if (!orphanedProjectIds.isEmpty()) {
            throw new ConfigurationException(
                "Cannot remove user " + userId + " from workspace \u2014 they are the last effective admin of "
                    + "project(s) " + orphanedProjectIds + ". Assign another admin to those projects first, "
                    + "or delete the projects, before removing this user from the workspace.",
                ProjectUserErrorType.LAST_ADMIN_PROTECTED);
        }
    }

    @Override
    @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_MANAGE_USERS')")
    public ProjectUser updateProjectUserRole(long projectId, long userId, int projectRoleOrdinal) {
        validateRoleNotExceedsCallerLevel(projectId, projectRoleOrdinal);

        ProjectUser projectUser = projectUserRepository.findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new ConfigurationException(
                "User " + userId + " is not a member of project " + projectId,
                ProjectUserErrorType.NOT_MEMBER));

        validateNotSelfDemotion(projectId, userId, projectUser, projectRoleOrdinal);

        if (isEffectiveAdmin(projectUser) && projectRoleOrdinal != ProjectRole.ADMIN.ordinal()) {
            validateNotLastEffectiveAdmin(projectId);
        }

        projectUser.assignBuiltInRole(ProjectRole.values()[projectRoleOrdinal]);

        ProjectUser saved = projectUserRepository.save(projectUser);

        permissionService.evictProjectScopeCache(userId, projectId);

        return saved;
    }

    /**
     * Refuses to let the current user lower their own role on the project. The last-admin guard only blocks removing
     * the only remaining admin, so without this check an admin holding {@code PROJECT_MANAGE_USERS} could step down as
     * long as at least one other admin row exists \u2014 even if that other admin is inactive, departed, or another
     * automation account. The result would be an instant loss of {@code PROJECT_MANAGE_USERS} with no way to recover
     * until a tenant admin intervenes. Require self-demotion to be performed by another admin so there is a human in
     * the loop confirming the survivor is real. Tenant admins bypass the guard (they can always restore themselves).
     */
    private void validateNotSelfDemotion(
        long projectId, long userId, ProjectUser projectUser, int targetRoleOrdinal) {

        if (permissionService.isTenantAdmin() || !permissionService.isCurrentUser(userId)) {
            return;
        }

        if (projectUser.getProjectRole() != null && targetRoleOrdinal == projectUser.getProjectRole()) {
            // No-op role change \u2014 allow (matches the shape of other idempotent update paths).
            return;
        }

        if (!isEffectiveAdmin(projectUser)) {
            // Non-admins self-updating can't lose PROJECT_MANAGE_USERS because they don't hold it today; skip.
            return;
        }

        if (targetRoleOrdinal != ProjectRole.ADMIN.ordinal()) {
            throw new ConfigurationException(
                "Cannot demote your own role on project " + projectId
                    + ". Ask another admin to change your role, or delete your own membership instead.",
                ProjectUserErrorType.SELF_DEMOTION_FORBIDDEN);
        }
    }

    /**
     * Refuses to grant a role higher than what the caller already has on the project. Without this guard, a member
     * holding {@code PROJECT_MANAGE_USERS} (e.g., via a custom role) could promote a confederate to ADMIN and then use
     * that confederate to demote the original admins. Tenant admins bypass the check (they can grant any role).
     */
    private void validateRoleNotExceedsCallerLevel(long projectId, int targetRoleOrdinal) {
        ProjectRole[] values = ProjectRole.values();

        // Bounds-check before anything else, including the tenant-admin bypass. A negative or out-of-range ordinal
        // almost certainly reflects a buggy caller or manipulated request payload — if we skipped this for tenant
        // admins, they would later hit ArrayIndexOutOfBoundsException at the ProjectRole.values()[ordinal] site,
        // bubbling up as an opaque HTTP 500 instead of a structured INVALID_ROLE ConfigurationException.
        if (targetRoleOrdinal < 0 || targetRoleOrdinal >= values.length) {
            throw new ConfigurationException(
                "Target role ordinal " + targetRoleOrdinal + " is out of range [0," + values.length + ")",
                ProjectUserErrorType.INVALID_ROLE);
        }

        if (permissionService.isTenantAdmin()) {
            return;
        }

        ProjectRole targetRole = values[targetRoleOrdinal];

        if (!permissionService.hasProjectRole(projectId, targetRole.name())) {
            throw new ConfigurationException(
                "Cannot grant role " + targetRole.name() + " on project " + projectId
                    + " \u2014 caller does not hold an equal-or-higher role",
                ProjectUserErrorType.ROLE_ELEVATION_FORBIDDEN);
        }
    }

    /**
     * Returns {@code true} if the given member is an effective admin of their project — either by holding the built-in
     * ADMIN role or via a custom role that grants {@link PermissionScope#PROJECT_MANAGE_USERS}. Keeps the last-admin
     * guard honest when deployments use custom roles to confer admin powers.
     */
    private boolean isEffectiveAdmin(ProjectUser projectUser) {
        if (projectUser.getProjectRole() != null && projectUser.getProjectRole() == ProjectRole.ADMIN.ordinal()) {
            return true;
        }

        if (projectUser.getCustomRoleId() == null) {
            return false;
        }

        if (customRoleScopeResolver == null) {
            // Deployment lacks a resolver but the member carries a custom_role_id. Fail-closed: treat as effective
            // admin so the last-admin guard blocks removal instead of silently orphaning the project. If this branch
            // is hit in practice, it indicates a misconfigured deployment (custom roles in use without the resolver
            // bean) that should be resolved at the platform layer, not by silently fanning out lockout risk.
            return true;
        }

        // If resolveScopes returns Optional.empty() the custom_role_id is orphaned (data corruption). Treat that as
        // "not an effective admin" so the last-admin guard does not count a ghost member \u2014 a phantom admin
        // would otherwise block legitimate removal of real admins.
        return customRoleScopeResolver.resolveScopes(projectUser.getCustomRoleId())
            .map(scopes -> scopes.contains(PermissionScope.PROJECT_MANAGE_USERS))
            .orElse(false);
    }

    private void validateNotLastEffectiveAdmin(long projectId) {
        long effectiveAdminCount = projectUserRepository.countEffectiveAdmins(
            projectId, ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name());

        if (effectiveAdminCount <= 1) {
            throw new ConfigurationException(
                "Cannot remove or demote the last admin of project " + projectId +
                    ". At least one user with project-admin privileges must remain.",
                ProjectUserErrorType.LAST_ADMIN_PROTECTED);
        }
    }
}

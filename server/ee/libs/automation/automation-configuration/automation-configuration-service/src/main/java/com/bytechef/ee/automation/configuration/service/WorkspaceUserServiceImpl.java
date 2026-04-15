/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
@SuppressFBWarnings({
    "EI2", "NM"
})
public class WorkspaceUserServiceImpl implements WorkspaceUserService {

    private final PermissionService permissionService;
    private final ProjectUserRepository projectUserRepository;
    private final ProjectUserService projectUserService;
    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceUserServiceImpl(
        PermissionService permissionService, ProjectUserRepository projectUserRepository,
        ProjectUserService projectUserService, WorkspaceUserRepository workspaceUserRepository) {

        this.permissionService = permissionService;
        this.projectUserRepository = projectUserRepository;
        this.projectUserService = projectUserService;
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'ADMIN')")
    public WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        Optional<WorkspaceUser> existing = workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId);

        if (existing.isPresent()) {
            throw new ConfigurationException(
                "User " + userId + " is already a member of workspace " + workspaceId,
                WorkspaceUserErrorType.ALREADY_MEMBER);
        }

        return workspaceUserRepository.save(WorkspaceUser.forRole(userId, workspaceId, workspaceRole));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkspaceUser> fetchWorkspaceUser(long userId, long workspaceId) {
        return workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceUser> getUserWorkspaceUsers(long userId) {
        return workspaceUserRepository.findAllByUserId(userId);
    }

    @Override
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")
    @Transactional(readOnly = true)
    public List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId) {
        return workspaceUserRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'ADMIN')")
    public boolean removeWorkspaceUser(long userId, long workspaceId) {
        WorkspaceUser workspaceUser = workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .orElseThrow(() -> new ConfigurationException(
                "User " + userId + " is not a member of workspace " + workspaceId,
                WorkspaceUserErrorType.NOT_MEMBER));

        if (Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal())) {
            validateNotLastAdmin(workspaceId);
        }

        // Snapshot the project ids whose cache entries must be evicted BEFORE the cascade delete removes the rows.
        // Querying after the delete returns an empty set, leaving stale (userId, projectId) ALLOW decisions in the
        // cache until the TTL expires.
        List<Long> evictableProjectIds = projectUserRepository.findProjectIdsByUserIdAndWorkspaceId(
            userId, workspaceId);

        // Guard per-project: if removing this user from the workspace would cascade-delete their last-effective-admin
        // project membership somewhere, refuse before touching any rows. Prior implementations silently orphaned the
        // project because the cascade path bypassed the per-project last-admin guard that the regular
        // deleteProjectUser flow enforces.
        projectUserService.validateCascadeRemovalDoesNotOrphanAdminProjects(userId, evictableProjectIds);

        // Cascade-delete project memberships before the workspace_user row. There is no DB-level FK from project_user
        // to workspace_user, so cascade delete does not fire automatically.
        projectUserRepository.deleteByUserIdAndWorkspaceId(userId, workspaceId);

        workspaceUserRepository.deleteByUserIdAndWorkspaceId(userId, workspaceId);

        for (Long projectId : evictableProjectIds) {
            permissionService.evictProjectScopeCache(userId, projectId);
        }

        return true;
    }

    @Override
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'ADMIN')")
    public WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        WorkspaceUser workspaceUser = workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .orElseThrow(() -> new ConfigurationException(
                "User " + userId + " is not a member of workspace " + workspaceId,
                WorkspaceUserErrorType.NOT_MEMBER));

        validateNotSelfDemotion(workspaceId, userId, workspaceUser, workspaceRole);

        if (Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal())
            && workspaceRole != WorkspaceRole.ADMIN) {

            validateNotLastAdmin(workspaceId);
        }

        workspaceUser.setWorkspaceRole(workspaceRole.ordinal());

        return workspaceUserRepository.save(workspaceUser);
    }

    /**
     * Refuses to let the current user demote their own workspace ADMIN role. The last-admin guard only blocks the
     * delete that would leave zero admins; without this check an admin could downgrade themselves to EDITOR while
     * another admin row exists, instantly losing {@code PROJECT_MANAGE_USERS} workspace-wide and the ability to reverse
     * the change. Require self-demotion to go through another admin so a human confirms the survivor is real. Tenant
     * admins bypass the guard (they can always restore themselves).
     */
    private void validateNotSelfDemotion(
        long workspaceId, long userId, WorkspaceUser workspaceUser, WorkspaceRole targetRole) {

        if (permissionService.isTenantAdmin() || !permissionService.isCurrentUser(userId)) {
            return;
        }

        if (Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal())
            && targetRole != WorkspaceRole.ADMIN) {

            throw new ConfigurationException(
                "Cannot demote your own role on workspace " + workspaceId
                    + ". Ask another admin to change your role, or leave the workspace instead.",
                WorkspaceUserErrorType.SELF_DEMOTION_FORBIDDEN);
        }
    }

    private void validateNotLastAdmin(long workspaceId) {
        long adminCount = workspaceUserRepository.countByWorkspaceIdAndWorkspaceRole(
            workspaceId, WorkspaceRole.ADMIN.ordinal());

        if (adminCount <= 1) {
            throw new ConfigurationException(
                "Cannot remove or demote the last admin of workspace " + workspaceId
                    + ". At least one admin must remain.",
                WorkspaceUserErrorType.LAST_ADMIN_PROTECTED);
        }
    }
}

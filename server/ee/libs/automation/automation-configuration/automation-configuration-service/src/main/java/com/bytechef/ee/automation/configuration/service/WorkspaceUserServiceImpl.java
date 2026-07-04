/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditEvent;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final WorkspaceUserAuditPublisher workspaceUserAuditPublisher;
    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceUserServiceImpl(
        PermissionService permissionService, WorkspaceUserAuditPublisher workspaceUserAuditPublisher,
        WorkspaceUserRepository workspaceUserRepository) {

        this.permissionService = permissionService;
        this.workspaceUserAuditPublisher = workspaceUserAuditPublisher;
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        Optional<WorkspaceUser> existing = workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId);

        if (existing.isPresent()) {
            throw new ConfigurationException(
                "User " + userId + " is already a member of workspace " + workspaceId,
                WorkspaceUserErrorType.ALREADY_MEMBER);
        }

        WorkspaceUser savedWorkspaceUser = workspaceUserRepository.save(
            WorkspaceUser.forRole(userId, workspaceId, workspaceRole));

        // The scope cache may already hold an empty-set DENY for (userId, workspaceId) from a pre-membership probe;
        // evict so the new member's scopes are re-resolved on the next check instead of being pinned to "no access"
        // until the TTL expires.
        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        Map<String, Object> data = new HashMap<>();

        data.put("workspaceId", String.valueOf(workspaceId));
        data.put("userId", String.valueOf(userId));
        data.put("role", workspaceRole.name());

        workspaceUserAuditPublisher.publish(WorkspaceUserAuditEvent.WORKSPACE_USER_ADDED, data);

        return savedWorkspaceUser;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCustomRoleId(long customRoleId) {
        return workspaceUserRepository.countByCustomRoleId(customRoleId);
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
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_VIEW')")
    @Transactional(readOnly = true)
    public List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId) {
        return workspaceUserRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE')")
    public boolean removeWorkspaceUser(long userId, long workspaceId) {
        WorkspaceUser workspaceUser = workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .orElseThrow(() -> new ConfigurationException(
                "User " + userId + " is not a member of workspace " + workspaceId,
                WorkspaceUserErrorType.NOT_MEMBER));

        if (Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal())) {
            validateNotLastAdmin(workspaceId);
        }

        workspaceUserRepository.deleteByUserIdAndWorkspaceId(userId, workspaceId);

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        Map<String, Object> data = new HashMap<>();

        data.put("workspaceId", String.valueOf(workspaceId));
        data.put("userId", String.valueOf(userId));

        workspaceUserAuditPublisher.publish(WorkspaceUserAuditEvent.WORKSPACE_USER_REMOVED, data);

        return true;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE')")
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

        WorkspaceUser savedWorkspaceUser = workspaceUserRepository.save(workspaceUser);

        // A role change alters the resolved scope set, so the cached (userId, workspaceId) scopes must be evicted or
        // the old role's scopes would be served until the TTL expires.
        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        Map<String, Object> data = new HashMap<>();

        data.put("workspaceId", String.valueOf(workspaceId));
        data.put("userId", String.valueOf(userId));
        data.put("role", workspaceRole.name());

        workspaceUserAuditPublisher.publish(WorkspaceUserAuditEvent.WORKSPACE_USER_ROLE_UPDATED, data);

        return savedWorkspaceUser;
    }

    /**
     * Refuses to let the current user demote their own workspace ADMIN role. The last-admin guard only blocks the
     * delete that would leave zero admins; without this check an admin could downgrade themselves to EDITOR while
     * another admin row exists, instantly losing workspace-management rights and the ability to reverse the change.
     * Require self-demotion to go through another admin so a human confirms the survivor is real. Tenant admins bypass
     * the guard (they can always restore themselves).
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

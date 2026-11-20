/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditEvents;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditMapper;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.Audited;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
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
public class WorkspaceUserFacadeImpl implements WorkspaceUserFacade {

    private final PermissionService permissionService;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public WorkspaceUserFacadeImpl(PermissionService permissionService, WorkspaceUserService workspaceUserService) {
        this.permissionService = permissionService;
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED, mapper = WorkspaceUserAuditMapper.class)
    public WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        WorkspaceUser savedWorkspaceUser = workspaceUserService.addWorkspaceUser(userId, workspaceId, workspaceRole);

        // The scope cache may already hold an empty-set DENY for (userId, workspaceId) from a pre-membership probe;
        // evict so the new member's scopes are re-resolved on the next check instead of being pinned to "no access"
        // until the TTL expires.
        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        return savedWorkspaceUser;
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED, mapper = WorkspaceUserAuditMapper.class)
    public boolean removeWorkspaceUser(long userId, long workspaceId) {
        boolean removed = workspaceUserService.removeWorkspaceUser(userId, workspaceId);

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        return removed;
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED, mapper = WorkspaceUserAuditMapper.class)
    public WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        Optional<WorkspaceUser> workspaceUserOptional = workspaceUserService.fetchWorkspaceUser(userId, workspaceId);

        if (workspaceUserOptional.isPresent()) {
            validateNotSelfDemotion(workspaceId, userId, workspaceUserOptional.get(), workspaceRole);
        }

        WorkspaceUser savedWorkspaceUser = workspaceUserService.updateWorkspaceUserRole(
            userId, workspaceId, workspaceRole);

        // A role change alters the resolved scope set, so the cached (userId, workspaceId) scopes must be evicted or
        // the old role's scopes would be served until the TTL expires.
        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

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
}

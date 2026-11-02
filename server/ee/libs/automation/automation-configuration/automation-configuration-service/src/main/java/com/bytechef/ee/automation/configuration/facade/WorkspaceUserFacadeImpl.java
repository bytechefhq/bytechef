/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditEvents;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditMapper;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workspace-wide membership changes need {@code WORKSPACE_MEMBER_MANAGE} in every environment, because they grant or
 * revoke a role everywhere at once; the per-environment ones need it only in the environment they change. Left
 * unguarded, any member could grant themselves ADMIN.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class WorkspaceUserFacadeImpl implements WorkspaceUserFacade {

    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public WorkspaceUserFacadeImpl(WorkspaceUserService workspaceUserService) {
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED, mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser addWorkspaceUser(
        long userId, long workspaceId, WorkspaceRole workspaceRole, Long customRoleId) {

        return workspaceUserService.addWorkspaceUser(userId, workspaceId, workspaceRole, customRoleId);
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED, mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser assignCustomRole(long userId, long workspaceId, long customRoleId) {
        return workspaceUserService.assignCustomRole(userId, workspaceId, customRoleId);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_VIEW')")
    @Transactional(readOnly = true)
    public List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId) {
        return workspaceUserService.getWorkspaceWorkspaceUsers(workspaceId);
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED, mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser inviteWorkspaceUser(
        long workspaceId, String email, WorkspaceRole workspaceRole, Long customRoleId) {

        return workspaceUserService.inviteWorkspaceUser(workspaceId, email, workspaceRole, customRoleId);
    }

    @Override
    @Audited(
        event = WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_REMOVED,
        mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE', #environment)")
    public void removeEnvironmentRole(long userId, long workspaceId, Environment environment) {
        workspaceUserService.removeEnvironmentRole(userId, workspaceId, environment);
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED, mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public boolean removeWorkspaceUser(long userId, long workspaceId) {
        return workspaceUserService.removeWorkspaceUser(userId, workspaceId);
    }

    @Override
    @Audited(
        event = WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED,
        mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE', #environment)")
    public WorkspaceUser setEnvironmentRole(
        long userId, long workspaceId, Environment environment, WorkspaceRole workspaceRole, Long customRoleId) {

        return workspaceUserService.setEnvironmentRole(userId, workspaceId, environment, workspaceRole, customRoleId);
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED, mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        return workspaceUserService.updateWorkspaceUserRole(userId, workspaceId, workspaceRole);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.exception.ConfigurationException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceUserFacadeTest {

    private static final long USER_ID = 2L;
    private static final long WORKSPACE_ID = 1L;

    private final PermissionService permissionService = mock(PermissionService.class);
    private final WorkspaceUserService workspaceUserService = mock(WorkspaceUserService.class);
    private final WorkspaceUserFacade workspaceUserFacade = new WorkspaceUserFacadeImpl(
        permissionService, workspaceUserService);

    @Test
    void testAddWorkspaceUserEvictsTheScopeCache() {
        WorkspaceUser workspaceUser = WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER);

        when(workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER))
            .thenReturn(workspaceUser);

        assertThat(workspaceUserFacade.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER))
            .isSameAs(workspaceUser);

        verify(permissionService).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRemoveWorkspaceUserEvictsTheScopeCache() {
        when(workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID)).thenReturn(true);

        assertThat(workspaceUserFacade.removeWorkspaceUser(USER_ID, WORKSPACE_ID)).isTrue();

        verify(permissionService).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRemoveWorkspaceUserDoesNotEvictWhenRemovalFails() {
        when(workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .thenThrow(new IllegalStateException("not a member"));

        assertThatThrownBy(() -> workspaceUserFacade.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(IllegalStateException.class);

        verify(permissionService, never()).evictWorkspaceScopeCache(anyLong(), anyLong());
    }

    @Test
    void testUpdateWorkspaceUserRoleEvictsTheScopeCache() {
        WorkspaceUser workspaceUser = WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        when(workspaceUserService.fetchWorkspaceUser(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER)));
        when(workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
            .thenReturn(workspaceUser);

        assertThat(workspaceUserFacade.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
            .isSameAs(workspaceUser);

        verify(permissionService).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testUpdateWorkspaceUserRoleRefusesSelfDemotionOfAnAdmin() {
        when(workspaceUserService.fetchWorkspaceUser(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN)));
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);

        assertThatThrownBy(
            () -> workspaceUserFacade.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
                .isInstanceOf(ConfigurationException.class);

        verify(workspaceUserService, never()).updateWorkspaceUserRole(
            anyLong(), anyLong(), any(WorkspaceRole.class));
        verify(permissionService, never()).evictWorkspaceScopeCache(anyLong(), anyLong());
    }

    @Test
    void testUpdateWorkspaceUserRoleAllowsATenantAdminToDemoteThemselves() {
        WorkspaceUser workspaceUser = WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        when(workspaceUserService.fetchWorkspaceUser(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN)));
        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);
        when(workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
            .thenReturn(workspaceUser);

        assertThat(workspaceUserFacade.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
            .isSameAs(workspaceUser);
    }
}

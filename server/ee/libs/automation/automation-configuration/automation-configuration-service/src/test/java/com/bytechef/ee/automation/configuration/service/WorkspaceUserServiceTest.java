/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.ProjectUserErrorType;
import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the last-admin guard, cross-project cascade delete, and cache eviction in {@link WorkspaceUserServiceImpl}.
 * {@code @PreAuthorize} enforcement is verified separately in {@link PreAuthorizeAnnotationTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceUserServiceTest {

    private static final long WORKSPACE_ID = 7L;
    private static final long USER_ID = 42L;
    private static final long PROJECT_ID = 100L;

    private PermissionService permissionService;
    private ProjectUserRepository projectUserRepository;
    private ProjectUserService projectUserService;
    private WorkspaceUserRepository workspaceUserRepository;
    private WorkspaceUserServiceImpl workspaceUserService;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        projectUserRepository = mock(ProjectUserRepository.class);
        projectUserService = mock(ProjectUserService.class);
        workspaceUserRepository = mock(WorkspaceUserRepository.class);

        workspaceUserService = new WorkspaceUserServiceImpl(
            permissionService, projectUserRepository, projectUserService, workspaceUserRepository);
    }

    @Test
    void testAddWorkspaceUserPersists() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceUser result = workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(result.getWorkspaceRole()).isEqualTo(WorkspaceRole.EDITOR.ordinal());
    }

    @Test
    void testAddWorkspaceUserRejectsDuplicate() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        assertThatThrownBy(() -> workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("already a member");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testRemoveWorkspaceUserThrowsWhenNotMember() {
        // Non-member removal was previously a silent no-op returning false; it now throws NOT_MEMBER so the caller
        // can distinguish "nothing to remove" from "removal succeeded", and the GraphQL layer can surface a typed
        // error instead of a confusing success toast on stale UI.
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("not a member");

        verify(workspaceUserRepository, never()).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
        verify(projectUserRepository, never()).deleteByUserIdAndWorkspaceId(anyLong(), anyLong());
    }

    @Test
    void testRemoveWorkspaceUserBlockedWhenLastAdmin() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));
        when(workspaceUserRepository.countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(1L);

        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("last admin");

        verify(workspaceUserRepository, never()).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
        verify(projectUserRepository, never()).deleteByUserIdAndWorkspaceId(anyLong(), anyLong());
    }

    @Test
    void testRemoveWorkspaceUserCascadesProjectMembershipsAndEvictsCache() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));
        // The snapshot is taken BEFORE the cascade delete so eviction can target the projects whose rows are about
        // to disappear. WorkspaceUserServiceImplIntTest covers the SQL semantics end-to-end against PostgreSQL —
        // this unit test only pins the eviction wiring against the snapshot.
        when(projectUserRepository.findProjectIdsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of(PROJECT_ID));

        boolean result = workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID);

        assertThat(result).isTrue();

        verify(projectUserRepository, times(1)).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
        verify(workspaceUserRepository, times(1)).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
        verify(permissionService, times(1)).evictProjectScopeCache(USER_ID, PROJECT_ID);
    }

    @Test
    void testRemoveWorkspaceUserBlockedWhenCascadeWouldOrphanProjectAdmin() {
        // Pre-cascade guard: if removing this workspace member would cascade-delete their last-effective-admin
        // project membership, refuse. Previously the cascade silently bypassed the per-project last-admin guard,
        // orphaning the project until operators re-assigned an admin manually.
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));
        when(projectUserRepository.findProjectIdsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of(PROJECT_ID));

        doThrow(new ConfigurationException(
            "Cannot remove user " + USER_ID + " from workspace \u2014 they are the last effective admin of "
                + "project(s) [" + PROJECT_ID + "].",
            ProjectUserErrorType.LAST_ADMIN_PROTECTED))
                .when(projectUserService)
                .validateCascadeRemovalDoesNotOrphanAdminProjects(USER_ID, List.of(PROJECT_ID));

        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("last effective admin");

        // Nothing is deleted or evicted when the guard fires \u2014 failure must be atomic.
        verify(projectUserRepository, never()).deleteByUserIdAndWorkspaceId(anyLong(), anyLong());
        verify(workspaceUserRepository, never()).deleteByUserIdAndWorkspaceId(anyLong(), anyLong());
        verify(permissionService, never()).evictProjectScopeCache(anyLong(), anyLong());
    }

    @Test
    void testRemoveWorkspaceUserAllowedWhenAnotherAdminExists() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));
        when(workspaceUserRepository.countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(2L);
        when(projectUserRepository.findProjectIdsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of());

        boolean result = workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID);

        assertThat(result).isTrue();
        verify(workspaceUserRepository, times(1)).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testUpdateWorkspaceUserRoleChangesRole() {
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal());

        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER);

        assertThat(workspaceUser.getWorkspaceRole()).isEqualTo(WorkspaceRole.VIEWER.ordinal());
    }

    @Test
    void testUpdateWorkspaceUserRoleBlockedWhenDemotingLastAdmin() {
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());

        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(1L);

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("last admin");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testUpdateWorkspaceUserRoleAllowsAdminToAdminUpdate() {
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());

        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN);

        verify(workspaceUserRepository, never())
            .countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());
    }

    @Test
    void testUpdateWorkspaceUserRoleRejectsUnknownMembership() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("not a member");
    }

    @Test
    void testUpdateWorkspaceUserRoleBlocksSelfDemotionEvenWhenAnotherAdminExists() {
        // Even with a second ADMIN row present, the caller demoting themselves would instantly lose
        // workspace-management privileges with no safe way to recover. Require another admin to perform the demotion.
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());

        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Cannot demote your own role");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
        verify(workspaceUserRepository, never())
            .countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());
    }

    @Test
    void testUpdateWorkspaceUserRoleAllowsAnotherAdminToDemoteYou() {
        // Caller != subject: the demotion proceeds (and last-admin guard fires on its own terms).
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());

        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(false);
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(2L);
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        assertThat(workspaceUser.getWorkspaceRole()).isEqualTo(WorkspaceRole.EDITOR.ordinal());
    }

    @Test
    void testUpdateWorkspaceUserRoleAllowsTenantAdminToSelfDemote() {
        // Tenant admins are exempt from the workspace-level self-demotion guard \u2014 they retain the ability to
        // restore themselves outside workspace RBAC.
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());

        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(2L);
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        verify(workspaceUserRepository, times(1)).save(any(WorkspaceUser.class));
    }
}

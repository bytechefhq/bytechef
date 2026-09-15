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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceErrorType;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Pins the implicit/explicit transitions. Exactly one mode is represented at any moment, and neither transition may
 * widen a member's access as a side effect.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceUserModeSwitchingTest {

    private static final long USER_ID = 1L;
    private static final long WORKSPACE_ID = 2L;

    @Mock
    private CustomRoleRepository customRoleRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private WorkspaceUserAuditPublisher workspaceUserAuditPublisher;

    @Mock
    private WorkspaceUserRepository workspaceUserRepository;

    @InjectMocks
    private WorkspaceUserServiceImpl workspaceUserService;

    @BeforeEach
    void setUp() {
        lenient().when(workspaceService.workspaceExists(WORKSPACE_ID))
            .thenReturn(true);
    }

    @Test
    void testGrantingAnEnvironmentRoleDeletesTheImplicitRow() {
        WorkspaceUser implicitWorkspaceUser = WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(implicitWorkspaceUser));
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT.ordinal())).thenReturn(Optional.empty());
        when(workspaceUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.setEnvironmentRole(
            USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT, WorkspaceRole.EDITOR, null);

        verify(workspaceUserRepository).delete(implicitWorkspaceUser);
    }

    @Test
    void testGrantingAnEnvironmentRoleEvictsTheScopeCache() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.STAGING.ordinal())).thenReturn(Optional.empty());
        when(workspaceUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.setEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.STAGING, WorkspaceRole.VIEWER, null);

        verify(permissionService).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testGrantsACustomRoleInOneEnvironment() {
        when(customRoleRepository.findById(7L))
            .thenReturn(Optional.of(new CustomRole("Deployer", Set.of("WORKFLOW_VIEW"))));
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal())).thenReturn(Optional.empty());
        when(workspaceUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceUser savedWorkspaceUser = workspaceUserService.setEnvironmentRole(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION, null, 7L);

        // custom_role_id rides in the same row as the environment, so a custom role is per-environment already.
        assertThat(savedWorkspaceUser.getCustomRoleId()).isEqualTo(7L);
        assertThat(savedWorkspaceUser.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testRejectsBothOrNeitherRoleArgument() {
        assertThatThrownBy(
            () -> workspaceUserService.setEnvironmentRole(
                USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER, 7L))
                    .isInstanceOf(ConfigurationException.class);
        assertThatThrownBy(
            () -> workspaceUserService.setEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION, null, null))
                .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testRemovingTheLastEnvironmentRowKeepsItsRoleInEveryEnvironment() {
        WorkspaceUser onlyWorkspaceUser =
            WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR, Environment.DEVELOPMENT);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT.ordinal())).thenReturn(Optional.of(onlyWorkspaceUser));
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of(onlyWorkspaceUser));
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, "WORKSPACE_MEMBER_MANAGE"))
            .thenReturn(true);

        workspaceUserService.removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT);

        ArgumentCaptor<WorkspaceUser> workspaceUserArgumentCaptor = ArgumentCaptor.forClass(WorkspaceUser.class);

        verify(workspaceUserRepository).delete(onlyWorkspaceUser);
        verify(workspaceUserRepository).save(workspaceUserArgumentCaptor.capture());

        WorkspaceUser workspaceWideWorkspaceUser = workspaceUserArgumentCaptor.getValue();

        assertThat(workspaceWideWorkspaceUser.getEnvironment()).isNull();
        assertThat(workspaceWideWorkspaceUser.getWorkspaceRole()).isEqualTo(WorkspaceRole.EDITOR.ordinal());
        assertThat(workspaceWideWorkspaceUser.getCustomRoleId()).isNull();
        verify(permissionService).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRemovingTheLastCustomRoleEnvironmentRowKeepsTheCustomRoleInEveryEnvironment() {
        WorkspaceUser onlyWorkspaceUser =
            WorkspaceUser.forCustomRole(USER_ID, WORKSPACE_ID, 7L, Environment.PRODUCTION);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal())).thenReturn(Optional.of(onlyWorkspaceUser));
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of(onlyWorkspaceUser));
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, "WORKSPACE_MEMBER_MANAGE"))
            .thenReturn(true);

        workspaceUserService.removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION);

        ArgumentCaptor<WorkspaceUser> workspaceUserArgumentCaptor = ArgumentCaptor.forClass(WorkspaceUser.class);

        verify(workspaceUserRepository).save(workspaceUserArgumentCaptor.capture());

        WorkspaceUser workspaceWideWorkspaceUser = workspaceUserArgumentCaptor.getValue();

        assertThat(workspaceWideWorkspaceUser.getEnvironment()).isNull();
        assertThat(workspaceWideWorkspaceUser.getCustomRoleId()).isEqualTo(7L);
        assertThat(workspaceWideWorkspaceUser.getWorkspaceRole()).isNull();
    }

    @Test
    void testRefusesToWidenTheLastEnvironmentRowWithoutMemberManagementInEveryEnvironment() {
        WorkspaceUser onlyWorkspaceUser =
            WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR, Environment.DEVELOPMENT);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT.ordinal())).thenReturn(Optional.of(onlyWorkspaceUser));
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of(onlyWorkspaceUser));
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, "WORKSPACE_MEMBER_MANAGE"))
            .thenReturn(false);

        assertThatThrownBy(
            () -> workspaceUserService.removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
                .isInstanceOf(AccessDeniedException.class);

        verify(workspaceUserRepository, never()).delete(any());
        verify(workspaceUserRepository, never()).save(any());
    }

    @Test
    void testRemovesAnEnvironmentRowWhenTheMemberHoldsAnother() {
        WorkspaceUser developmentWorkspaceUser =
            WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR, Environment.DEVELOPMENT);
        WorkspaceUser productionWorkspaceUser =
            WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER, Environment.PRODUCTION);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT.ordinal()))
                .thenReturn(Optional.of(developmentWorkspaceUser));
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of(developmentWorkspaceUser, productionWorkspaceUser));

        workspaceUserService.removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT);

        verify(workspaceUserRepository).delete(developmentWorkspaceUser);
        verify(workspaceUserRepository, never()).save(any());
    }

    @Test
    void testRefusesToRemoveAnEnvironmentRoleAnImplicitModeMemberDoesNotHold() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal())).thenReturn(Optional.empty());
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(true);

        assertThatThrownBy(
            () -> workspaceUserService.removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                .isEqualTo(WorkspaceUserErrorType.NO_ENVIRONMENT_ROLE.getErrorKey());

        verify(workspaceUserRepository, never()).delete(any());
        verify(permissionService, never()).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRefusesToRemoveAnEnvironmentRoleFromANonMember() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal())).thenReturn(Optional.empty());
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(false);

        assertThatThrownBy(
            () -> workspaceUserService.removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                .isEqualTo(WorkspaceUserErrorType.NOT_MEMBER.getErrorKey());

        verify(workspaceUserRepository, never()).delete(any());
    }

    @Test
    void testRefusesToSetAnEnvironmentRoleOnAnUnknownWorkspace() {
        // The tenant-admin path: hasWorkspaceScopeInEnvironment grants unconditionally for a tenant admin, and every
        // guard inside the method resolves to an empty set rather than throwing, so without the existence check the
        // insert reaches the workspace foreign key and surfaces as a 500.
        when(workspaceService.workspaceExists(WORKSPACE_ID)).thenReturn(false);

        assertThatThrownBy(
            () -> workspaceUserService.setEnvironmentRole(
                USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER, null))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                    .isEqualTo(WorkspaceErrorType.WORKSPACE_NOT_FOUND.getErrorKey());

        verify(workspaceUserRepository, never()).save(any());
    }
}

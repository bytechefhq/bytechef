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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceUserServiceTest {

    private static final long USER_ID = 42L;
    private static final long WORKSPACE_ID = 7L;

    private WorkspaceUserRepository workspaceUserRepository;
    private WorkspaceUserService workspaceUserService;

    @BeforeEach
    void setUp() {
        workspaceUserRepository = mock(WorkspaceUserRepository.class);

        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService = new WorkspaceUserServiceImpl(workspaceUserRepository);
    }

    @Test
    void testAddWorkspaceUserPersists() {
        WorkspaceUser result = workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(result.getWorkspaceRole()).isEqualTo(WorkspaceRole.EDITOR.ordinal());
    }

    @Test
    void testAddWorkspaceUserAcceptsACustomRole() {
        WorkspaceUser result = workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, null, 900L);

        assertThat(result.getCustomRoleId()).isEqualTo(900L);
        assertThat(result.getWorkspaceRole()).isNull();
    }

    @Test
    void testAddWorkspaceUserRejectsAnExistingMember() {
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(true);

        assertThatThrownBy(() -> workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("already a member");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testAddWorkspaceUserTakesTheWorkspaceLockBeforeCheckingMembership() {
        workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER);

        InOrder inOrder = inOrder(workspaceUserRepository);

        inOrder.verify(workspaceUserRepository)
            .lockWorkspace(WORKSPACE_ID);
        inOrder.verify(workspaceUserRepository)
            .existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
        inOrder.verify(workspaceUserRepository)
            .save(any());
    }

    @Test
    void testAssignCustomRoleRejectsConvertingTheLastAdmin() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN)));
        when(workspaceUserRepository.countAdminsForEnvironment(eq(WORKSPACE_ID), eq(WorkspaceRole.ADMIN.ordinal()),
            anyInt()))
                .thenReturn(1L);

        assertThatThrownBy(() -> workspaceUserService.assignCustomRole(USER_ID, WORKSPACE_ID, 900L))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("last admin");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testCountByCustomRoleIdDelegatesToRepository() {
        when(workspaceUserRepository.countByCustomRoleId(900L)).thenReturn(4L);

        assertThat(workspaceUserService.countByCustomRoleId(900L)).isEqualTo(4L);
    }

    @Test
    void testGetEnvironmentWorkspaceUserNamesAMemberWithoutAnEnvironmentRole() {
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(true);

        assertThatThrownBy(
            () -> workspaceUserService.getEnvironmentWorkspaceUser(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                .isEqualTo(WorkspaceUserErrorType.NO_ENVIRONMENT_ROLE.getErrorKey());
    }

    @Test
    void testGetLostAdminEnvironmentsKeepsTheAdminRowsOfOtherEnvironments() {
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(
                List.of(
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.DEVELOPMENT),
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION)));

        assertThat(
            workspaceUserService.getLostAdminEnvironments(
                USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER))
                    .isEqualTo(Set.of(Environment.PRODUCTION));
    }

    @Test
    void testGetWorkspaceWideWorkspaceUserRejectsAMemberInExplicitMode() {
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(true);

        assertThatThrownBy(() -> workspaceUserService.getWorkspaceWideWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("per-environment roles");
    }

    @Test
    void testIsWorkspaceMemberHoldsForAMemberInExplicitMode() {
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(true);

        assertThat(workspaceUserService.isWorkspaceMember(USER_ID, WORKSPACE_ID)).isTrue();
    }

    @Test
    void testRemoveEnvironmentRoleWidensTheLastEnvironmentRow() {
        WorkspaceUser environmentWorkspaceUser = WorkspaceUser.forRole(
            USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR, Environment.PRODUCTION);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal()))
                .thenReturn(Optional.of(environmentWorkspaceUser));
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of(environmentWorkspaceUser));

        workspaceUserService.removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION);

        ArgumentCaptor<WorkspaceUser> workspaceUserArgumentCaptor = ArgumentCaptor.forClass(WorkspaceUser.class);

        verify(workspaceUserRepository).delete(environmentWorkspaceUser);
        verify(workspaceUserRepository).save(workspaceUserArgumentCaptor.capture());

        WorkspaceUser workspaceWideWorkspaceUser = workspaceUserArgumentCaptor.getValue();

        assertThat(workspaceWideWorkspaceUser.getEnvironment()).isNull();
        assertThat(workspaceWideWorkspaceUser.getWorkspaceRole()).isEqualTo(WorkspaceRole.EDITOR.ordinal());
    }

    @Test
    void testRemoveWorkspaceUserBlockedWhenLastAdmin() {
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN)));
        when(workspaceUserRepository.countAdminsForEnvironment(eq(WORKSPACE_ID), eq(WorkspaceRole.ADMIN.ordinal()),
            anyInt()))
                .thenReturn(1L);

        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("last admin");

        verify(workspaceUserRepository, never()).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRemoveWorkspaceUserDeletesEveryRowTheMemberHolds() {
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(
                List.of(
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR),
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER, Environment.DEVELOPMENT)));

        assertThat(workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID)).isTrue();

        verify(workspaceUserRepository, times(1)).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRemoveWorkspaceUserThrowsWhenNotMember() {
        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("not a member");

        verify(workspaceUserRepository, never()).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testSetEnvironmentRoleDeletesTheImplicitRow() {
        WorkspaceUser implicitWorkspaceUser = WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(implicitWorkspaceUser));

        WorkspaceUser result = workspaceUserService.setEnvironmentRole(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER, null);

        assertThat(result.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(result.getWorkspaceRole()).isEqualTo(WorkspaceRole.VIEWER.ordinal());

        verify(workspaceUserRepository).delete(implicitWorkspaceUser);
    }

    @Test
    void testUpdateWorkspaceUserRoleBlockedWhenDemotingLastAdmin() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN)));
        when(workspaceUserRepository.countAdminsForEnvironment(eq(WORKSPACE_ID), eq(WorkspaceRole.ADMIN.ordinal()),
            anyInt()))
                .thenReturn(1L);

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("last admin");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }
}

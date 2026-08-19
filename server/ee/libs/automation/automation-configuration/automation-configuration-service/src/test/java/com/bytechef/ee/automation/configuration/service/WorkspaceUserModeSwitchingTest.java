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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private WorkspaceUserAuditPublisher workspaceUserAuditPublisher;

    @Mock
    private WorkspaceUserRepository workspaceUserRepository;

    @InjectMocks
    private WorkspaceUserServiceImpl workspaceUserService;

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
    void testRemovingTheLastEnvironmentRowRemovesMembership() {
        WorkspaceUser onlyWorkspaceUser =
            WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR, Environment.DEVELOPMENT);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT.ordinal())).thenReturn(Optional.of(onlyWorkspaceUser));

        workspaceUserService.removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT);

        verify(workspaceUserRepository).delete(onlyWorkspaceUser);

        // The security assertion: no implicit row is written to replace the removed one, which would silently promote
        // "revoke their last environment" into "grant them every environment".
        verify(workspaceUserRepository, never()).save(any());
    }

    @Test
    void testRemovingAnEnvironmentRoleTheMemberDoesNotHoldIsANoOp() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal())).thenReturn(Optional.empty());

        workspaceUserService.removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION);

        verify(workspaceUserRepository, never()).delete(any());
        verify(permissionService, never()).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }
}

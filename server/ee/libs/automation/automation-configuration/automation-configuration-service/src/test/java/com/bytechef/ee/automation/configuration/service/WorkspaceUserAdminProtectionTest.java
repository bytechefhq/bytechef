/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * No per-environment operation may leave an environment with nobody able to administer it.
 * <p>
 * The workspace-wide guard cannot express this: it counts ADMIN rows for the whole workspace, so an admin who moves
 * themselves to "ADMIN in Development only" still counts as one admin while Production is left unadministered.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceUserAdminProtectionTest {

    private static final long OTHER_USER_ID = 9L;
    private static final long USER_ID = 1L;
    private static final long WORKSPACE_ID = 2L;

    @Mock
    private PermissionService permissionService;

    @Mock
    private WorkspaceUserAuditPublisher workspaceUserAuditPublisher;

    @Mock
    private WorkspaceUserRepository workspaceUserRepository;

    @InjectMocks
    private WorkspaceUserServiceImpl workspaceUserService;

    @BeforeEach
    void setUp() {
        lenient().when(permissionService.isTenantAdmin())
            .thenReturn(false);
        lenient().when(permissionService.isCurrentUser(anyLong()))
            .thenReturn(false);
        lenient().when(workspaceUserRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testRefusesToStrandAnEnvironmentWithoutAnAdmin() {
        // The workspace's only admin, moving themselves to Development-only. Staging and Production would be left
        // with nobody able to administer them, and the workspace-wide admin count would still read one.
        givenImplicitRow(OTHER_USER_ID, WorkspaceRole.ADMIN);
        givenSoleAdminEverywhere();

        assertThatThrownBy(
            () -> workspaceUserService.setEnvironmentRole(
                OTHER_USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT, WorkspaceRole.ADMIN, null))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("without an admin");
    }

    @Test
    void testAllowsTheSplitWhenAnotherAdminCoversTheOtherEnvironments() {
        givenImplicitRow(OTHER_USER_ID, WorkspaceRole.ADMIN);
        when(workspaceUserRepository.countAdminsForEnvironment(anyLong(), anyInt(), anyInt()))
            .thenReturn(2L);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(anyLong(), anyLong(), anyInt()))
            .thenReturn(Optional.empty());

        assertThatCode(
            () -> workspaceUserService.setEnvironmentRole(
                OTHER_USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT, WorkspaceRole.ADMIN, null))
                    .doesNotThrowAnyException();
    }

    @Test
    void testRefusesToRemoveTheLastAdminOfAnEnvironment() {
        WorkspaceUser productionAdmin =
            WorkspaceUser.forRole(OTHER_USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            OTHER_USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal())).thenReturn(Optional.of(productionAdmin));
        when(
            workspaceUserRepository.countAdminsForEnvironment(
                WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal(), Environment.PRODUCTION.ordinal()))
                    .thenReturn(1L);

        assertThatThrownBy(
            () -> workspaceUserService.removeEnvironmentRole(OTHER_USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("PRODUCTION");
    }

    @Test
    void testAllowsRemovingANonAdminEnvironmentRole() {
        WorkspaceUser productionViewer =
            WorkspaceUser.forRole(OTHER_USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER, Environment.PRODUCTION);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            OTHER_USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal())).thenReturn(Optional.of(productionViewer));

        assertThatCode(
            () -> workspaceUserService.removeEnvironmentRole(OTHER_USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
                .doesNotThrowAnyException();
    }

    @Test
    void testRefusesSelfDemotionOutOfAnEnvironment() {
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);

        givenImplicitRow(USER_ID, WorkspaceRole.ADMIN);

        assertThatThrownBy(
            () -> workspaceUserService.setEnvironmentRole(
                USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT, WorkspaceRole.ADMIN, null))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("own");
    }

    @Test
    void testTenantAdminMayStrandAnEnvironment() {
        // A tenant admin administers every workspace regardless of membership, so they can always restore access.
        when(permissionService.isTenantAdmin()).thenReturn(true);

        givenImplicitRow(OTHER_USER_ID, WorkspaceRole.ADMIN);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(anyLong(), anyLong(), anyInt()))
            .thenReturn(Optional.empty());

        assertThatCode(
            () -> workspaceUserService.setEnvironmentRole(
                OTHER_USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT, WorkspaceRole.ADMIN, null))
                    .doesNotThrowAnyException();
    }

    private void givenImplicitRow(long userId, WorkspaceRole workspaceRole) {
        lenient().when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(userId, WORKSPACE_ID))
            .thenReturn(List.of(WorkspaceUser.forRole(userId, WORKSPACE_ID, workspaceRole)));
        lenient().when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forRole(userId, WORKSPACE_ID, workspaceRole)));
    }

    private void givenSoleAdminEverywhere() {
        lenient().when(workspaceUserRepository.countAdminsForEnvironment(anyLong(), anyInt(), anyInt()))
            .thenReturn(1L);
    }
}

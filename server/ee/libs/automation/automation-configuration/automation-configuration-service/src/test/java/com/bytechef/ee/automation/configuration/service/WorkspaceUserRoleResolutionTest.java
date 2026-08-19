/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.domain.ResolvedRole;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The resolution matrix: implicit only, explicit covering the environment, explicit not covering it, and neither.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceUserRoleResolutionTest {

    private static final long USER_ID = 1L;
    private static final long WORKSPACE_ID = 2L;

    @Mock
    private WorkspaceUserRepository workspaceUserRepository;

    @InjectMocks
    private WorkspaceUserServiceImpl workspaceUserService;

    @Test
    void testImplicitRowAppliesToEveryEnvironment() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal())).thenReturn(Optional.empty());
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR)));

        Optional<ResolvedRole> resolved = workspaceUserService.fetchRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION);

        assertThat(resolved).map(ResolvedRole::workspaceRole)
            .hasValue(WorkspaceRole.EDITOR);
    }

    @Test
    void testEnvironmentRowWins() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal()))
                .thenReturn(
                    Optional.of(
                        WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER, Environment.PRODUCTION)));

        Optional<ResolvedRole> resolved = workspaceUserService.fetchRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION);

        assertThat(resolved).map(ResolvedRole::workspaceRole)
            .hasValue(WorkspaceRole.VIEWER);

        // An environment row must short-circuit rather than merge with the implicit row.
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceIdAndEnvironmentIsNull(anyLong(), anyLong());
    }

    @Test
    void testExplicitModeDeniesAnEnvironmentWithNoRow() {
        // A member in explicit mode has no implicit row at all, so the fallback finds nothing and the environment is
        // denied. This is the case the whole feature exists for: "no access to Production".
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal())).thenReturn(Optional.empty());
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThat(workspaceUserService.fetchRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testResolvesACustomRole() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.STAGING.ordinal()))
                .thenReturn(
                    Optional.of(WorkspaceUser.forCustomRole(USER_ID, WORKSPACE_ID, 7L, Environment.STAGING)));

        Optional<ResolvedRole> resolved = workspaceUserService.fetchRole(USER_ID, WORKSPACE_ID, Environment.STAGING);

        assertThat(resolved).map(ResolvedRole::customRoleId)
            .hasValue(7L);
        assertThat(resolved).map(ResolvedRole::workspaceRole)
            .isEmpty();
    }
}

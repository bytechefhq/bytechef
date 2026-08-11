/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.user.service.WorkspaceMembershipAssigner.WorkspaceAssignment;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

/**
 * Covers the platform-to-automation seam that places an invited user into workspaces.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceMembershipAssignerTest {

    private static final long USER_ID = 42L;

    private WorkspaceUserService workspaceUserService;
    private WorkspaceMembershipAssignerImpl workspaceMembershipAssigner;

    @BeforeEach
    void setUp() {
        workspaceUserService = mock(WorkspaceUserService.class);

        workspaceMembershipAssigner = new WorkspaceMembershipAssignerImpl(workspaceUserService);
    }

    @Test
    void testAssignDelegatesEachAssignment() {
        workspaceMembershipAssigner.assign(
            USER_ID, List.of(new WorkspaceAssignment(1L, "EDITOR"), new WorkspaceAssignment(2L, "VIEWER")));

        // Delegating rather than writing rows keeps the already-a-member guard and the scope-cache eviction in one
        // place, so an invited member and a hand-added one go through identical checks.
        verify(workspaceUserService).addWorkspaceUser(USER_ID, 1L, WorkspaceRole.EDITOR);
        verify(workspaceUserService).addWorkspaceUser(USER_ID, 2L, WorkspaceRole.VIEWER);
    }

    @Test
    void testAssignWithNoAssignmentsDoesNothing() {
        workspaceMembershipAssigner.assign(USER_ID, List.of());

        verifyNoInteractions(workspaceUserService);
    }

    @Test
    void testAssignRejectsUnknownRole() {
        assertThatThrownBy(
            () -> workspaceMembershipAssigner.assign(USER_ID, List.of(new WorkspaceAssignment(1L, "SUPERUSER"))))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Unknown workspace role");

        // Substituting a default role would hand out an access level nobody asked for, so nothing is written.
        verify(workspaceUserService, never()).addWorkspaceUser(
            ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(), ArgumentMatchers.any());
    }
}

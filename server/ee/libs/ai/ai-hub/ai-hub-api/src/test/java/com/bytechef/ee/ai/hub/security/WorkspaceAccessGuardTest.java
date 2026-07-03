/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceAccessGuardTest {

    private static final long USER_ID = 1L;
    private static final long WORKSPACE_ID = 100L;

    @Test
    void testIsMemberReturnsTrueWhenUserBelongsToWorkspace() {
        WorkspaceFacade facade = mock(WorkspaceFacade.class);

        when(facade.getUserWorkspaces(USER_ID)).thenReturn(List.of(buildWorkspace(WORKSPACE_ID)));

        assertThat(WorkspaceAccessGuard.isMember(facade, USER_ID, WORKSPACE_ID)).isTrue();
    }

    @Test
    void testIsMemberReturnsFalseWhenUserDoesNotBelongToWorkspace() {
        WorkspaceFacade facade = mock(WorkspaceFacade.class);

        when(facade.getUserWorkspaces(USER_ID)).thenReturn(List.of(buildWorkspace(999L)));

        assertThat(WorkspaceAccessGuard.isMember(facade, USER_ID, WORKSPACE_ID)).isFalse();
    }

    @Test
    void testIsMemberReturnsFalseWhenUserHasNoWorkspaces() {
        WorkspaceFacade facade = mock(WorkspaceFacade.class);

        when(facade.getUserWorkspaces(USER_ID)).thenReturn(List.of());

        assertThat(WorkspaceAccessGuard.isMember(facade, USER_ID, WORKSPACE_ID)).isFalse();
    }

    @Test
    void testVerifyUserCanAccessWorkspacePassesForMember() {
        WorkspaceFacade facade = mock(WorkspaceFacade.class);

        when(facade.getUserWorkspaces(USER_ID)).thenReturn(List.of(buildWorkspace(WORKSPACE_ID)));

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(facade, USER_ID, WORKSPACE_ID);
    }

    @Test
    void testVerifyUserCanAccessWorkspaceThrowsForbiddenForNonMember() {
        WorkspaceFacade facade = mock(WorkspaceFacade.class);

        when(facade.getUserWorkspaces(USER_ID)).thenReturn(List.of(buildWorkspace(999L)));

        assertThatThrownBy(() -> WorkspaceAccessGuard.verifyUserCanAccessWorkspace(facade, USER_ID, WORKSPACE_ID))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void testVerifyUserCanAccessWorkspaceMessageDoesNotLeakExistence() {
        WorkspaceFacade facade = mock(WorkspaceFacade.class);

        when(facade.getUserWorkspaces(USER_ID)).thenReturn(List.of());

        ForbiddenException missingWorkspaceException = catchForbidden(facade, USER_ID, 0L);
        ForbiddenException existsButNotAccessibleException = catchForbidden(facade, USER_ID, WORKSPACE_ID);

        assertThat(missingWorkspaceException.getMessage())
            .as("the error message must not differentiate 'does not exist' from 'not accessible'")
            .doesNotContain("does not exist")
            .doesNotContain("not found")
            .contains("not accessible");
        assertThat(existsButNotAccessibleException.getMessage())
            .doesNotContain("does not exist")
            .doesNotContain("not found")
            .contains("not accessible");
    }

    private ForbiddenException catchForbidden(WorkspaceFacade facade, long userId, long workspaceId) {
        try {
            WorkspaceAccessGuard.verifyUserCanAccessWorkspace(facade, userId, workspaceId);

            throw new AssertionError("expected ForbiddenException");
        } catch (ForbiddenException exception) {
            return exception;
        }
    }

    private Workspace buildWorkspace(long id) {
        Workspace workspace = new Workspace();

        workspace.setId(id);
        workspace.setName("workspace-" + id);

        return workspace;
    }
}

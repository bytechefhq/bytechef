/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.automation.configuration.web.graphql.WorkspaceUserGraphQlController.WorkspaceUserView;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceUserEnvironmentGraphQlTest {

    private static final long USER_ID = 1L;
    private static final long WORKSPACE_ID = 2L;

    @Mock
    private WorkspaceUserService workspaceUserService;

    @InjectMocks
    private WorkspaceUserGraphQlController workspaceUserGraphQlController;

    @Test
    void testSetEnvironmentRoleDelegatesToTheService() {
        when(
            workspaceUserService.setEnvironmentRole(
                USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER, null))
                    .thenReturn(
                        WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER, Environment.PRODUCTION));

        WorkspaceUserView workspaceUserView = workspaceUserGraphQlController.setWorkspaceUserEnvironmentRole(
            WORKSPACE_ID, USER_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER, null);

        assertThat(workspaceUserView.environment()).isEqualTo("PRODUCTION");
        assertThat(workspaceUserView.workspaceRole()).isEqualTo("VIEWER");
    }

    @Test
    void testRemoveEnvironmentRoleDelegatesToTheService() {
        boolean removed = workspaceUserGraphQlController.removeWorkspaceUserEnvironmentRole(
            WORKSPACE_ID, USER_ID, Environment.DEVELOPMENT);

        assertThat(removed).isTrue();

        verify(workspaceUserService).removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT);
    }

    @Test
    void testAStoredImplicitRowHasNoEnvironment() {
        when(
            workspaceUserService.setEnvironmentRole(
                USER_ID, WORKSPACE_ID, Environment.STAGING, WorkspaceRole.EDITOR, null))
                    .thenReturn(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR));

        WorkspaceUserView workspaceUserView = workspaceUserGraphQlController.setWorkspaceUserEnvironmentRole(
            WORKSPACE_ID, USER_ID, Environment.STAGING, WorkspaceRole.EDITOR, null);

        assertThat(workspaceUserView.environment()).isNull();
    }

    @Test
    void testAnInheritedEntryHasNoEnvironment() {
        WorkspaceUserView workspaceUserView = WorkspaceUserView.inherited(WORKSPACE_ID, USER_ID);

        assertThat(workspaceUserView.environment()).isNull();
        assertThat(workspaceUserView.inherited()).isTrue();
    }
}

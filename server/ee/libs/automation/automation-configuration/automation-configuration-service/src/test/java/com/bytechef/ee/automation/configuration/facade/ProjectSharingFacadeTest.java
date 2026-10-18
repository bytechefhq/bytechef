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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.automation.configuration.security.ProjectVisibilityPolicy;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Behavioural tests for project visibility and named-user grants.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectSharingFacadeTest {

    private static final long NON_MEMBER_USER_ID = 99L;
    private static final long OTHER_WORKSPACE_ID = 2L;
    private static final long PROJECT_ID = 10L;
    private static final long UNKNOWN_PROJECT_ID = 404L;
    private static final long USER_ID = 7L;
    private static final long WORKSPACE_ID = 1L;

    private final ProjectService projectService = mock(ProjectService.class);
    private final ResourceGrantService resourceGrantService = mock(ResourceGrantService.class);
    private final WorkspaceUserService workspaceUserService = mock(WorkspaceUserService.class);

    private ProjectSharingFacadeImpl projectSharingFacade;

    @BeforeEach
    void setUp() {
        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setVisibility(ResourceVisibility.WORKSPACE);
        project.setWorkspaceId(WORKSPACE_ID);
        when(projectService.fetchProject(PROJECT_ID)).thenReturn(Optional.of(project));
        when(workspaceUserService.isWorkspaceMember(USER_ID, WORKSPACE_ID)).thenReturn(true);
        when(workspaceUserService.isWorkspaceMember(NON_MEMBER_USER_ID, WORKSPACE_ID)).thenReturn(false);

        projectSharingFacade = new ProjectSharingFacadeImpl(
            projectService, resourceGrantService,
            new ResourceVisibilityPolicyRegistry(List.of(new ProjectVisibilityPolicy())), workspaceUserService);
    }

    @Test
    void testSetVisibilityUpdates() {
        projectSharingFacade.setProjectVisibility(WORKSPACE_ID, PROJECT_ID, ResourceVisibility.PRIVATE);

        verify(projectService).updateVisibility(PROJECT_ID, ResourceVisibility.PRIVATE);
    }

    @Test
    void testSetVisibilityRejectsOrganization() {
        assertThatThrownBy(
            () -> projectSharingFacade.setProjectVisibility(
                WORKSPACE_ID, PROJECT_ID, ResourceVisibility.ORGANIZATION))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("does not support ORGANIZATION visibility")
                    .satisfies(
                        exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                            .isEqualTo(ProjectErrorType.UNSUPPORTED_VISIBILITY.getErrorKey()));
        verify(projectService, never()).updateVisibility(anyLong(), any());
    }

    @Test
    void testSetVisibilityRejectsProjectOutsideWorkspace() {
        assertThatThrownBy(
            () -> projectSharingFacade.setProjectVisibility(
                OTHER_WORKSPACE_ID, PROJECT_ID, ResourceVisibility.PRIVATE))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("does not belong to workspace")
                    .satisfies(
                        exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                            .isEqualTo(ProjectErrorType.INVALID_PROJECT.getErrorKey()));

        verify(projectService, never()).updateVisibility(anyLong(), any());
    }

    @Test
    void testGrantValidatesMembershipThenGrants() {
        projectSharingFacade.grantProjectAccess(WORKSPACE_ID, PROJECT_ID, USER_ID);

        verify(workspaceUserService).isWorkspaceMember(USER_ID, WORKSPACE_ID);
        verify(resourceGrantService).grant("Project", PROJECT_ID, USER_ID);
    }

    @Test
    void testGrantToNonMemberFailsWithTheUnknownProjectError() {
        assertThatThrownBy(
            () -> projectSharingFacade.grantProjectAccess(WORKSPACE_ID, PROJECT_ID, NON_MEMBER_USER_ID))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("is not a member of workspace")
                .satisfies(
                    exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                        .isEqualTo(ProjectErrorType.INVALID_PROJECT.getErrorKey()));
        verify(workspaceUserService).isWorkspaceMember(NON_MEMBER_USER_ID, WORKSPACE_ID);
        verify(resourceGrantService, never()).grant(anyString(), anyLong(), anyLong());
    }

    @Test
    void testGrantOnProjectOutsideWorkspaceFailsWithTheSameErrorAsANonMember() {
        assertThatThrownBy(
            () -> projectSharingFacade.grantProjectAccess(OTHER_WORKSPACE_ID, PROJECT_ID, USER_ID))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("does not belong to workspace")
                .satisfies(
                    exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                        .isEqualTo(ProjectErrorType.INVALID_PROJECT.getErrorKey()));
        verify(projectService).fetchProject(PROJECT_ID);
        verify(workspaceUserService, never()).isWorkspaceMember(anyLong(), anyLong());
        verify(resourceGrantService, never()).grant(anyString(), anyLong(), anyLong());
    }

    @Test
    void testGrantOnUnknownProjectFailsWithATypedError() {
        assertThatThrownBy(
            () -> projectSharingFacade.grantProjectAccess(WORKSPACE_ID, UNKNOWN_PROJECT_ID, USER_ID))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(
                    exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                        .isEqualTo(ProjectErrorType.INVALID_PROJECT.getErrorKey()));
        verify(projectService, never()).getProject(anyLong());
        verify(projectService).fetchProject(UNKNOWN_PROJECT_ID);
        verify(workspaceUserService, never()).isWorkspaceMember(anyLong(), anyLong());
        verify(resourceGrantService, never()).grant(anyString(), anyLong(), anyLong());
    }

    @Test
    void testRevokeSkipsMembershipCheck() {
        projectSharingFacade.revokeProjectAccess(WORKSPACE_ID, PROJECT_ID, NON_MEMBER_USER_ID);

        verify(resourceGrantService).revoke("Project", PROJECT_ID, NON_MEMBER_USER_ID);
        verify(workspaceUserService, never()).isWorkspaceMember(anyLong(), anyLong());
    }

    @Test
    void testGetGrantsReturnsUserIds() {
        when(resourceGrantService.getGrantedUserIds("Project", PROJECT_ID)).thenReturn(List.of(USER_ID));

        assertThat(projectSharingFacade.getProjectGrants(WORKSPACE_ID, PROJECT_ID)).containsExactly(USER_ID);
    }

    @Test
    void testGetGrantsRejectsProjectOutsideWorkspace() {
        assertThatThrownBy(() -> projectSharingFacade.getProjectGrants(OTHER_WORKSPACE_ID, PROJECT_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("does not belong to workspace");

        verify(resourceGrantService, never()).getGrantedUserIds(anyString(), anyLong());
    }
}

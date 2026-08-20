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

import com.bytechef.automation.configuration.audit.ProjectAuditEvent;
import com.bytechef.automation.configuration.audit.ProjectAuditPublisher;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.automation.configuration.security.ProjectVisibilityPolicy;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Behavioural tests for project visibility and named-user grants. The authorization expressions themselves are pinned
 * by {@link ProjectSharingFacadeAuthorizationTest}; these exercise the validation that runs after them.
 *
 * <p>
 * The negative cases assert on the <em>branch</em> that produced the rejection, not only on the error it carries: every
 * validation failure deliberately collapses to the same {@code INVALID_PROJECT} key so a caller cannot enumerate user
 * ids, which means the key alone cannot tell a membership rejection from an unknown-project one.
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

    private final ProjectAuditPublisher projectAuditPublisher = mock(ProjectAuditPublisher.class);
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

        // fetchProject, not getProject: the facade must fold an unknown id into its own typed error rather than let
        // getProject's NoSuchElementException escape. An id with no stub therefore reads as "no such project".
        when(projectService.fetchProject(PROJECT_ID)).thenReturn(Optional.of(project));
        when(workspaceUserService.fetchWorkspaceUser(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(mock(WorkspaceUser.class)));
        when(workspaceUserService.fetchWorkspaceUser(NON_MEMBER_USER_ID, WORKSPACE_ID)).thenReturn(Optional.empty());

        projectSharingFacade = new ProjectSharingFacadeImpl(
            projectAuditPublisher, projectService, resourceGrantService,
            new ResourceVisibilityPolicyRegistry(List.of(new ProjectVisibilityPolicy())), workspaceUserService);
    }

    @Test
    void testSetVisibilityUpdatesAndAudits() {
        projectSharingFacade.setProjectVisibility(WORKSPACE_ID, PROJECT_ID, ResourceVisibility.PRIVATE);

        verify(projectService).updateVisibility(PROJECT_ID, ResourceVisibility.PRIVATE);
        verify(projectAuditPublisher).publish(
            ProjectAuditEvent.PROJECT_VISIBILITY_CHANGED, PROJECT_ID, Map.of("toVisibility", "PRIVATE"));
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

        // ProjectServiceImpl.updateVisibility is a plain setter-and-save with no policy check of its own, and
        // ProjectMapper maps ORGANIZATION to THROW_EXCEPTION — a row written here would make every subsequent read
        // of this project throw. The rung must therefore be rejected before the service is reached.
        verify(projectService, never()).updateVisibility(anyLong(), any());
        verify(projectAuditPublisher, never()).publish(any(), anyLong(), any());
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
    void testGrantValidatesMembershipThenGrantsAndAudits() {
        projectSharingFacade.grantProjectAccess(WORKSPACE_ID, PROJECT_ID, USER_ID);

        verify(workspaceUserService).fetchWorkspaceUser(USER_ID, WORKSPACE_ID);
        verify(resourceGrantService).grant("Project", PROJECT_ID, USER_ID);
        verify(projectAuditPublisher).publish(
            ProjectAuditEvent.PROJECT_ACCESS_GRANTED, PROJECT_ID, Map.of("targetUserId", USER_ID));
    }

    @Test
    void testGrantToNonMemberFailsWithTheUnknownProjectError() {
        assertThatThrownBy(
            () -> projectSharingFacade.grantProjectAccess(WORKSPACE_ID, PROJECT_ID, NON_MEMBER_USER_ID))
                .isInstanceOf(ConfigurationException.class)
                // The message names the membership branch; the error key deliberately does not, so that the two
                // rejections are indistinguishable to a caller.
                .hasMessageContaining("is not a member of workspace")
                .satisfies(
                    exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                        .isEqualTo(ProjectErrorType.INVALID_PROJECT.getErrorKey()));

        // Proves the rejection came from the membership branch rather than from the project-in-workspace branch
        // ahead of it: control reached the membership lookup, which means the project validation passed.
        verify(workspaceUserService).fetchWorkspaceUser(NON_MEMBER_USER_ID, WORKSPACE_ID);
        verify(resourceGrantService, never()).grant(anyString(), anyLong(), anyLong());
        verify(projectAuditPublisher, never()).publish(any(), anyLong(), any());
    }

    @Test
    void testGrantOnProjectOutsideWorkspaceFailsWithTheSameErrorAsANonMember() {
        assertThatThrownBy(
            () -> projectSharingFacade.grantProjectAccess(OTHER_WORKSPACE_ID, PROJECT_ID, USER_ID))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("does not belong to workspace")
                .satisfies(
                    exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                        // Same key a non-member grant produces: the two are indistinguishable to a caller, which is
                        // what stops the endpoint from being used to enumerate user ids.
                        .isEqualTo(ProjectErrorType.INVALID_PROJECT.getErrorKey()));

        // And this one failed at the workspace-mismatch branch specifically, not at the missing-row branch beside it:
        // fetchProject(PROJECT_ID) is stubbed to return a project, so the row was found and only its workspace
        // disagreed. It failed before the membership lookup ran at all.
        verify(projectService).fetchProject(PROJECT_ID);
        verify(workspaceUserService, never()).fetchWorkspaceUser(anyLong(), anyLong());
        verify(resourceGrantService, never()).grant(anyString(), anyLong(), anyLong());
    }

    @Test
    void testGrantOnUnknownProjectFailsWithATypedError() {
        assertThatThrownBy(
            () -> projectSharingFacade.grantProjectAccess(WORKSPACE_ID, UNKNOWN_PROJECT_ID, USER_ID))
                // Not NoSuchElementException: that is what ProjectService.getProject raises for a missing row, and
                // letting it escape the facade surfaces as an unhandled 500 rather than a typed error.
                .isInstanceOf(ConfigurationException.class)
                .satisfies(
                    exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                        .isEqualTo(ProjectErrorType.INVALID_PROJECT.getErrorKey()));

        // The mechanism, not just the outcome: the throwing accessor must not be on this path at all. Were it still
        // called, an unstubbed mock would return null and this test could pass on an NPE-free accident elsewhere.
        verify(projectService, never()).getProject(anyLong());
        verify(projectService).fetchProject(UNKNOWN_PROJECT_ID);
        verify(workspaceUserService, never()).fetchWorkspaceUser(anyLong(), anyLong());
        verify(resourceGrantService, never()).grant(anyString(), anyLong(), anyLong());
    }

    @Test
    void testRevokeSkipsMembershipCheck() {
        // Someone removed from the workspace must still be revocable, otherwise their grant would be stranded.
        projectSharingFacade.revokeProjectAccess(WORKSPACE_ID, PROJECT_ID, NON_MEMBER_USER_ID);

        verify(resourceGrantService).revoke("Project", PROJECT_ID, NON_MEMBER_USER_ID);
        verify(workspaceUserService, never()).fetchWorkspaceUser(anyLong(), anyLong());
        verify(projectAuditPublisher).publish(
            ProjectAuditEvent.PROJECT_ACCESS_REVOKED, PROJECT_ID, Map.of("targetUserId", NON_MEMBER_USER_ID));
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

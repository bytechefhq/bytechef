/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
import com.bytechef.automation.ai.tool.exception.ProjectToolErrorType;
import com.bytechef.automation.ai.tool.exception.ProjectWorkflowToolErrorType;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ExecutionException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ScopedProjectGuardTest {

    private static final long OWNED_PROJECT_ID = 9L;
    private static final long OWN_PROJECT_ID = 7L;
    private static final long WORKSPACE_ID = 3L;

    @Mock
    private ProjectOwnershipService projectOwnershipService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private ScopedConnectionGuard scopedConnectionGuard;

    @Mock
    private WorkspaceScopeResolver workspaceScopeResolver;

    @Test
    void testCheckWorkflowAllowsAWorkflowOfTheScopedProject() {
        givenOwnedProject(OWN_PROJECT_ID);

        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-1"))
            .thenReturn(new ProjectWorkflow(OWN_PROJECT_ID, 1, "wf-1"));

        assertThatCode(
            () -> newGuard().checkWorkflow(
                "wf-1", null, scopedToolContext(), ProjectWorkflowToolErrorType.GET_WORKFLOW))
                    .doesNotThrowAnyException();

        verify(workspaceScopeResolver, never()).resolveWorkspace(any(), any());
    }

    @Test
    void testCheckWorkflowRejectsAWorkflowWhoseLookupFails() {
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-1"))
            .thenThrow(new IllegalStateException("database unavailable"));

        ScopedProjectGuard scopedProjectGuard = newGuard();

        assertThatCode(
            () -> scopedProjectGuard.checkWorkflow(
                "wf-1", null, scopedToolContext(), ProjectWorkflowToolErrorType.GET_WORKFLOW))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessageContaining("was not found in this chat's project");
    }

    @Test
    void testGetAccessibleProjectIdsReturnsOnlyTheScopedProject() {
        lenient().when(projectService.getWorkspaceProjectIds(anyLong(), anySet()))
            .thenReturn(List.of(OWN_PROJECT_ID, OWNED_PROJECT_ID));

        assertThat(
            newGuard().getAccessibleProjectIds(
                null, scopedToolContext(), ProjectWorkflowToolErrorType.SEARCH_WORKFLOWS))
                    .containsExactly(OWN_PROJECT_ID);
    }

    @Test
    void testGetProjectRejectsAConnectedUserProjectLikeAnInaccessibleOne() {
        Project project = mock(Project.class);

        when(project.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(projectService.fetchProject(OWNED_PROJECT_ID)).thenReturn(Optional.of(project));
        when(workspaceScopeResolver.resolveWorkspace(any(), any()))
            .thenReturn(new WorkspaceScopeResolver.Resolved(WORKSPACE_ID, 1L));
        when(projectOwnershipService.getOwnedProjectIds(any(), any()))
            .thenAnswer(invocation -> {
                Set<ProjectOwner> projectOwners = invocation.getArgument(1);

                return projectOwners.contains(ProjectOwner.EMBEDDED_CONNECTED_USER)
                    ? Set.of(OWNED_PROJECT_ID) : Set.of();
            });

        ScopedProjectGuard scopedProjectGuard = newGuard();

        assertThatCode(
            () -> scopedProjectGuard.getProject(
                OWNED_PROJECT_ID, WORKSPACE_ID, null, ProjectToolErrorType.GET_PROJECT))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage("Project " + OWNED_PROJECT_ID + " not found in workspace " + WORKSPACE_ID);
    }

    @Test
    void testGetProjectReturnsTheScopedProjectEvenWhenItIsOwned() {
        givenOwnedProject(OWN_PROJECT_ID);

        Project project = mock(Project.class);

        when(projectService.getProject(OWN_PROJECT_ID)).thenReturn(project);

        assertThat(newGuard().getProject(OWN_PROJECT_ID, null, scopedToolContext(), ProjectToolErrorType.GET_PROJECT))
            .isSameAs(project);
    }

    @Test
    void testGetProjectReturnsAnUnownedProjectOfTheWorkspace() {
        Project project = mock(Project.class);

        when(project.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(projectService.fetchProject(OWN_PROJECT_ID)).thenReturn(Optional.of(project));
        when(workspaceScopeResolver.resolveWorkspace(any(), any()))
            .thenReturn(new WorkspaceScopeResolver.Resolved(WORKSPACE_ID, 1L));

        assertThat(newGuard().getProject(OWN_PROJECT_ID, WORKSPACE_ID, null, ProjectToolErrorType.GET_PROJECT))
            .isSameAs(project);
    }

    @Test
    void testResolveWorkspaceIdRejectsAConfinedRun() {
        ScopedProjectGuard scopedProjectGuard = newGuard();

        assertThatCode(
            () -> scopedProjectGuard.resolveWorkspaceId(
                null, scopedToolContext(), ProjectWorkflowToolErrorType.SEARCH_WORKFLOWS))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);

        verify(workspaceScopeResolver, never()).resolveWorkspace(any(), any());
    }

    @Test
    void testRunThatSkipsAuthorizationWithoutAScopedProjectIsRejected() {
        ToolContext toolContext = new ToolContext(
            AgentToolInvocationContext.builder()
                .authentication(new UsernamePasswordAuthenticationToken("1234567890", null, List.of()))
                .skipAutomationAuthorization(true)
                .build()
                .toToolContext());

        ScopedProjectGuard scopedProjectGuard = newGuard();

        assertThatCode(
            () -> scopedProjectGuard.checkWorkflow("wf-1", null, toolContext,
                ProjectWorkflowToolErrorType.GET_WORKFLOW))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage(ScopedProjectGuard.UNSCOPED_MESSAGE);

        verify(projectWorkflowService, never()).getWorkflowProjectWorkflow(any());
        verify(workspaceScopeResolver, never()).resolveWorkspace(any(), any());
    }

    @Test
    void testCheckConnectionRejectsWithTheGuardsDenialMessage() {
        ToolContext toolContext = scopedToolContext();

        when(scopedConnectionGuard.fetchDenialMessage(null, 900L, toolContext))
            .thenReturn(ScopedConnectionGuard.UNVERIFIED_MESSAGE);

        ScopedProjectGuard scopedProjectGuard = newGuard();

        assertThatCode(
            () -> scopedProjectGuard.checkConnection(900L, toolContext,
                ProjectWorkflowToolErrorType.SAVE_TEST_CONNECTION))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage(ScopedConnectionGuard.UNVERIFIED_MESSAGE);
    }

    private void givenOwnedProject(long projectId) {
        lenient().when(projectOwnershipService.getOwnedProjectIds(any(), any()))
            .thenReturn(Set.of(projectId));
    }

    private ScopedProjectGuard newGuard() {
        return new ScopedProjectGuard(
            projectOwnershipService, projectService, projectWorkflowService, scopedConnectionGuard,
            workspaceScopeResolver);
    }

    private static ToolContext scopedToolContext() {
        return new ToolContext(Map.of(ScopedProjectToolContext.TOOL_CONTEXT_SCOPED_PROJECT_ID_KEY, OWN_PROJECT_ID));
    }
}

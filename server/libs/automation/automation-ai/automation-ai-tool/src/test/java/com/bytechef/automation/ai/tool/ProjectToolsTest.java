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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
import com.bytechef.automation.ai.tool.model.ProjectDetailInfo;
import com.bytechef.automation.ai.tool.model.ProjectInfo;
import com.bytechef.automation.ai.tool.model.ProjectStatusInfo;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectCategoryFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectTagFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ProjectToolsTest {

    private static final long WORKSPACE_ID = 1L;

    @Mock
    private ProjectCategoryFacade projectCategoryFacade;

    @Mock
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Mock
    private ProjectFacade projectFacade;

    @Mock
    private ProjectTagFacade projectTagFacade;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private ScopedConnectionGuard scopedConnectionGuard;

    @Mock
    private ToolContext toolContext;

    @Mock
    private WorkspaceFacade workspaceFacade;

    @Test
    void testListProjectsRejectsAWorkspaceTheCallerIsNotAMemberOf() {
        when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(2L, "Other")));

        ProjectTools projectTools = newTools();

        assertThatCode(() -> projectTools.listProjects(WORKSPACE_ID, null, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("Workspace 1 is not accessible to the caller.");

        verifyNoInteractions(projectFacade);
    }

    @Test
    void testListProjectsListsTheExplicitWorkspace() {
        givenTheCallerIsAWorkspaceMember();

        mockWorkspaceProjects(projectDTO(7L, "Alpha"));

        List<ProjectInfo> projectInfos = newTools().listProjects(WORKSPACE_ID, null, toolContext);

        assertThat(projectInfos).extracting(ProjectInfo::id)
            .containsExactly(7L);
    }

    @Test
    void testListProjectsFallsBackToTheChatWorkspace() {
        when(toolContext.getContext())
            .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID));

        mockWorkspaceProjects(projectDTO(7L, "Alpha"));

        assertThat(newTools().listProjects(null, null, toolContext)).hasSize(1);
    }

    @Test
    void testListProjectsRequiresAWorkspace() {
        when(toolContext.getContext()).thenReturn(Map.of());

        ProjectTools projectTools = newTools();

        assertThatCode(() -> projectTools.listProjects(null, null, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("listWorkspaces");
    }

    @Test
    void testSearchProjectsMatchesNameAndDescription() {
        givenTheCallerIsAWorkspaceMember();

        mockWorkspaceProjects(projectDTO(7L, "Alpha"), projectDTO(8L, "Beta"));

        List<ProjectInfo> projectInfos = newTools().searchProjects("alp", WORKSPACE_ID, toolContext);

        assertThat(projectInfos).extracting(ProjectInfo::id)
            .containsExactly(7L);
    }

    @Test
    void testListProjectsWithAQueryReturnsOnlyMatchingProjects() {
        givenTheCallerIsAWorkspaceMember();

        mockWorkspaceProjects(
            projectDTO(7L, "Alpha"), projectDTO(8L, "Beta"),
            ProjectDTO.builder()
                .description("Mirrors ALPHA data")
                .id(9L)
                .name("Gamma")
                .workspaceId(WORKSPACE_ID)
                .build());

        List<ProjectInfo> projectInfos = newTools().listProjects(WORKSPACE_ID, " alp ", toolContext);

        assertThat(projectInfos).extracting(ProjectInfo::id)
            .containsExactly(7L, 9L);
    }

    @Test
    void testListProjectsWithABlankQueryListsEveryProject() {
        givenTheCallerIsAWorkspaceMember();

        mockWorkspaceProjects(projectDTO(7L, "Alpha"), projectDTO(8L, "Beta"));

        assertThat(newTools().listProjects(WORKSPACE_ID, "  ", toolContext)).extracting(ProjectInfo::id)
            .containsExactly(7L, 8L);
    }

    @Test
    void testGetProjectCarriesTheProjectStatus() {
        Instant lastPublishedDate = Instant.parse("2026-10-01T10:00:00Z");

        when(projectFacade.getProject(7L)).thenReturn(
            ProjectDTO.builder()
                .id(7L)
                .lastProjectVersion(3)
                .lastPublishedDate(lastPublishedDate)
                .lastStatus(ProjectVersion.Status.PUBLISHED)
                .name("Alpha")
                .projectVersions(List.of())
                .workspaceId(WORKSPACE_ID)
                .build());

        ProjectDeploymentDTO projectDeploymentDTO = ProjectDeploymentDTO.builder()
            .enabled(true)
            .environment(Environment.PRODUCTION)
            .id(9L)
            .projectId(7L)
            .projectVersion(2)
            .build();

        when(projectDeploymentFacade.getWorkspaceProjectDeployments(WORKSPACE_ID, null, 7L, null, false))
            .thenReturn(List.of(projectDeploymentDTO));

        ProjectDetailInfo projectDetailInfo = newTools().getProject(7L, toolContext);

        assertThat(projectDetailInfo.status()).isEqualTo("PUBLISHED");
        assertThat(projectDetailInfo.isPublished()).isTrue();
        assertThat(projectDetailInfo.lastVersion()).isEqualTo(3);
        assertThat(projectDetailInfo.lastPublishedDate()).isEqualTo(lastPublishedDate);
        assertThat(projectDetailInfo.deployments()).singleElement()
            .satisfies(deployment -> {
                assertThat(deployment.id()).isEqualTo(9L);
                assertThat(deployment.environment()).isEqualTo("PRODUCTION");
                assertThat(deployment.enabled()).isTrue();
            });
    }

    @Test
    void testGetProjectMapsTheFacadeProject() {
        ProjectVersion projectVersion = new ProjectVersion(1);

        when(projectFacade.getProject(7L)).thenReturn(
            ProjectDTO.builder()
                .category(new Category(3L, "Sales"))
                .id(7L)
                .name("Alpha")
                .projectVersions(List.of(projectVersion))
                .tags(List.of(new Tag(5L, "crm")))
                .workspaceId(WORKSPACE_ID)
                .build());

        ProjectDetailInfo projectDetailInfo = newTools().getProject(7L, toolContext);

        assertThat(projectDetailInfo.id()).isEqualTo(7L);
        assertThat(projectDetailInfo.categoryId()).isEqualTo(3L);
        assertThat(projectDetailInfo.tagIds()).containsExactly(5L);
        assertThat(projectDetailInfo.versions()).hasSize(1);
    }

    @Test
    void testGetProjectSurfacesAFacadeFailureAsToolError() {
        when(projectFacade.getProject(7L)).thenThrow(new IllegalStateException("denied"));

        ProjectTools projectTools = newTools();

        assertThatCode(() -> projectTools.getProject(7L, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("denied");
    }

    @Test
    void testGetProjectStatusReadsTheProjectDeploymentsOfItsWorkspace() {
        when(projectFacade.getProject(7L)).thenReturn(projectDTO(7L, "Alpha"));

        ProjectDeploymentDTO projectDeploymentDTO = ProjectDeploymentDTO.builder()
            .enabled(true)
            .environment(Environment.DEVELOPMENT)
            .id(9L)
            .projectId(7L)
            .projectVersion(1)
            .build();

        when(projectDeploymentFacade.getWorkspaceProjectDeployments(WORKSPACE_ID, null, 7L, null, false))
            .thenReturn(List.of(projectDeploymentDTO));

        ProjectStatusInfo projectStatusInfo = newTools().getProjectStatus(7L, toolContext);

        assertThat(projectStatusInfo.deployments()).hasSize(1);
        assertThat(projectStatusInfo.deployments()
            .getFirst()
            .enabled()).isTrue();
    }

    @Test
    void testCreateProjectPassesTheResolvedCategoryAndTags() {
        givenTheCallerIsAWorkspaceMember();

        Category category = new Category(3L, "Sales");
        Tag tag = new Tag(5L, "crm");

        when(projectCategoryFacade.getProjectCategories(WORKSPACE_ID)).thenReturn(List.of(category));
        when(projectTagFacade.getProjectTags()).thenReturn(List.of(tag, new Tag(6L, "other")));
        when(projectFacade.createProject(any())).thenReturn(7L);
        when(projectFacade.getProject(7L)).thenReturn(projectDTO(7L, "Alpha"));

        newTools().createProject("Alpha", " notes ", 3L, WORKSPACE_ID, List.of(5L), toolContext);

        ArgumentCaptor<ProjectDTO> projectDTOArgumentCaptor = ArgumentCaptor.forClass(ProjectDTO.class);

        verify(projectFacade).createProject(projectDTOArgumentCaptor.capture());

        ProjectDTO projectDTO = projectDTOArgumentCaptor.getValue();

        assertThat(projectDTO.category()).isSameAs(category);
        assertThat(projectDTO.description()).isEqualTo("notes");
        assertThat(projectDTO.tags()).containsExactly(tag);
        assertThat(projectDTO.workspaceId()).isEqualTo(WORKSPACE_ID);
    }

    @Test
    void testCreateProjectRejectsAWorkspaceTheCallerCannotAccess() {
        when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(2L, "Other")));

        ProjectTools projectTools = newTools();

        assertThatCode(() -> projectTools.createProject("Alpha", null, null, WORKSPACE_ID, null, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("Workspace 1 is not accessible to the caller.");

        verifyNoInteractions(projectFacade);
    }

    @Test
    void testCreateProjectRejectsAWorkspaceOtherThanTheChatWorkspace() {
        when(toolContext.getContext())
            .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 2L));

        ProjectTools projectTools = newTools();

        assertThatCode(() -> projectTools.createProject("Alpha", null, null, WORKSPACE_ID, null, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("this conversation is scoped to");

        verifyNoInteractions(projectFacade);
    }

    @Test
    void testCreateProjectRejectsAnUnknownCategory() {
        givenTheCallerIsAWorkspaceMember();

        when(projectCategoryFacade.getProjectCategories(WORKSPACE_ID)).thenReturn(List.of());

        ProjectTools projectTools = newTools();

        assertThatCode(() -> projectTools.createProject("Alpha", null, 3L, WORKSPACE_ID, null, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("Category 3 not found");

        verify(projectFacade, never()).createProject(any());
    }

    @Test
    void testUpdateProjectKeepsUnchangedFieldsAndTheVersion() {
        Category category = new Category(3L, "Sales");

        when(projectFacade.getProject(7L)).thenReturn(
            ProjectDTO.builder()
                .category(category)
                .description("old")
                .id(7L)
                .name("Alpha")
                .version(4)
                .workspaceId(WORKSPACE_ID)
                .build());

        newTools().updateProject(7L, "Renamed", null, null, null, toolContext);

        ArgumentCaptor<ProjectDTO> projectDTOArgumentCaptor = ArgumentCaptor.forClass(ProjectDTO.class);

        verify(projectFacade).updateProject(projectDTOArgumentCaptor.capture());

        ProjectDTO projectDTO = projectDTOArgumentCaptor.getValue();

        assertThat(projectDTO.name()).isEqualTo("Renamed");
        assertThat(projectDTO.description()).isEqualTo("old");
        assertThat(projectDTO.category()).isSameAs(category);
        assertThat(projectDTO.version()).isEqualTo(4);
    }

    @Test
    void testDeleteProjectDeletesThroughTheFacade() {
        when(projectFacade.getProject(7L)).thenReturn(projectDTO(7L, "Alpha"));

        String result = newTools().deleteProject(7L, toolContext);

        verify(projectFacade).deleteProject(7L);

        assertThat(result).contains("Alpha");
    }

    @Test
    void testPublishProjectPublishesThroughTheFacade() {
        when(projectFacade.publishProject(7L, "v1", false)).thenReturn(2);
        when(projectFacade.getProject(7L)).thenReturn(projectDTO(7L, "Alpha"));

        assertThat(newTools().publishProject(7L, "v1", toolContext)
            .publishedVersion()).isEqualTo(2);
    }

    @Nested
    class ScopedProjectTest {

        private static final long OWN_PROJECT_ID = 7L;
        private static final long VENDOR_PROJECT_ID = 8L;

        @Test
        void testListProjectsListsOnlyTheScopedProject() {
            when(projectFacade.getProject(OWN_PROJECT_ID)).thenReturn(projectDTO(OWN_PROJECT_ID, "Alpha"));

            List<ProjectInfo> projectInfos = newTools().listProjects(WORKSPACE_ID, null, scopedToolContext());

            assertThat(projectInfos).extracting(ProjectInfo::id)
                .containsExactly(OWN_PROJECT_ID);

            verify(projectFacade, never()).getWorkspaceProjects(any(), any(), anyBoolean(), any(), any(), any(),
                anyLong());
            verifyNoInteractions(workspaceFacade);
        }

        @Test
        void testSearchProjectsSearchesOnlyTheScopedProject() {
            when(projectFacade.getProject(OWN_PROJECT_ID)).thenReturn(projectDTO(OWN_PROJECT_ID, "Alpha"));

            ProjectTools projectTools = newTools();

            assertThat(projectTools.searchProjects("alp", null, scopedToolContext())).extracting(ProjectInfo::id)
                .containsExactly(OWN_PROJECT_ID);
            assertThat(projectTools.searchProjects("beta", null, scopedToolContext())).isEmpty();

            verify(projectFacade, never()).getWorkspaceProjects(any(), any(), anyBoolean(), any(), any(), any(),
                anyLong());
            verifyNoInteractions(workspaceFacade);
        }

        @Test
        void testGetProjectRejectsAnotherProject() {
            ProjectTools projectTools = newTools();

            assertThatCode(() -> projectTools.getProject(VENDOR_PROJECT_ID, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);
            assertThatCode(() -> projectTools.getProjectStatus(VENDOR_PROJECT_ID, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);

            verifyNoInteractions(projectFacade, projectDeploymentFacade);
        }

        @Test
        void testGetProjectReadsTheScopedProject() {
            when(projectFacade.getProject(OWN_PROJECT_ID)).thenReturn(
                ProjectDTO.builder()
                    .id(OWN_PROJECT_ID)
                    .name("Alpha")
                    .projectVersions(List.of())
                    .workspaceId(WORKSPACE_ID)
                    .build());

            ProjectDetailInfo projectDetailInfo = newTools().getProject(OWN_PROJECT_ID, scopedToolContext());

            assertThat(projectDetailInfo.id()).isEqualTo(OWN_PROJECT_ID);
        }

        @Test
        void testConfinedRunCannotChangeProjects() {
            ProjectTools projectTools = newTools();

            assertThatCode(
                () -> projectTools.createProject("Alpha", null, null, WORKSPACE_ID, null, scopedToolContext()))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage(ScopedProjectGuard.PROJECT_CHANGE_MESSAGE);
            assertThatCode(
                () -> projectTools.updateProject(OWN_PROJECT_ID, "Renamed", null, null, null, scopedToolContext()))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage(ScopedProjectGuard.PROJECT_CHANGE_MESSAGE);
            assertThatCode(() -> projectTools.deleteProject(OWN_PROJECT_ID, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.PROJECT_CHANGE_MESSAGE);
            assertThatCode(() -> projectTools.publishProject(OWN_PROJECT_ID, "v1", scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.PROJECT_CHANGE_MESSAGE);

            verifyNoInteractions(projectCategoryFacade, projectFacade, projectTagFacade, workspaceFacade);
        }

        private static ToolContext scopedToolContext() {
            return new ToolContext(
                Map.of(ScopedProjectToolContext.TOOL_CONTEXT_SCOPED_PROJECT_ID_KEY, OWN_PROJECT_ID));
        }
    }

    private void givenTheCallerIsAWorkspaceMember() {
        when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(WORKSPACE_ID, "Workspace")));
    }

    private void mockWorkspaceProjects(ProjectDTO... projectDTOs) {
        when(projectFacade.getWorkspaceProjects(null, null, false, null, null, null, WORKSPACE_ID))
            .thenReturn(List.of(projectDTOs));
    }

    private ProjectTools newTools() {
        return new ProjectTools(
            projectCategoryFacade, projectDeploymentFacade, projectFacade, projectTagFacade,
            new ScopedProjectGuard(new GrantingPermissionService(), projectWorkflowService, scopedConnectionGuard),
            workspaceFacade);
    }

    private static ProjectDTO projectDTO(long projectId, String name) {
        return ProjectDTO.builder()
            .id(projectId)
            .name(name)
            .workspaceId(WORKSPACE_ID)
            .build();
    }
}

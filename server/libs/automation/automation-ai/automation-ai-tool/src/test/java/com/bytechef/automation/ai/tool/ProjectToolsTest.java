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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;
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

    private final WorkspaceFacade workspaceFacade = memberWorkspaceFacade();

    @Mock
    private ToolContext toolContext;

    @Test
    void testListProjectsListsTheExplicitWorkspace() {
        mockWorkspaceProjects(projectDTO(7L, "Alpha"));

        List<ProjectInfo> projectInfos = newTools().listProjects(WORKSPACE_ID, toolContext);

        assertThat(projectInfos).extracting(ProjectInfo::id)
            .containsExactly(7L);
    }

    @Test
    void testListProjectsFallsBackToTheChatWorkspace() {
        when(toolContext.getContext())
            .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID));

        mockWorkspaceProjects(projectDTO(7L, "Alpha"));

        assertThat(newTools().listProjects(null, toolContext)).hasSize(1);
    }

    @Test
    void testListProjectsRequiresAWorkspace() {
        when(toolContext.getContext()).thenReturn(Map.of());

        ProjectTools projectTools = newTools();

        assertThatCode(() -> projectTools.listProjects(null, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("listWorkspaces");
    }

    @Test
    void testSearchProjectsMatchesNameAndDescription() {
        mockWorkspaceProjects(projectDTO(7L, "Alpha"), projectDTO(8L, "Beta"));

        List<ProjectInfo> projectInfos = newTools().searchProjects("alp", WORKSPACE_ID, toolContext);

        assertThat(projectInfos).extracting(ProjectInfo::id)
            .containsExactly(7L);
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

        ProjectDetailInfo projectDetailInfo = newTools().getProject(7L);

        assertThat(projectDetailInfo.id()).isEqualTo(7L);
        assertThat(projectDetailInfo.categoryId()).isEqualTo(3L);
        assertThat(projectDetailInfo.tagIds()).containsExactly(5L);
        assertThat(projectDetailInfo.versions()).hasSize(1);
    }

    @Test
    void testGetProjectSurfacesAFacadeFailureAsToolError() {
        when(projectFacade.getProject(7L)).thenThrow(new IllegalStateException("denied"));

        ProjectTools projectTools = newTools();

        assertThatCode(() -> projectTools.getProject(7L))
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

        ProjectStatusInfo projectStatusInfo = newTools().getProjectStatus(7L);

        assertThat(projectStatusInfo.deployments()).hasSize(1);
        assertThat(projectStatusInfo.deployments()
            .getFirst()
            .enabled()).isTrue();
    }

    @Test
    void testCreateProjectPassesTheResolvedCategoryAndTags() {
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
    void testCreateProjectRejectsAnUnknownCategory() {
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

        newTools().updateProject(7L, "Renamed", null, null, null);

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

        String result = newTools().deleteProject(7L);

        verify(projectFacade).deleteProject(7L);

        assertThat(result).contains("Alpha");
    }

    @Test
    void testPublishProjectPublishesThroughTheFacade() {
        when(projectFacade.publishProject(7L, "v1", false)).thenReturn(2);
        when(projectFacade.getProject(7L)).thenReturn(projectDTO(7L, "Alpha"));

        assertThat(newTools().publishProject(7L, "v1")
            .publishedVersion()).isEqualTo(2);
    }

    private void mockWorkspaceProjects(ProjectDTO... projectDTOs) {
        when(projectFacade.getWorkspaceProjects(null, null, false, null, null, null, WORKSPACE_ID))
            .thenReturn(List.of(projectDTOs));
    }

    private ProjectTools newTools() {
        return new ProjectTools(
            projectCategoryFacade, projectDeploymentFacade, projectFacade, projectTagFacade, workspaceFacade);
    }

    private static ProjectDTO projectDTO(long projectId, String name) {
        return ProjectDTO.builder()
            .id(projectId)
            .name(name)
            .workspaceId(WORKSPACE_ID)
            .build();
    }

    private static WorkspaceFacade memberWorkspaceFacade() {
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        lenient().when(workspaceFacade.getMyWorkspaces())
            .thenReturn(
                List.of(
                    new Workspace(1L, "Workspace 1"), new Workspace(7L, "Workspace 7"),
                    new Workspace(42L, "Workspace 42"), new Workspace(43L, "Workspace 43")));

        return workspaceFacade;
    }
}

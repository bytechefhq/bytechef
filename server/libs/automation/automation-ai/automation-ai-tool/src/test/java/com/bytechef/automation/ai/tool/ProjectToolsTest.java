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
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.tool.model.ProjectDetailInfo;
import com.bytechef.automation.ai.tool.model.ProjectInfo;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Optional;
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

    private static final long FOREIGN_WORKSPACE_ID = 2L;
    private static final long WORKSPACE_ID = 1L;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectFacade projectFacade;

    @Mock
    private ProjectService projectService;

    @Mock
    private TagService tagService;

    @Mock
    private ToolContext toolContext;

    @Mock
    private WorkspaceScopeResolver workspaceScopeResolver;

    @Nested
    class WorkspaceScopeTest {

        @Test
        void testListProjectsListsOnlyTheResolvedWorkspaceProjects() {
            resolveWorkspace(WORKSPACE_ID);

            Project project = project(7L, WORKSPACE_ID);

            when(projectService.getWorkspaceProjectIds(WORKSPACE_ID)).thenReturn(List.of(7L));
            when(projectService.getProjects(List.of(7L))).thenReturn(List.of(project));

            List<ProjectInfo> projectInfos = newTools().listProjects(null, toolContext);

            assertThat(projectInfos).extracting(ProjectInfo::id)
                .containsExactly(7L);
            verify(projectService, never()).getProjects();
        }

        @Test
        void testListProjectsFailsClosedWhenTheWorkspaceIsRejected() {
            when(workspaceScopeResolver.resolveWorkspace(null, toolContext))
                .thenReturn(new WorkspaceScopeResolver.Rejected("{\"error\":\"workspace_required\"}"));

            ProjectTools projectTools = newTools();

            assertThatCode(() -> projectTools.listProjects(null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("workspace_required");

            verify(projectService, never()).getWorkspaceProjectIds(anyLong());
            verify(projectService, never()).getProjects();
        }

        @Test
        void testSearchProjectsSearchesOnlyTheResolvedWorkspaceProjects() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectService.getWorkspaceProjectIds(WORKSPACE_ID)).thenReturn(List.of(7L));
            when(projectService.getProjects(List.of(7L))).thenReturn(List.of(project(7L, WORKSPACE_ID)));

            List<ProjectInfo> projectInfos = newTools().searchProjects("project", null, toolContext);

            assertThat(projectInfos).hasSize(1);
            verify(projectService, never()).getProjects();
        }

        @Test
        void testGetProjectReadsAProjectOfTheWorkspace() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectService.fetchProject(7L)).thenReturn(Optional.of(project(7L, WORKSPACE_ID)));
            when(projectService.getProjectVersions(7L)).thenReturn(List.of());

            ProjectDetailInfo projectDetailInfo = newTools().getProject(7L, null, toolContext);

            assertThat(projectDetailInfo.id()).isEqualTo(7L);
        }

        @Test
        void testGetProjectRejectsAProjectOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectService.fetchProject(7L)).thenReturn(Optional.of(project(7L, FOREIGN_WORKSPACE_ID)));

            ProjectTools projectTools = newTools();

            assertThatCode(() -> projectTools.getProject(7L, null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Project 7 not found in workspace " + WORKSPACE_ID);

            verify(projectService, never()).getProjectVersions(anyLong());
        }

        @Test
        void testGetProjectStatusRejectsAProjectOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectService.fetchProject(7L)).thenReturn(Optional.of(project(7L, FOREIGN_WORKSPACE_ID)));

            ProjectTools projectTools = newTools();

            assertThatCode(() -> projectTools.getProjectStatus(7L, null, toolContext))
                .isInstanceOf(ExecutionException.class);

            verify(projectDeploymentService, never()).getProjectDeployments(anyLong());
        }

        @Test
        void testCreateProjectCreatesTheProjectInTheResolvedWorkspace() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectService.create(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

            newTools().createProject("project", null, null, null, null, toolContext);

            ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);

            verify(projectService).create(projectArgumentCaptor.capture());

            Project project = projectArgumentCaptor.getValue();

            assertThat(project.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        }

        @Test
        void testUpdateProjectRejectsAProjectOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectService.fetchProject(7L)).thenReturn(Optional.of(project(7L, FOREIGN_WORKSPACE_ID)));

            ProjectTools projectTools = newTools();

            assertThatCode(() -> projectTools.updateProject(7L, "renamed", null, null, null, null, toolContext))
                .isInstanceOf(ExecutionException.class);

            verify(projectFacade, never()).updateProject(any());
        }

        @Test
        void testDeleteProjectRejectsAProjectOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectService.fetchProject(7L)).thenReturn(Optional.of(project(7L, FOREIGN_WORKSPACE_ID)));

            ProjectTools projectTools = newTools();

            assertThatCode(() -> projectTools.deleteProject(7L, null, toolContext))
                .isInstanceOf(ExecutionException.class);

            verify(projectFacade, never()).deleteProject(anyLong());
        }

        @Test
        void testPublishProjectRejectsAProjectOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectService.fetchProject(7L)).thenReturn(Optional.of(project(7L, FOREIGN_WORKSPACE_ID)));

            ProjectTools projectTools = newTools();

            assertThatCode(() -> projectTools.publishProject(7L, null, null, toolContext))
                .isInstanceOf(ExecutionException.class);

            verify(projectFacade, never()).publishProject(anyLong(), any(), anyBoolean());
        }
    }

    private void resolveWorkspace(long workspaceId) {
        when(workspaceScopeResolver.resolveWorkspace(null, toolContext))
            .thenReturn(new WorkspaceScopeResolver.Resolved(workspaceId, 0L));
    }

    private ProjectTools newTools() {
        return new ProjectTools(
            categoryService, projectDeploymentService, projectFacade, projectService, tagService,
            workspaceScopeResolver);
    }

    private static Project project(long projectId, long workspaceId) {
        return Project.builder()
            .id(projectId)
            .name("project-" + projectId)
            .workspaceId(workspaceId)
            .build();
    }
}

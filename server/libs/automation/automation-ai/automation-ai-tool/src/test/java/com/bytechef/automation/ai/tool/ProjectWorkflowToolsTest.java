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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.ai.tool.model.WorkflowInfo;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Marko Kriskovic
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ProjectWorkflowToolsTest {

    private static final String DEFINITION = """
        {"label": "My Flow", "triggers": [], "tasks": []}""";

    private static final String WORKFLOW_DTO_DEFINITION = """
        {"label": "My Flow", "tasks": []}""";

    private static final long FOREIGN_WORKSPACE_ID = 2L;
    private static final long WORKSPACE_ID = 1L;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private ObjectProvider<WorkflowTestConfigurationFacade> testConfigurationFacadeProvider;

    @Mock
    private WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @Mock
    private ToolContext toolContext;

    @Mock
    private WorkspaceScopeResolver workspaceScopeResolver;

    @Nested
    class PersistedWorkflowCaptureTest {

        @Test
        void testCreateProjectWorkflowCapturesPersistedIds() {
            List<Map<String, Object>> captures = Collections.synchronizedList(new ArrayList<>());

            when(toolContext.getContext())
                .thenReturn(Map.of("bytechef.workflowEditor.persistedWorkflows", captures));

            resolveWorkspace(WORKSPACE_ID);
            mockProject(7L, WORKSPACE_ID);

            ProjectWorkflow projectWorkflow = buildProjectWorkflow(55L, 7L, "wf-uuid-1");

            when(projectWorkflowFacade.addWorkflow(7L, DEFINITION)).thenReturn(projectWorkflow);

            ProjectWorkflowTools tools = newTools();

            tools.createProjectWorkflow(7L, DEFINITION, null, toolContext);

            assertThat(captures).hasSize(1);
            assertThat(captures.get(0))
                .containsEntry("created", true)
                .containsEntry("workflowId", "wf-uuid-1")
                .containsEntry("projectId", 7L)
                .containsEntry("projectWorkflowId", 55L)
                .containsEntry("name", "My Flow");
        }

        @Test
        void testUpdateWorkflowCapturesPersistedIds() {
            List<Map<String, Object>> captures = Collections.synchronizedList(new ArrayList<>());

            when(toolContext.getContext())
                .thenReturn(Map.of("bytechef.workflowEditor.persistedWorkflows", captures));

            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-2", 9L, WORKSPACE_ID);

            ProjectWorkflowDTO dto = buildDto("wf-uuid-2", 88L, 3);

            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(dto);

            ProjectWorkflow projectWorkflow = buildProjectWorkflow(88L, 9L, "wf-uuid-2");

            when(projectWorkflowService.getProjectWorkflow(88L)).thenReturn(projectWorkflow);

            ProjectWorkflowTools tools = newTools();

            tools.updateWorkflow("wf-uuid-2", DEFINITION, null, toolContext);

            assertThat(captures).hasSize(1);
            assertThat(captures.get(0))
                .containsEntry("created", false)
                .containsEntry("workflowId", "wf-uuid-2")
                .containsEntry("projectId", 9L)
                .containsEntry("projectWorkflowId", 88L);
        }

        @Test
        void testUpdateWorkflowSucceedsWhenProjectIdLookupThrows() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-2", 9L, WORKSPACE_ID);

            ProjectWorkflowDTO dto = buildDto("wf-uuid-2", 88L, "Updated Flow", 3);

            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(dto);
            when(projectWorkflowService.getProjectWorkflow(88L)).thenThrow(new RuntimeException("lookup boom"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow("wf-uuid-2", DEFINITION, null, toolContext))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    class SearchWorkflowsTest {

        @Test
        void testSearchWorkflowsScopesToTheResolvedWorkspace() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectService.getWorkspaceProjectIds(WORKSPACE_ID)).thenReturn(List.of(10L));
            when(projectWorkflowFacade.getProjectWorkflows(10L)).thenReturn(List.of(buildDto("wf-1", 1L, 1)));

            ProjectWorkflowTools tools = newTools();

            List<WorkflowInfo> result = tools.searchWorkflows("flow", null, null, toolContext);

            assertThat(result).hasSize(1);

            verify(projectWorkflowFacade, never()).getProjectWorkflows();
        }

        @Test
        void testSearchWorkflowsRejectsAProjectOutsideTheWorkspace() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectService.getWorkspaceProjectIds(WORKSPACE_ID)).thenReturn(List.of(10L));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.searchWorkflows("flow", 99L, null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("99");

            verify(projectWorkflowFacade, never()).getProjectWorkflows(99L);
            verify(projectWorkflowFacade, never()).getProjectWorkflows();
        }

        @Test
        void testSearchWorkflowsFailsClosedWhenTheWorkspaceIsRejected() {
            when(workspaceScopeResolver.resolveWorkspace(null, toolContext))
                .thenReturn(new WorkspaceScopeResolver.Rejected("{\"error\":\"workspace_required\"}"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.searchWorkflows("flow", null, null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("workspace_required");

            verify(projectService, never()).getWorkspaceProjectIds(anyLong());
            verify(projectWorkflowFacade, never()).getProjectWorkflows(anyLong());
        }
    }

    @Nested
    class SaveWorkflowTestConnectionTest {

        @Test
        void testSaveWorkflowTestConnectionDelegatesToFacadeWithAutomationToolEnvironment() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, WORKSPACE_ID);

            when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(workflowTestConfigurationFacade);
            when(toolContext.getContext())
                .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L));

            ProjectWorkflowTools tools = newTools();

            String result = tools.saveWorkflowTestConnection(
                "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, null, toolContext);

            verify(workflowTestConfigurationFacade)
                .saveWorkflowTestConfigurationConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, 2L);
            assertThat(result).contains("1107", "sendChannelMessage_1");
        }

        @Test
        void testSaveWorkflowTestConnectionFallsBackToAgentToolEnvironmentKey() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, WORKSPACE_ID);

            when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(workflowTestConfigurationFacade);
            when(toolContext.getContext()).thenReturn(Map.of("bytechef.agentTool.environmentId", 1L));

            ProjectWorkflowTools tools = newTools();

            tools.saveWorkflowTestConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, null, toolContext);

            verify(workflowTestConfigurationFacade)
                .saveWorkflowTestConfigurationConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, 1L);
        }

        @Test
        void testSaveWorkflowTestConnectionFailsWhenNoEnvironmentIsResolvable() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, WORKSPACE_ID);

            when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(workflowTestConfigurationFacade);
            when(toolContext.getContext()).thenReturn(Map.of());

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.saveWorkflowTestConnection(
                    "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, null, toolContext))
                        .isInstanceOf(ExecutionException.class);

            verify(workflowTestConfigurationFacade, never())
                .saveWorkflowTestConfigurationConnection(any(), any(), any(), anyLong(), anyLong());
        }

        @Test
        void testSaveWorkflowTestConnectionThrowsWhenFacadeAbsent() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, WORKSPACE_ID);

            when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(null);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.saveWorkflowTestConnection(
                    "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, null, toolContext))
                        .isInstanceOf(ExecutionException.class);
        }
    }

    @Nested
    class WorkspaceScopeTest {

        @Test
        void testGetWorkflowReadsAWorkflowOfTheWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, WORKSPACE_ID);

            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-1")).thenReturn(buildDto("wf-uuid-1", 55L, 1));

            WorkflowInfo workflowInfo = newTools().getWorkflow("wf-uuid-1", null, toolContext);

            assertThat(workflowInfo.id()).isEqualTo("wf-uuid-1");
        }

        @Test
        void testGetWorkflowRejectsAWorkflowOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, FOREIGN_WORKSPACE_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.getWorkflow("wf-uuid-1", null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("not found in workspace " + WORKSPACE_ID);

            verify(projectWorkflowFacade, never()).getProjectWorkflow(anyString());
        }

        @Test
        void testGetWorkflowRejectsAnUnknownWorkflow() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectWorkflowService.getWorkflowProjectWorkflow("missing"))
                .thenThrow(new IllegalArgumentException("ProjectWorkflow not found"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.getWorkflow("missing", null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("not found in workspace " + WORKSPACE_ID);

            verify(projectWorkflowFacade, never()).getProjectWorkflow(anyString());
        }

        @Test
        void testGetWorkflowFailsClosedWhenTheWorkspaceIsRejected() {
            when(workspaceScopeResolver.resolveWorkspace(FOREIGN_WORKSPACE_ID, toolContext))
                .thenReturn(new WorkspaceScopeResolver.Rejected("{\"error\":\"not accessible\"}"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.getWorkflow("wf-uuid-1", FOREIGN_WORKSPACE_ID, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("not accessible");

            verify(projectWorkflowService, never()).getWorkflowProjectWorkflow(anyString());
            verify(projectWorkflowFacade, never()).getProjectWorkflow(anyString());
        }

        @Test
        void testListWorkflowsRejectsAProjectOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockProject(7L, FOREIGN_WORKSPACE_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.listWorkflows(7L, null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Project 7 not found in workspace " + WORKSPACE_ID);

            verify(projectWorkflowFacade, never()).getProjectWorkflows(anyLong());
        }

        @Test
        void testListWorkflowsListsAProjectOfTheWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockProject(7L, WORKSPACE_ID);

            when(projectWorkflowFacade.getProjectWorkflows(7L)).thenReturn(List.of(buildDto("wf-1", 1L, 1)));

            List<WorkflowInfo> workflowInfos = newTools().listWorkflows(7L, null, toolContext);

            assertThat(workflowInfos).hasSize(1);
        }

        @Test
        void testCreateProjectWorkflowRejectsAProjectOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockProject(7L, FOREIGN_WORKSPACE_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.createProjectWorkflow(7L, DEFINITION, null, toolContext))
                .isInstanceOf(ExecutionException.class);

            verify(projectWorkflowFacade, never()).addWorkflow(anyLong(), anyString());
        }

        @Test
        void testDeleteWorkflowRejectsAWorkflowOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, FOREIGN_WORKSPACE_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.deleteWorkflow("wf-uuid-1", null, toolContext))
                .isInstanceOf(ExecutionException.class);

            verify(projectWorkflowFacade, never()).deleteWorkflow(anyString());
        }

        @Test
        void testUpdateWorkflowRejectsAWorkflowOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, FOREIGN_WORKSPACE_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow("wf-uuid-1", DEFINITION, null, toolContext))
                .isInstanceOf(ExecutionException.class);

            verify(projectWorkflowFacade, never()).updateWorkflow(anyString(), anyString(), anyInt());
        }
    }

    private void mockProject(long projectId, long workspaceId) {
        Project project = Project.builder()
            .id(projectId)
            .name("project-" + projectId)
            .workspaceId(workspaceId)
            .build();

        when(projectService.fetchProject(projectId)).thenReturn(Optional.of(project));
    }

    private void mockWorkflow(String workflowId, long projectId, long workspaceId) {
        when(projectWorkflowService.getWorkflowProjectWorkflow(workflowId))
            .thenReturn(buildProjectWorkflow(100L, projectId, workflowId));

        mockProject(projectId, workspaceId);
    }

    private void resolveWorkspace(long workspaceId) {
        when(workspaceScopeResolver.resolveWorkspace(null, toolContext))
            .thenReturn(new WorkspaceScopeResolver.Resolved(workspaceId, 0L));
    }

    private ProjectWorkflowTools newTools() {
        return new ProjectWorkflowTools(
            projectService, projectWorkflowFacade, projectWorkflowService, testConfigurationFacadeProvider,
            workspaceScopeResolver);
    }

    private static ProjectWorkflow buildProjectWorkflow(long id, long projectId, String workflowId) {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(projectId, 1, workflowId);

        ReflectionTestUtils.setField(projectWorkflow, "id", id);

        return projectWorkflow;
    }

    private static ProjectWorkflowDTO buildDto(String workflowUuid, long projectWorkflowId, int version) {
        return buildDto(workflowUuid, projectWorkflowId, null, version);
    }

    private static ProjectWorkflowDTO buildDto(
        String workflowUuid, long projectWorkflowId, @SuppressWarnings("unused") String label, int version) {

        Workflow workflow = new Workflow(workflowUuid, WORKFLOW_DTO_DEFINITION, Workflow.Format.JSON);

        workflow.setVersion(version);

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(projectWorkflowId);

        return new ProjectWorkflowDTO(workflow, projectWorkflow, false);
    }
}

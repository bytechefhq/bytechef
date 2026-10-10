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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.ai.tool.model.WorkflowInfo;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    private static final long WORKSPACE_ID = 1L;

    @Mock
    private ProjectFacade projectFacade;

    private final WorkspaceFacade workspaceFacade = memberWorkspaceFacade();

    @Mock
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Mock
    private ObjectProvider<WorkflowTestConfigurationFacade> testConfigurationFacadeProvider;

    @Mock
    private WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @Mock
    private ToolContext toolContext;

    @Mock
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @Nested
    class PersistedWorkflowCaptureTest {

        @Test
        void testCreateProjectWorkflowCapturesPersistedIds() {
            List<Map<String, Object>> captures = Collections.synchronizedList(new ArrayList<>());

            when(toolContext.getContext())
                .thenReturn(Map.of("bytechef.workflowEditor.persistedWorkflows", captures));

            ProjectWorkflow projectWorkflow = buildProjectWorkflow(55L, 7L, "wf-uuid-1");

            when(projectWorkflowFacade.addWorkflow(7L, DEFINITION)).thenReturn(projectWorkflow);

            ProjectWorkflowTools tools = newTools();

            tools.createProjectWorkflow(7L, DEFINITION, toolContext);

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

            ProjectWorkflowDTO dto = buildDto("wf-uuid-2", 88L, 3);

            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(dto);

            when(projectWorkflowFacade.getWorkflowProjectId("wf-uuid-2")).thenReturn(9L);

            ProjectWorkflowTools tools = newTools();

            tools.updateWorkflow("wf-uuid-2", DEFINITION, toolContext);

            assertThat(captures).hasSize(1);
            assertThat(captures.get(0))
                .containsEntry("created", false)
                .containsEntry("workflowId", "wf-uuid-2")
                .containsEntry("projectId", 9L)
                .containsEntry("projectWorkflowId", 88L);
        }

        @Test
        void testUpdateWorkflowSucceedsWhenProjectIdLookupThrows() {
            ProjectWorkflowDTO dto = buildDto("wf-uuid-2", 88L, 3);

            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(dto);
            when(projectWorkflowFacade.getWorkflowProjectId("wf-uuid-2"))
                .thenThrow(new RuntimeException("lookup boom"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow("wf-uuid-2", DEFINITION, toolContext))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    class SearchWorkflowsTest {

        @Test
        void testSearchWorkflowsSearchesTheContextWorkspaceWhenNoProjectIsGiven() {
            when(toolContext.getContext())
                .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID));
            when(projectFacade.getWorkspaceProjectWorkflows(WORKSPACE_ID)).thenReturn(List.of(buildDto("wf-1", 1L, 1)));

            List<WorkflowInfo> result = newTools().searchWorkflows("flow", null, null, toolContext);

            assertThat(result).hasSize(1);
        }

        @Test
        void testSearchWorkflowsSearchesTheGivenProject() {
            when(projectWorkflowFacade.getProjectWorkflows(10L)).thenReturn(List.of(buildDto("wf-1", 1L, 1)));

            List<WorkflowInfo> result = newTools().searchWorkflows("flow", 10L, null, toolContext);

            assertThat(result).hasSize(1);

            verify(projectFacade, never()).getWorkspaceProjectWorkflows(anyLong());
        }

        @Test
        void testSearchWorkflowsRequiresAProjectOrAWorkspace() {
            when(toolContext.getContext()).thenReturn(Map.of());

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.searchWorkflows("flow", null, null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("listWorkspaces");

            verify(projectFacade, never()).getWorkspaceProjectWorkflows(anyLong());
        }
    }

    @Nested
    class SaveWorkflowTestConnectionTest {

        @Test
        void testSaveWorkflowTestConnectionDelegatesToFacadeWithAutomationToolEnvironment() {
            mockWorkflowWorkspace("wf-uuid-1", 7L, WORKSPACE_ID);

            when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(workflowTestConfigurationFacade);
            when(toolContext.getContext())
                .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L));

            mockConnections(WORKSPACE_ID, 2L, 1107L);

            ProjectWorkflowTools tools = newTools();

            String result = tools.saveWorkflowTestConnection(
                "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext);

            verify(workflowTestConfigurationFacade)
                .saveWorkflowTestConfigurationConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, 2L);
            assertThat(result).contains("1107", "sendChannelMessage_1");
        }

        @Test
        void testSaveWorkflowTestConnectionFallsBackToAgentToolEnvironmentKey() {
            mockWorkflowWorkspace("wf-uuid-1", 7L, WORKSPACE_ID);

            when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(workflowTestConfigurationFacade);
            when(toolContext.getContext()).thenReturn(Map.of("bytechef.agentTool.environmentId", 1L));

            mockConnections(WORKSPACE_ID, 1L, 1107L);

            ProjectWorkflowTools tools = newTools();

            tools.saveWorkflowTestConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext);

            verify(workflowTestConfigurationFacade)
                .saveWorkflowTestConfigurationConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, 1L);
        }

        @Test
        void testSaveWorkflowTestConnectionFailsWhenNoEnvironmentIsResolvable() {
            when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(workflowTestConfigurationFacade);
            when(toolContext.getContext()).thenReturn(Map.of());

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.saveWorkflowTestConnection(
                    "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext))
                        .isInstanceOf(ExecutionException.class);

            verify(workflowTestConfigurationFacade, never())
                .saveWorkflowTestConfigurationConnection(any(), any(), any(), anyLong(), anyLong());
        }

        @Test
        void testSaveWorkflowTestConnectionThrowsWhenFacadeAbsent() {
            when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(null);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.saveWorkflowTestConnection(
                    "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext))
                        .isInstanceOf(ExecutionException.class);
        }

        @Test
        void testSaveWorkflowTestConnectionRejectsAConnectionOutsideTheWorkflowWorkspace() {
            mockWorkflowWorkspace("wf-uuid-1", 7L, WORKSPACE_ID);

            when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(workflowTestConfigurationFacade);
            when(toolContext.getContext())
                .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L));

            mockConnections(WORKSPACE_ID, 2L, 5L);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.saveWorkflowTestConnection(
                    "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext))
                        .isInstanceOf(ExecutionException.class)
                        .hasMessageContaining("Connection 1107 not found");

            verify(workflowTestConfigurationFacade, never())
                .saveWorkflowTestConfigurationConnection(any(), any(), any(), anyLong(), anyLong());
        }
    }

    private void mockConnections(long workspaceId, long environmentId, long connectionId) {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .id(connectionId)
            .build();

        when(workspaceConnectionFacade.getConnections(workspaceId, null, null, environmentId, null))
            .thenReturn(List.of(connectionDTO));
    }

    private void mockWorkflowWorkspace(String workflowId, long projectId, long workspaceId) {
        when(projectWorkflowFacade.getWorkflowProjectId(workflowId)).thenReturn(projectId);
        when(projectFacade.getProject(projectId)).thenReturn(
            ProjectDTO.builder()
                .id(projectId)
                .workspaceId(workspaceId)
                .build());
    }

    private ProjectWorkflowTools newTools() {
        return new ProjectWorkflowTools(
            projectFacade, projectWorkflowFacade, workspaceConnectionFacade, testConfigurationFacadeProvider,
            workspaceFacade);
    }

    private static ProjectWorkflow buildProjectWorkflow(long id, long projectId, String workflowId) {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(projectId, 1, workflowId);

        ReflectionTestUtils.setField(projectWorkflow, "id", id);

        return projectWorkflow;
    }

    private static ProjectWorkflowDTO buildDto(String workflowUuid, long projectWorkflowId, int version) {
        Workflow workflow = new Workflow(workflowUuid, WORKFLOW_DTO_DEFINITION, Workflow.Format.JSON);

        workflow.setVersion(version);

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(projectWorkflowId);

        return new ProjectWorkflowDTO(workflow, projectWorkflow, false);
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

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
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
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    @Mock
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private ToolContext toolContext;

    @Mock
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @Mock
    private WorkspaceFacade workspaceFacade;

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

            when(projectWorkflowFacade.updateWorkflow("wf-uuid-2", DEFINITION, 3))
                .thenReturn(buildDto("wf-uuid-2", 88L, 4));
            when(projectWorkflowFacade.getWorkflowProjectId("wf-uuid-2")).thenReturn(9L);

            ProjectWorkflowTools tools = newTools();

            tools.updateWorkflow("wf-uuid-2", DEFINITION, 3, toolContext);

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
            when(projectWorkflowFacade.updateWorkflow("wf-uuid-2", DEFINITION, 3))
                .thenReturn(buildDto("wf-uuid-2", 88L, 4));
            when(projectWorkflowFacade.getWorkflowProjectId("wf-uuid-2"))
                .thenThrow(new RuntimeException("lookup boom"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow("wf-uuid-2", DEFINITION, 3, toolContext))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    class UpdateWorkflowTest {

        @Test
        void testUpdateWorkflowPassesTheVersionTheAgentReadAndReturnsTheNewVersion() {
            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(buildDto("wf-uuid-2", 88L, 3));
            when(projectWorkflowFacade.updateWorkflow("wf-uuid-2", DEFINITION, 3))
                .thenReturn(buildDto("wf-uuid-2", 88L, 4));

            WorkflowInfo workflowInfo = newTools().updateWorkflow("wf-uuid-2", DEFINITION, 3, toolContext);

            assertThat(workflowInfo.version()).isEqualTo(4);
            assertThat(workflowInfo.definition()).isEqualTo(DEFINITION);
        }

        @Test
        void testUpdateWorkflowRejectsAStaleVersion() {
            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(buildDto("wf-uuid-2", 88L, 5));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow("wf-uuid-2", DEFINITION, 3, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("getWorkflow");

            verify(projectWorkflowFacade, never()).updateWorkflow(any(), any(), anyInt());
        }
    }

    @Nested
    class ScopedProjectTest {

        private static final long OWN_PROJECT_ID = 1114L;
        private static final String OWN_WORKFLOW_ID = "own-workflow";
        private static final long VENDOR_PROJECT_ID = 7L;
        private static final String VENDOR_WORKFLOW_ID = "vendor-workflow";

        @Test
        void testScopedRunCannotUpdateAWorkflowOfAnotherProject() {
            stubWorkflowProject(VENDOR_WORKFLOW_ID, VENDOR_PROJECT_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow(VENDOR_WORKFLOW_ID, DEFINITION, 1, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);

            verify(projectWorkflowFacade, never()).getProjectWorkflow(VENDOR_WORKFLOW_ID);
            verify(projectWorkflowFacade, never()).updateWorkflow(any(), any(), anyInt());
        }

        @Test
        void testScopedRunCanUpdateAWorkflowOfItsOwnProject() {
            stubWorkflowProject(OWN_WORKFLOW_ID, OWN_PROJECT_ID);

            when(projectWorkflowFacade.getProjectWorkflow(OWN_WORKFLOW_ID))
                .thenReturn(buildDto(OWN_WORKFLOW_ID, 1L, 1));
            when(projectWorkflowFacade.updateWorkflow(OWN_WORKFLOW_ID, DEFINITION, 1))
                .thenReturn(buildDto(OWN_WORKFLOW_ID, 1L, 2));

            WorkflowInfo workflowInfo = newTools().updateWorkflow(OWN_WORKFLOW_ID, DEFINITION, 1, scopedToolContext());

            assertThat(workflowInfo.version()).isEqualTo(2);
        }

        @Test
        void testScopedRunCannotReadOrDeleteAWorkflowOfAnotherProject() {
            stubWorkflowProject(VENDOR_WORKFLOW_ID, VENDOR_PROJECT_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.getWorkflow(VENDOR_WORKFLOW_ID, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);
            assertThatCode(() -> tools.deleteWorkflow(VENDOR_WORKFLOW_ID, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);
            assertThatCode(
                () -> tools.saveWorkflowTestConnection(VENDOR_WORKFLOW_ID, "slack_1", "slack", 1L, scopedToolContext()))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);

            verify(projectWorkflowFacade, never()).getProjectWorkflow(VENDOR_WORKFLOW_ID);
            verify(projectWorkflowFacade, never()).deleteWorkflow(any());
        }

        @Test
        void testScopedRunTreatsAnUnknownWorkflowAsOutOfScope() {
            when(projectWorkflowService.getWorkflowProjectWorkflow("missing"))
                .thenThrow(new IllegalArgumentException("not found"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow("missing", DEFINITION, 1, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);
        }

        @Test
        void testScopedRunCannotListOrCreateInAnotherProject() {
            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.listWorkflows(VENDOR_PROJECT_ID, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);
            assertThatCode(() -> tools.createProjectWorkflow(VENDOR_PROJECT_ID, DEFINITION, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);

            verify(projectWorkflowFacade, never()).getProjectWorkflows(anyLong());
            verify(projectWorkflowFacade, never()).addWorkflow(anyLong(), any());
        }

        @Test
        void testScopedRunSearchesOnlyItsOwnProject() {
            when(projectWorkflowFacade.getProjectWorkflows(OWN_PROJECT_ID)).thenReturn(List.of());

            newTools().searchWorkflows("standup", null, WORKSPACE_ID, scopedToolContext());

            verify(projectWorkflowFacade).getProjectWorkflows(OWN_PROJECT_ID);
            verify(projectFacade, never()).getWorkspaceProjectWorkflows(anyLong());
        }

        @Test
        void testScopedRunCannotSearchAnotherProject() {
            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.searchWorkflows("standup", VENDOR_PROJECT_ID, null, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);
        }

        @Test
        void testUnscopedRunIsNotRestrictedByTheGuard() {
            when(projectWorkflowFacade.getProjectWorkflow(VENDOR_WORKFLOW_ID))
                .thenReturn(buildDto(VENDOR_WORKFLOW_ID, 501L, 1));
            when(projectWorkflowFacade.updateWorkflow(VENDOR_WORKFLOW_ID, DEFINITION, 1))
                .thenReturn(buildDto(VENDOR_WORKFLOW_ID, 501L, 2));

            newTools().updateWorkflow(VENDOR_WORKFLOW_ID, DEFINITION, 1, new ToolContext(Map.of()));

            verify(projectWorkflowFacade).updateWorkflow(VENDOR_WORKFLOW_ID, DEFINITION, 1);
            verify(projectWorkflowService, never()).getWorkflowProjectWorkflow(any());
        }

        private ToolContext scopedToolContext() {
            Map<String, Object> context = new HashMap<>(
                AgentToolInvocationContext.builder()
                    .environmentId(0L)
                    .tenantId("000001")
                    .authentication(new UsernamePasswordAuthenticationToken("1234567890", null, List.of()))
                    .build()
                    .toToolContext());

            context.put(ScopedProjectToolContext.TOOL_CONTEXT_SCOPED_PROJECT_ID_KEY, OWN_PROJECT_ID);

            return new ToolContext(context);
        }

        private void stubWorkflowProject(String workflowId, long projectId) {
            when(projectWorkflowService.getWorkflowProjectWorkflow(workflowId))
                .thenReturn(buildProjectWorkflow(1L, projectId, workflowId));
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

        @Test
        void testSearchWorkflowsRejectsAWorkspaceTheCallerCannotAccess() {
            when(toolContext.getContext()).thenReturn(Map.of());
            when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(WORKSPACE_ID, "Workspace")));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.searchWorkflows("flow", null, 99L, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Workspace 99 is not accessible to the caller.");

            verify(projectFacade, never()).getWorkspaceProjectWorkflows(anyLong());
        }

        @Test
        void testSearchWorkflowsRejectsAWorkspaceOtherThanTheChatWorkspace() {
            when(toolContext.getContext())
                .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.searchWorkflows("flow", null, 99L, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("this conversation is scoped to");

            verify(projectFacade, never()).getWorkspaceProjectWorkflows(anyLong());
        }
    }

    @Nested
    class SaveWorkflowTestConnectionTest {

        @Test
        void testSaveWorkflowTestConnectionDelegatesToFacadeWithAutomationToolEnvironment() {
            mockWorkflowWorkspace("wf-uuid-1", 7L, WORKSPACE_ID);

            when(toolContext.getContext())
                .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L));

            mockConnections(WORKSPACE_ID, 2L, 1107L);

            ProjectWorkflowTools tools = newTools();

            String result = tools.saveWorkflowTestConnection(
                "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext);

            verify(projectWorkflowFacade)
                .saveWorkflowTestConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, 2L);
            assertThat(result).contains("1107", "sendChannelMessage_1");
        }

        @Test
        void testSaveWorkflowTestConnectionFallsBackToAgentToolEnvironmentKey() {
            mockWorkflowWorkspace("wf-uuid-1", 7L, WORKSPACE_ID);

            when(toolContext.getContext()).thenReturn(Map.of("bytechef.agentTool.environmentId", 1L));

            mockConnections(WORKSPACE_ID, 1L, 1107L);

            ProjectWorkflowTools tools = newTools();

            tools.saveWorkflowTestConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext);

            verify(projectWorkflowFacade)
                .saveWorkflowTestConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, 1L);
        }

        @Test
        void testSaveWorkflowTestConnectionFailsWhenNoEnvironmentIsResolvable() {
            when(toolContext.getContext()).thenReturn(Map.of());

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.saveWorkflowTestConnection(
                    "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext))
                        .isInstanceOf(ExecutionException.class);

            verify(projectWorkflowFacade, never())
                .saveWorkflowTestConnection(any(), any(), any(), anyLong(), anyLong());
        }

        @Test
        void testSaveWorkflowTestConnectionRejectsAConnectionOutsideTheWorkflowWorkspace() {
            mockWorkflowWorkspace("wf-uuid-1", 7L, WORKSPACE_ID);

            when(toolContext.getContext())
                .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L));

            mockConnections(WORKSPACE_ID, 2L, 5L);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.saveWorkflowTestConnection(
                    "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext))
                        .isInstanceOf(ExecutionException.class)
                        .hasMessageContaining("Connection 1107 not found");

            verify(projectWorkflowFacade, never())
                .saveWorkflowTestConnection(any(), any(), any(), anyLong(), anyLong());
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
            projectFacade, projectWorkflowFacade, new ScopedProjectGuard(projectWorkflowService),
            workspaceConnectionFacade, workspaceFacade);
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
}

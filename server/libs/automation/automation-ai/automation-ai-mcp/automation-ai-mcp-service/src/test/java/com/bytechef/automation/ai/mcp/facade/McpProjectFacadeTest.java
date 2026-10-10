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

package com.bytechef.automation.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.audit.McpProjectAuditEvent;
import com.bytechef.automation.ai.mcp.audit.McpProjectAuditPublisher;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class McpProjectFacadeTest {

    private static final long WORKSPACE_ID = 500L;

    private final ComponentConnectionFacade componentConnectionFacade = mock(ComponentConnectionFacade.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final McpProjectAuditPublisher mcpProjectAuditPublisher = mock(McpProjectAuditPublisher.class);
    private final McpProjectService mcpProjectService = mock(McpProjectService.class);
    private final McpProjectWorkflowService mcpProjectWorkflowService = mock(McpProjectWorkflowService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final WorkflowTestConfigurationService workflowTestConfigurationService =
        mock(WorkflowTestConfigurationService.class);
    private final WorkspaceMcpServerService workspaceMcpServerService = mock(WorkspaceMcpServerService.class);

    private final McpProjectFacade mcpProjectFacade = new McpProjectFacadeImpl(
        componentConnectionFacade, connectionService, mcpProjectAuditPublisher, mcpProjectService,
        mcpProjectWorkflowService, mcpServerService, projectDeploymentFacade, projectDeploymentService,
        projectDeploymentWorkflowService,
        projectService, projectWorkflowService, workflowService, workflowTestConfigurationService,
        workspaceMcpServerService);

    @BeforeEach
    void beforeEach() {
        when(mcpServerService.getMcpServer(1L)).thenReturn(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.PRODUCTION));
        when(projectService.getProject(100L)).thenReturn(project(3));
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(1L)).thenReturn(Optional.of(WORKSPACE_ID));
        when(projectDeploymentService.create(any(ProjectDeployment.class)))
            .thenAnswer(invocation -> {
                ProjectDeployment projectDeployment = invocation.getArgument(0);

                projectDeployment.setId(10L);

                return projectDeployment;
            });
        when(mcpProjectService.create(any(McpProject.class)))
            .thenAnswer(invocation -> {
                McpProject mcpProject = invocation.getArgument(0);

                mcpProject.setId(20L);

                return mcpProject;
            });
        when(projectDeploymentWorkflowService.create(any(ProjectDeploymentWorkflow.class)))
            .thenAnswer(invocation -> {
                ProjectDeploymentWorkflow projectDeploymentWorkflow = invocation.getArgument(0);

                projectDeploymentWorkflow.setId(30L);

                return projectDeploymentWorkflow;
            });
        when(workflowService.getWorkflow(anyString())).thenAnswer(
            invocation -> workflow(invocation.getArgument(0)));
    }

    @Test
    void testCreateMcpProjectUsesMcpServerEnvironment() {
        mcpProjectFacade.createMcpProject(1L, 100L, 1, List.of());

        ArgumentCaptor<ProjectDeployment> projectDeploymentArgumentCaptor =
            ArgumentCaptor.forClass(ProjectDeployment.class);

        verify(projectDeploymentService).create(projectDeploymentArgumentCaptor.capture());

        ProjectDeployment projectDeployment = projectDeploymentArgumentCaptor.getValue();

        assertThat(projectDeployment.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testDeleteMcpProjectDeletesItsSystemDeploymentThroughTheDeploymentFacade() {
        McpProject mcpProject = new McpProject(10L, 1L);

        mcpProject.setId(20L);

        when(mcpProjectService.fetchMcpProject(20L)).thenReturn(Optional.of(mcpProject));

        mcpProjectFacade.deleteMcpProject(20L);

        verify(projectDeploymentFacade).deleteProjectDeployment(10L);
        verify(projectDeploymentService, never()).delete(anyLong());
        verify(mcpProjectService, never()).delete(anyLong());
        verify(mcpProjectAuditPublisher).publish(McpProjectAuditEvent.MCP_PROJECT_DELETED, 20L);
    }

    @Nested
    class WorkspaceIsolation {

        @Test
        void testCreateMcpProjectRejectsAnMcpServerOfAnotherWorkspace() {
            when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(1L)).thenReturn(Optional.of(501L));

            assertThatIllegalArgumentException()
                .isThrownBy(() -> mcpProjectFacade.createMcpProject(1L, 100L, 1, List.of()))
                .withMessageContaining("not in the same workspace");

            verify(projectDeploymentService, never()).create(any(ProjectDeployment.class));
            verify(mcpProjectService, never()).create(any(McpProject.class));
        }

        @Test
        void testCreateMcpProjectRejectsAnMcpServerWithoutWorkspace() {
            when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(1L)).thenReturn(Optional.empty());

            assertThatIllegalArgumentException()
                .isThrownBy(() -> mcpProjectFacade.createMcpProject(1L, 100L, 1, List.of()))
                .withMessageContaining("not in the same workspace");

            verify(projectDeploymentService, never()).create(any(ProjectDeployment.class));
        }
    }

    @Nested
    class CreateMcpProjectProjectVersionPublication {

        @Test
        void testCreateMcpProjectRejectsAnUnpublishedProject() {
            when(projectService.getProject(100L)).thenReturn(project(0));
            when(projectWorkflowService.getProjectWorkflowIds(100L, 1)).thenReturn(List.of("wf-1"));

            assertThatIllegalArgumentException()
                .isThrownBy(() -> mcpProjectFacade.createMcpProject(1L, 100L, 1, List.of("wf-1")))
                .withMessageContaining("Project 100 is not published");

            verify(projectDeploymentService, never()).create(any());
        }

        @Test
        void testCreateMcpProjectRejectsTheDraftProjectVersion() {
            when(projectWorkflowService.getProjectWorkflowIds(100L, 4)).thenReturn(List.of("wf-1"));

            assertThatIllegalArgumentException()
                .isThrownBy(() -> mcpProjectFacade.createMcpProject(1L, 100L, 4, List.of("wf-1")))
                .withMessageContaining("Version 4 of project 100 is not published");

            verify(projectDeploymentService, never()).create(any());
        }

        @Test
        void testCreateMcpProjectRejectsAProjectVersionThatDoesNotExist() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> mcpProjectFacade.createMcpProject(1L, 100L, 9, List.of()))
                .withMessageContaining("Version 9 of project 100 is not published");

            verify(projectDeploymentService, never()).create(any());
        }

        @Test
        void testCreateMcpProjectAcceptsAPublishedProjectVersion() {
            when(projectWorkflowService.getProjectWorkflowIds(100L, 2)).thenReturn(List.of("wf-1"));

            mcpProjectFacade.createMcpProject(1L, 100L, 2, List.of("wf-1"));

            verify(projectDeploymentService).create(any());
            verify(mcpProjectWorkflowService).create(20L, 30L);
        }
    }

    @Nested
    class ProjectDeploymentWorkflowConnections {

        private static final UUID WORKFLOW_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

        @BeforeEach
        void beforeEach() {
            when(projectWorkflowService.getWorkflowProjectWorkflow("wf-1"))
                .thenReturn(new ProjectWorkflow(100L, 3, "wf-1", WORKFLOW_UUID));
            when(projectWorkflowService.fetchLastProjectWorkflowId(100L, WORKFLOW_UUID.toString()))
                .thenReturn(Optional.of("wf-draft"));
            when(projectWorkflowService.getProjectWorkflowIds(100L, 3)).thenReturn(List.of("kept", "wf-1"));
        }

        @Test
        void testCreateMcpProjectCopiesTheTestConfigurationConnectionOfTheServerEnvironment() {
            stubComponentConnection(true);
            stubWorkflowTestConfiguration(Environment.PRODUCTION, 77L);
            stubConnection(77L, "slack", Environment.PRODUCTION);

            mcpProjectFacade.createMcpProject(1L, 100L, 3, List.of("wf-1"));

            assertThat(capturedProjectDeploymentWorkflow().getConnections())
                .singleElement()
                .satisfies(connection -> {
                    assertThat(connection.getConnectionId()).isEqualTo(77L);
                    assertThat(connection.getWorkflowConnectionKey()).isEqualTo("slack");
                    assertThat(connection.getWorkflowNodeName()).isEqualTo("trigger_1");
                });
        }

        @Test
        void testCreateMcpProjectRejectsAWorkflowWithoutItsRequiredConnection() {
            stubComponentConnection(true);

            assertThatIllegalArgumentException()
                .isThrownBy(() -> mcpProjectFacade.createMcpProject(1L, 100L, 3, List.of("wf-1")))
                .withMessageContaining("wf-1")
                .withMessageContaining("slack")
                .withMessageContaining("PRODUCTION");

            verify(projectDeploymentService, never()).create(any());
        }

        @Test
        void testCreateMcpProjectRejectsARequiredConnectionFromAnotherEnvironment() {
            stubComponentConnection(true);
            stubWorkflowTestConfiguration(Environment.PRODUCTION, 77L);
            stubConnection(77L, "slack", Environment.DEVELOPMENT);

            assertThatIllegalArgumentException()
                .isThrownBy(() -> mcpProjectFacade.createMcpProject(1L, 100L, 3, List.of("wf-1")))
                .withMessageContaining("wf-1");

            verify(projectDeploymentService, never()).create(any());
        }

        @Test
        void testCreateMcpProjectRejectsARequiredConnectionOfAnotherComponent() {
            stubComponentConnection(true);
            stubWorkflowTestConfiguration(Environment.PRODUCTION, 77L);
            stubConnection(77L, "github", Environment.PRODUCTION);

            assertThatIllegalArgumentException()
                .isThrownBy(() -> mcpProjectFacade.createMcpProject(1L, 100L, 3, List.of("wf-1")))
                .withMessageContaining("wf-1");

            verify(projectDeploymentService, never()).create(any());
        }

        @Test
        void testCreateMcpProjectAcceptsAWorkflowWithoutItsOptionalConnection() {
            stubComponentConnection(false);

            mcpProjectFacade.createMcpProject(1L, 100L, 3, List.of("wf-1"));

            assertThat(capturedProjectDeploymentWorkflow().getConnections()).isEmpty();
        }

        @Test
        void testUpdateMcpProjectCopiesTheTestConfigurationConnectionOfTheDeploymentEnvironment() {
            stubExistingMcpProject();
            stubComponentConnection(true);
            stubWorkflowTestConfiguration(Environment.STAGING, 78L);
            stubConnection(78L, "slack", Environment.STAGING);

            mcpProjectFacade.updateMcpProject(20L, List.of("kept", "wf-1"));

            assertThat(capturedProjectDeploymentWorkflow().getConnections())
                .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
                .containsExactly(78L);
        }

        @Test
        void testUpdateMcpProjectRejectsAnAddedWorkflowWithoutItsRequiredConnection() {
            stubExistingMcpProject();
            stubComponentConnection(true);

            assertThatIllegalArgumentException()
                .isThrownBy(() -> mcpProjectFacade.updateMcpProject(20L, List.of("wf-1")))
                .withMessageContaining("wf-1");

            verify(projectDeploymentWorkflowService, never()).create(any(ProjectDeploymentWorkflow.class));
            verify(mcpProjectWorkflowService, never()).delete(anyLong());
            verify(projectDeploymentWorkflowService, never()).delete(anyLong());
        }

        @Test
        void testUpdateMcpProjectDisablesARemovedWorkflowBeforeDeletingIt() {
            stubExistingMcpProject();

            mcpProjectFacade.updateMcpProject(20L, List.of());

            InOrder inOrder = inOrder(projectDeploymentFacade, projectDeploymentWorkflowService);

            inOrder.verify(projectDeploymentFacade)
                .enableProjectDeploymentWorkflow(10L, "kept", false);
            inOrder.verify(projectDeploymentWorkflowService)
                .delete(201L);
        }

        private ProjectDeploymentWorkflow capturedProjectDeploymentWorkflow() {
            ArgumentCaptor<ProjectDeploymentWorkflow> projectDeploymentWorkflowArgumentCaptor =
                ArgumentCaptor.forClass(ProjectDeploymentWorkflow.class);

            verify(projectDeploymentWorkflowService).create(projectDeploymentWorkflowArgumentCaptor.capture());

            return projectDeploymentWorkflowArgumentCaptor.getValue();
        }

        private void stubComponentConnection(boolean required) {
            when(componentConnectionFacade.getComponentConnections(any(WorkflowTrigger.class)))
                .thenReturn(List.of(new ComponentConnection("slack", 1, "trigger_1", "slack", required)));
        }

        private void stubConnection(long connectionId, String componentName, Environment environment) {
            Connection connection = new Connection();

            connection.setComponentName(componentName);
            connection.setEnvironmentId(environment.ordinal());
            connection.setId(connectionId);

            when(connectionService.getConnection(connectionId)).thenReturn(connection);
        }

        private void stubExistingMcpProject() {
            McpProject mcpProject = new McpProject(10L, 1L);

            mcpProject.setId(20L);

            when(mcpProjectService.fetchMcpProject(20L)).thenReturn(Optional.of(mcpProject));

            McpProjectWorkflow mcpProjectWorkflow = new McpProjectWorkflow(20L, 201L);

            mcpProjectWorkflow.setId(901L);

            when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(20L))
                .thenReturn(List.of(mcpProjectWorkflow));

            ProjectDeploymentWorkflow keptProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            keptProjectDeploymentWorkflow.setEnabled(true);
            keptProjectDeploymentWorkflow.setId(201L);
            keptProjectDeploymentWorkflow.setProjectDeploymentId(10L);
            keptProjectDeploymentWorkflow.setWorkflowId("kept");

            when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(10L))
                .thenReturn(List.of(keptProjectDeploymentWorkflow));

            ProjectDeployment projectDeployment = new ProjectDeployment();

            projectDeployment.setEnvironment(Environment.STAGING);
            projectDeployment.setId(10L);
            projectDeployment.setProjectId(100L);
            projectDeployment.setProjectVersion(3);

            when(projectDeploymentService.getProjectDeployment(10L)).thenReturn(projectDeployment);
        }

        private void stubWorkflowTestConfiguration(Environment environment, long connectionId) {
            WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

            workflowTestConfiguration.setConnections(
                List.of(new WorkflowTestConfigurationConnection(connectionId, "slack", "trigger_1")));

            when(workflowTestConfigurationService.fetchWorkflowTestConfiguration("wf-draft", environment.ordinal()))
                .thenReturn(Optional.of(workflowTestConfiguration));
        }
    }

    private static Project project(int publishedProjectVersionCount) {
        Project project = Project.builder()
            .id(100L)
            .name("project")
            .workspaceId(WORKSPACE_ID)
            .build();

        for (int index = 0; index < publishedProjectVersionCount; index++) {
            project.publish("v" + (index + 1));
        }

        return project;
    }

    private static Workflow workflow(String workflowId) {
        String definition = "{\"label\": \"" + workflowId + "\", \"triggers\": [{\"name\": \"trigger_1\", " +
            "\"type\": \"workflow/v1/newWorkflowCall\"}], \"tasks\": []}";

        return new Workflow(workflowId, definition, Workflow.Format.JSON);
    }
}

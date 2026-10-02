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

package com.bytechef.automation.ai.mcp.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.relational.core.mapping.event.AfterSaveEvent;
import org.springframework.data.relational.core.mapping.event.BeforeConvertEvent;

/**
 * Unit test for {@link McpServerAfterSaveEventListener}.
 *
 * @author Ivica Cardic
 */
public class McpServerAfterSaveEventListenerTest {

    private static final long MCP_SERVER_ID = 1L;
    private static final long PROJECT_DEPLOYMENT_A_ID = 100L;
    private static final long PROJECT_DEPLOYMENT_B_ID = 200L;
    private static final String REQUIRED_INPUT_NAME = "apiKey";
    private static final String WORKFLOW_ID = "wf-1";

    private final McpProjectService mcpProjectService = mock(McpProjectService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final TriggerLifecycleFacade triggerLifecycleFacade = mock(TriggerLifecycleFacade.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    @Test
    public void testOnAfterSaveEnabledServerEnablesProjectDeployments() {
        // Given
        McpServerAfterSaveEventListener listener = new McpServerAfterSaveEventListener(
            mcpProjectService, mcpServerService, projectDeploymentFacade);

        McpServer mcpServer = new McpServer();
        mcpServer.setId(1L);
        mcpServer.setEnabled(true);

        McpProject mcpProject1 = new McpProject(100L, 1L);
        McpProject mcpProject2 = new McpProject(200L, 1L);
        List<McpProject> mcpProjects = Arrays.asList(mcpProject1, mcpProject2);

        when(mcpProjectService.getMcpServerMcpProjects(1L)).thenReturn(mcpProjects);

        @SuppressWarnings("unchecked")
        AfterSaveEvent<McpServer> event = mock(AfterSaveEvent.class);
        when(event.getEntity()).thenReturn(mcpServer);

        // When
        listener.onAfterSave(event);

        // Then
        verify(mcpProjectService).getMcpServerMcpProjects(1L);
        verify(projectDeploymentFacade).enableProjectDeployment(eq(100L), eq(true));
        verify(projectDeploymentFacade).enableProjectDeployment(eq(200L), eq(true));
    }

    @Test
    public void testOnAfterSaveDisabledServerDisablesProjectDeployments() {
        // Given
        McpServerAfterSaveEventListener listener = new McpServerAfterSaveEventListener(
            mcpProjectService, mcpServerService, projectDeploymentFacade);

        McpServer mcpServer = new McpServer();
        mcpServer.setId(1L);
        mcpServer.setEnabled(false);

        McpProject mcpProject1 = new McpProject(100L, 1L);
        McpProject mcpProject2 = new McpProject(200L, 1L);
        List<McpProject> mcpProjects = Arrays.asList(mcpProject1, mcpProject2);

        when(mcpProjectService.getMcpServerMcpProjects(1L)).thenReturn(mcpProjects);

        @SuppressWarnings("unchecked")
        AfterSaveEvent<McpServer> event = mock(AfterSaveEvent.class);
        when(event.getEntity()).thenReturn(mcpServer);

        // When
        listener.onAfterSave(event);

        // Then
        verify(mcpProjectService).getMcpServerMcpProjects(1L);
        verify(projectDeploymentFacade).enableProjectDeployment(eq(100L), eq(false));
        verify(projectDeploymentFacade).enableProjectDeployment(eq(200L), eq(false));
    }

    @Test
    public void testOnAfterSaveServerWithNoProjectsNoFacadeCalls() {
        // Given
        McpServerAfterSaveEventListener listener = new McpServerAfterSaveEventListener(
            mcpProjectService, mcpServerService, projectDeploymentFacade);

        McpServer mcpServer = new McpServer();
        mcpServer.setId(1L);
        mcpServer.setEnabled(true);

        when(mcpProjectService.getMcpServerMcpProjects(1L)).thenReturn(Collections.emptyList());

        @SuppressWarnings("unchecked")
        AfterSaveEvent<McpServer> event = mock(AfterSaveEvent.class);
        when(event.getEntity()).thenReturn(mcpServer);

        // When
        listener.onAfterSave(event);

        // Then
        verify(mcpProjectService).getMcpServerMcpProjects(1L);
        verify(projectDeploymentFacade, times(0)).enableProjectDeployment(eq(100L), eq(true));
        verify(projectDeploymentFacade, times(0)).enableProjectDeployment(eq(200L), eq(true));
    }

    @Test
    public void testOnAfterSaveMultipleProjectsWithDifferentDeployments() {
        // Given
        McpServerAfterSaveEventListener listener = new McpServerAfterSaveEventListener(
            mcpProjectService, mcpServerService, projectDeploymentFacade);

        McpServer mcpServer = new McpServer();
        mcpServer.setId(2L);
        mcpServer.setEnabled(true);

        McpProject mcpProject1 = new McpProject(300L, 2L);
        McpProject mcpProject2 = new McpProject(400L, 2L);
        McpProject mcpProject3 = new McpProject(500L, 2L);
        List<McpProject> mcpProjects = Arrays.asList(mcpProject1, mcpProject2, mcpProject3);

        when(mcpProjectService.getMcpServerMcpProjects(2L)).thenReturn(mcpProjects);

        @SuppressWarnings("unchecked")
        AfterSaveEvent<McpServer> event = mock(AfterSaveEvent.class);
        when(event.getEntity()).thenReturn(mcpServer);

        // When
        listener.onAfterSave(event);

        // Then
        verify(mcpProjectService).getMcpServerMcpProjects(2L);
        verify(projectDeploymentFacade).enableProjectDeployment(eq(300L), eq(true));
        verify(projectDeploymentFacade).enableProjectDeployment(eq(400L), eq(true));
        verify(projectDeploymentFacade).enableProjectDeployment(eq(500L), eq(true));
    }

    @Test
    public void testRenamingAnEnabledServerTouchesNoProjectDeployment() {
        givenStoredMcpServer(true);
        givenProjectDeployments(100L);

        McpServer renamedMcpServer = mcpServer(true);

        renamedMcpServer.setName("renamed");

        saveThroughListener(renamedMcpServer);

        verify(projectDeploymentFacade, never()).checkEnableProjectDeployment(anyLong(), anyBoolean());
        verify(projectDeploymentFacade, never()).enableProjectDeployment(anyLong(), anyBoolean());
    }

    @Test
    public void testSavingADisabledServerThatStaysDisabledTouchesNoProjectDeployment() {
        givenStoredMcpServer(false);
        givenProjectDeployments(100L);

        saveThroughListener(mcpServer(false));

        verify(projectDeploymentFacade, never()).checkEnableProjectDeployment(anyLong(), anyBoolean());
        verify(projectDeploymentFacade, never()).enableProjectDeployment(anyLong(), anyBoolean());
    }

    @Test
    public void testEnablingAServerChecksEveryDeploymentBeforeEnablingAny() {
        givenStoredMcpServer(false);
        givenProjectDeployments(100L, 200L);

        saveThroughListener(mcpServer(true));

        InOrder inOrder = inOrder(projectDeploymentFacade);

        inOrder.verify(projectDeploymentFacade)
            .checkEnableProjectDeployment(100L, true);
        inOrder.verify(projectDeploymentFacade)
            .checkEnableProjectDeployment(200L, true);
        inOrder.verify(projectDeploymentFacade)
            .enableProjectDeployment(100L, true);
        inOrder.verify(projectDeploymentFacade)
            .enableProjectDeployment(200L, true);
    }

    @Test
    public void testAFlagRecordedForOneSaveNeverDecidesAnotherSaveOfTheSameId() {
        givenStoredMcpServer(true);
        givenProjectDeployments(100L);

        McpServerAfterSaveEventListener mcpServerAfterSaveEventListener = new McpServerAfterSaveEventListener(
            mcpProjectService, mcpServerService, projectDeploymentFacade);

        mcpServerAfterSaveEventListener.onBeforeConvert(new BeforeConvertEvent<>(mcpServer(true)));

        mcpServerAfterSaveEventListener.onAfterSave(afterSaveEvent(mcpServer(true)));

        verify(projectDeploymentFacade).checkEnableProjectDeployment(100L, true);
        verify(projectDeploymentFacade).enableProjectDeployment(100L, true);
    }

    @Test
    public void testEnablingAServerEnablesNoDeploymentWhenTheCheckOfALaterOneFails() {
        givenStoredMcpServer(false);
        givenProjectDeployments(100L, 200L);

        doThrow(new IllegalArgumentException("Missing required param: apiKey")).when(projectDeploymentFacade)
            .checkEnableProjectDeployment(200L, true);

        assertThatThrownBy(() -> saveThroughListener(mcpServer(true)))
            .isInstanceOf(IllegalArgumentException.class);

        verify(projectDeploymentFacade, never()).enableProjectDeployment(anyLong(), anyBoolean());
    }

    @Test
    public void testDisablingAServerDisablesItsProjectDeployments() {
        givenStoredMcpServer(true);
        givenProjectDeployments(100L);

        saveThroughListener(mcpServer(false));

        verify(projectDeploymentFacade).checkEnableProjectDeployment(100L, false);
        verify(projectDeploymentFacade).enableProjectDeployment(100L, false);
    }

    @Test
    public void testEnablingAServerArmsNoDeploymentWhenALaterDeploymentIsMissingARequiredInput() {
        givenTwoDeploymentsWhereTheSecondMissesARequiredInput();

        McpServerAfterSaveEventListener mcpServerAfterSaveEventListener = new McpServerAfterSaveEventListener(
            mcpProjectService, mock(McpServerService.class), createProjectDeploymentFacade());

        assertThatThrownBy(() -> mcpServerAfterSaveEventListener.onAfterSave(afterSaveEvent(mcpServer(true))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Missing required param: " + REQUIRED_INPUT_NAME);

        verify(triggerLifecycleFacade, never())
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
        verify(projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    private void givenProjectDeployments(long... projectDeploymentIds) {
        List<McpProject> mcpProjects = new ArrayList<>();

        for (long projectDeploymentId : projectDeploymentIds) {
            mcpProjects.add(new McpProject(projectDeploymentId, MCP_SERVER_ID));
        }

        when(mcpProjectService.getMcpServerMcpProjects(MCP_SERVER_ID)).thenReturn(mcpProjects);
    }

    private void givenStoredMcpServer(boolean enabled) {
        when(mcpServerService.getMcpServer(MCP_SERVER_ID)).thenReturn(mcpServer(enabled));
    }

    private void givenTwoDeploymentsWhereTheSecondMissesARequiredInput() {
        when(mcpProjectService.getMcpServerMcpProjects(MCP_SERVER_ID)).thenReturn(
            List.of(
                new McpProject(PROJECT_DEPLOYMENT_A_ID, MCP_SERVER_ID),
                new McpProject(PROJECT_DEPLOYMENT_B_ID, MCP_SERVER_ID)));

        givenEnabledRow(PROJECT_DEPLOYMENT_A_ID, Map.of(REQUIRED_INPUT_NAME, "secret"));
        givenEnabledRow(PROJECT_DEPLOYMENT_B_ID, Map.of());

        Workflow workflow = mock(Workflow.class);
        WorkflowTrigger workflowTrigger = mock(WorkflowTrigger.class);

        when(workflow.getId()).thenReturn(WORKFLOW_ID);
        when(workflow.getInputs()).thenReturn(
            List.of(new Workflow.Input(REQUIRED_INPUT_NAME, "API key", "string", true, Map.of())));
        when(workflowTrigger.getName()).thenReturn("trigger_1");
        when(workflowTrigger.getType()).thenReturn("schedule/v1/interval");
        doReturn(List.of(workflowTrigger)).when(workflow)
            .getExtensions(anyString(), any(), any());
        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);

        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getUuidAsString()).thenReturn("workflow-uuid");
        when(projectWorkflowService.getWorkflowProjectWorkflow(WORKFLOW_ID)).thenReturn(projectWorkflow);
    }

    private void givenEnabledRow(long projectDeploymentId, Map<String, ?> inputs) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(projectDeploymentId + 1);
        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        projectDeploymentWorkflow.setWorkflowId(WORKFLOW_ID);
        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setInputs(inputs);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId))
            .thenReturn(List.of(projectDeploymentWorkflow));
        when(projectDeploymentService.getProjectDeployment(projectDeploymentId))
            .thenReturn(mock(ProjectDeployment.class));
    }

    private ProjectDeploymentFacadeImpl createProjectDeploymentFacade() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getWebhookUrl()).thenReturn("http://localhost/webhooks/{id}");

        return new ProjectDeploymentFacadeImpl(
            null, null, null, null, null, null, null, projectDeploymentService, projectDeploymentWorkflowService, null,
            projectWorkflowService, null, null, null, triggerLifecycleFacade, applicationProperties,
            mock(ComponentConnectionFacade.class), workflowService);
    }

    private void saveThroughListener(McpServer mcpServer) {
        McpServerAfterSaveEventListener mcpServerAfterSaveEventListener = new McpServerAfterSaveEventListener(
            mcpProjectService, mcpServerService, projectDeploymentFacade);

        mcpServerAfterSaveEventListener.onBeforeConvert(new BeforeConvertEvent<>(mcpServer));

        mcpServerAfterSaveEventListener.onAfterSave(afterSaveEvent(mcpServer));
    }

    @SuppressWarnings("unchecked")
    private static AfterSaveEvent<McpServer> afterSaveEvent(McpServer mcpServer) {
        AfterSaveEvent<McpServer> afterSaveEvent = mock(AfterSaveEvent.class);

        when(afterSaveEvent.getEntity()).thenReturn(mcpServer);

        return afterSaveEvent;
    }

    private static McpServer mcpServer(boolean enabled) {
        McpServer mcpServer = new McpServer();

        mcpServer.setId(MCP_SERVER_ID);
        mcpServer.setEnabled(enabled);

        return mcpServer;
    }
}

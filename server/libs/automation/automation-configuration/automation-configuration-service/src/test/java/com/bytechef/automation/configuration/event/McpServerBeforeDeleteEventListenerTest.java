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

package com.bytechef.automation.configuration.event;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.McpProject;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.McpProjectService;
import com.bytechef.automation.configuration.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.configuration.domain.McpProjectWorkflow;
import com.bytechef.platform.configuration.domain.McpServer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;

/**
 * Unit test for {@link McpServerBeforeDeleteEventListener}.
 *
 * @author Ivica Cardic
 */
public class McpServerBeforeDeleteEventListenerTest {

    private final McpProjectService mcpProjectService = mock(McpProjectService.class);
    private final McpProjectWorkflowService mcpProjectWorkflowService = mock(McpProjectWorkflowService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService = mock(
        ProjectDeploymentWorkflowService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);

    @Test
    public void testOnBeforeDeleteServerWithProjectsDeletesAllRelatedData() {
        // Given
        McpServerBeforeDeleteEventListener listener = new McpServerBeforeDeleteEventListener(
            mcpProjectService, mcpProjectWorkflowService, projectDeploymentWorkflowService,
            projectDeploymentService, projectDeploymentFacade);

        Long mcpServerId = 1L;

        // Create test MCP projects
        McpProject mcpProject1 = new McpProject(100L, mcpServerId);
        mcpProject1.setId(10L);
        McpProject mcpProject2 = new McpProject(200L, mcpServerId);
        mcpProject2.setId(20L);
        List<McpProject> mcpProjects = Arrays.asList(mcpProject1, mcpProject2);

        // Create test MCP project workflows
        McpProjectWorkflow mcpProjectWorkflow1 = new McpProjectWorkflow(10L, 1001L);
        mcpProjectWorkflow1.setId(101L);
        McpProjectWorkflow mcpProjectWorkflow2 = new McpProjectWorkflow(10L, 1002L);
        mcpProjectWorkflow2.setId(102L);
        McpProjectWorkflow mcpProjectWorkflow3 = new McpProjectWorkflow(20L, 2001L);
        mcpProjectWorkflow3.setId(201L);

        // Create test project deployment workflows
        ProjectDeploymentWorkflow projectDeploymentWorkflow1 = new ProjectDeploymentWorkflow();
        projectDeploymentWorkflow1.setId(1001L);
        ProjectDeploymentWorkflow projectDeploymentWorkflow2 = new ProjectDeploymentWorkflow();
        projectDeploymentWorkflow2.setId(1002L);
        ProjectDeploymentWorkflow projectDeploymentWorkflow3 = new ProjectDeploymentWorkflow();
        projectDeploymentWorkflow3.setId(2001L);

        // Mock service calls
        when(mcpProjectService.getMcpServerMcpProjects(mcpServerId)).thenReturn(mcpProjects);
        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(10L))
            .thenReturn(Arrays.asList(mcpProjectWorkflow1, mcpProjectWorkflow2));
        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(20L))
            .thenReturn(List.of(mcpProjectWorkflow3));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(1001L))
            .thenReturn(projectDeploymentWorkflow1);
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(1002L))
            .thenReturn(projectDeploymentWorkflow2);
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(2001L))
            .thenReturn(projectDeploymentWorkflow3);

        // Create mock event
        @SuppressWarnings("unchecked")
        BeforeDeleteEvent<McpServer> event = mock(BeforeDeleteEvent.class);
        Identifier identifier = mock(Identifier.class);
        when(event.getId()).thenReturn(identifier);
        when(identifier.getValue()).thenReturn(mcpServerId);

        // When
        listener.onBeforeDelete(event);

        // Then
        // Verify project deployments are disabled
        verify(projectDeploymentFacade).enableProjectDeployment(eq(100L), eq(false));
        verify(projectDeploymentFacade).enableProjectDeployment(eq(200L), eq(false));

        // Verify MCP project workflows are deleted
        verify(mcpProjectWorkflowService).delete(eq(101L));
        verify(mcpProjectWorkflowService).delete(eq(102L));
        verify(mcpProjectWorkflowService).delete(eq(201L));

        // Verify project deployment workflows are deleted
        verify(projectDeploymentWorkflowService).delete(eq(1001L));
        verify(projectDeploymentWorkflowService).delete(eq(1002L));
        verify(projectDeploymentWorkflowService).delete(eq(2001L));

        // Verify MCP projects are deleted
        verify(mcpProjectService).delete(eq(10L));
        verify(mcpProjectService).delete(eq(20L));

        // Verify project deployments are deleted
        verify(projectDeploymentService).delete(eq(100L));
        verify(projectDeploymentService).delete(eq(200L));
    }

    @Test
    public void testOnBeforeDeleteServerWithNoProjectsNoOperations() {
        // Given
        McpServerBeforeDeleteEventListener listener = new McpServerBeforeDeleteEventListener(
            mcpProjectService, mcpProjectWorkflowService, projectDeploymentWorkflowService,
            projectDeploymentService, projectDeploymentFacade);

        long mcpServerId = 1L;

        when(mcpProjectService.getMcpServerMcpProjects(mcpServerId)).thenReturn(Collections.emptyList());

        // Create mock event
        @SuppressWarnings("unchecked")
        BeforeDeleteEvent<McpServer> event = mock(BeforeDeleteEvent.class);
        Identifier identifier = mock(Identifier.class);
        when(event.getId()).thenReturn(identifier);
        when(identifier.getValue()).thenReturn(mcpServerId);

        // When
        listener.onBeforeDelete(event);

        // Then
        verify(mcpProjectService).getMcpServerMcpProjects(mcpServerId);

        // Verify no other operations are performed
        verify(projectDeploymentFacade, times(0)).enableProjectDeployment(eq(100L), eq(false));
        verify(mcpProjectWorkflowService, times(0)).delete(eq(101L));
        verify(projectDeploymentWorkflowService, times(0)).delete(eq(1001L));
        verify(mcpProjectService, times(0)).delete(eq(10L));
        verify(projectDeploymentService, times(0)).delete(eq(100L));
    }

    @Test
    public void testOnBeforeDeleteProjectWithNoWorkflowsDeletesProjectOnly() {
        // Given
        McpServerBeforeDeleteEventListener listener = new McpServerBeforeDeleteEventListener(
            mcpProjectService, mcpProjectWorkflowService, projectDeploymentWorkflowService,
            projectDeploymentService, projectDeploymentFacade);

        Long mcpServerId = 1L;

        McpProject mcpProject = new McpProject(100L, mcpServerId);
        mcpProject.setId(10L);
        List<McpProject> mcpProjects = List.of(mcpProject);

        when(mcpProjectService.getMcpServerMcpProjects(mcpServerId)).thenReturn(mcpProjects);
        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(10L))
            .thenReturn(Collections.emptyList());

        // Create mock event
        @SuppressWarnings("unchecked")
        BeforeDeleteEvent<McpServer> event = mock(BeforeDeleteEvent.class);
        Identifier identifier = mock(Identifier.class);
        when(event.getId()).thenReturn(identifier);
        when(identifier.getValue()).thenReturn(mcpServerId);

        // When
        listener.onBeforeDelete(event);

        // Then
        // Verify project deployment is disabled
        verify(projectDeploymentFacade).enableProjectDeployment(eq(100L), eq(false));

        // Verify no workflows are deleted (since there are none)
        verify(mcpProjectWorkflowService, times(0)).delete(eq(101L));
        verify(projectDeploymentWorkflowService, times(0)).delete(eq(1001L));

        // Verify MCP project and project deployment are deleted
        verify(mcpProjectService).delete(eq(10L));
        verify(projectDeploymentService).delete(eq(100L));
    }

    @Test
    public void testOnBeforeDeleteMultipleProjectsWithVariousWorkflows() {
        // Given
        McpServerBeforeDeleteEventListener listener = new McpServerBeforeDeleteEventListener(
            mcpProjectService, mcpProjectWorkflowService, projectDeploymentWorkflowService,
            projectDeploymentService, projectDeploymentFacade);

        Long mcpServerId = 2L;

        // Create test MCP projects
        McpProject mcpProject1 = new McpProject(300L, mcpServerId);
        mcpProject1.setId(30L);
        McpProject mcpProject2 = new McpProject(400L, mcpServerId);
        mcpProject2.setId(40L);
        McpProject mcpProject3 = new McpProject(500L, mcpServerId);
        mcpProject3.setId(50L);
        List<McpProject> mcpProjects = Arrays.asList(mcpProject1, mcpProject2, mcpProject3);

        // Project 1 has 1 workflow, Project 2 has 2 workflows, Project 3 has no workflows
        McpProjectWorkflow workflow1 = new McpProjectWorkflow(30L, 3001L);
        workflow1.setId(301L);
        McpProjectWorkflow workflow2 = new McpProjectWorkflow(40L, 4001L);
        workflow2.setId(401L);
        McpProjectWorkflow workflow3 = new McpProjectWorkflow(40L, 4002L);
        workflow3.setId(402L);

        ProjectDeploymentWorkflow pdWorkflow1 = new ProjectDeploymentWorkflow();
        pdWorkflow1.setId(3001L);
        ProjectDeploymentWorkflow pdWorkflow2 = new ProjectDeploymentWorkflow();
        pdWorkflow2.setId(4001L);
        ProjectDeploymentWorkflow pdWorkflow3 = new ProjectDeploymentWorkflow();
        pdWorkflow3.setId(4002L);

        // Mock service calls
        when(mcpProjectService.getMcpServerMcpProjects(mcpServerId)).thenReturn(mcpProjects);
        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(30L))
            .thenReturn(List.of(workflow1));
        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(40L))
            .thenReturn(Arrays.asList(workflow2, workflow3));
        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(50L))
            .thenReturn(Collections.emptyList());
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(3001L))
            .thenReturn(pdWorkflow1);
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(4001L))
            .thenReturn(pdWorkflow2);
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(4002L))
            .thenReturn(pdWorkflow3);

        // Create mock event
        @SuppressWarnings("unchecked")
        BeforeDeleteEvent<McpServer> event = mock(BeforeDeleteEvent.class);
        Identifier identifier = mock(Identifier.class);
        when(event.getId()).thenReturn(identifier);
        when(identifier.getValue()).thenReturn(mcpServerId);

        // When
        listener.onBeforeDelete(event);

        // Then
        // Verify all project deployments are disabled
        verify(projectDeploymentFacade).enableProjectDeployment(eq(300L), eq(false));
        verify(projectDeploymentFacade).enableProjectDeployment(eq(400L), eq(false));
        verify(projectDeploymentFacade).enableProjectDeployment(eq(500L), eq(false));

        // Verify workflows are deleted
        verify(mcpProjectWorkflowService).delete(eq(301L));
        verify(mcpProjectWorkflowService).delete(eq(401L));
        verify(mcpProjectWorkflowService).delete(eq(402L));
        verify(projectDeploymentWorkflowService).delete(eq(3001L));
        verify(projectDeploymentWorkflowService).delete(eq(4001L));
        verify(projectDeploymentWorkflowService).delete(eq(4002L));

        // Verify all projects and deployments are deleted
        verify(mcpProjectService).delete(eq(30L));
        verify(mcpProjectService).delete(eq(40L));
        verify(mcpProjectService).delete(eq(50L));
        verify(projectDeploymentService).delete(eq(300L));
        verify(projectDeploymentService).delete(eq(400L));
        verify(projectDeploymentService).delete(eq(500L));
    }
}

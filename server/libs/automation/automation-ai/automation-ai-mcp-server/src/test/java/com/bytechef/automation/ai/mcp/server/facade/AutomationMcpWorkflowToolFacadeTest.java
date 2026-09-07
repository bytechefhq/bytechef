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

package com.bytechef.automation.ai.mcp.server.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * A workflow exposed as an MCP tool carries an optional name and description. Left unset, the workflow's own label and
 * description are what the model is given.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AutomationMcpWorkflowToolFacadeTest {

    private final McpProjectWorkflowService mcpProjectWorkflowService = mock(McpProjectWorkflowService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final AutomationMcpToolFacade automationMcpToolFacade = new AutomationMcpToolFacade(
        mock(ClusterElementDefinitionFacade.class), mock(ClusterElementDefinitionService.class), mock(Evaluator.class),
        mock(JobSyncExecutor.class), mock(McpComponentService.class), mcpProjectWorkflowService,
        mock(McpServerService.class), mock(PrincipalJobFacade.class), projectDeploymentWorkflowService,
        mock(TaskExecutionService.class), mock(TaskFileStorage.class), workflowService);

    @Test
    void testToolNameFallsBackToTheWorkflowLabel() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of());

        assertEquals("Send_Email", toolDefinition.name());
    }

    @Test
    void testToolNameUsesTheConfiguredName() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolName", "sendEmail"));

        assertEquals("sendEmail", toolDefinition.name());
    }

    @Test
    void testToolDescriptionFallsBackToTheWorkflowDescription() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of());

        assertEquals("Sends an email to a customer", toolDefinition.description());
    }

    @Test
    void testToolDescriptionUsesTheConfiguredDescription() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolDescription", "Emails the customer"));

        assertEquals("Emails the customer", toolDefinition.description());
    }

    @Test
    void testToolDescriptionFallsBackWhenTheConfiguredDescriptionIsBlank() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolDescription", "   "));

        assertEquals("Sends an email to a customer", toolDefinition.description());
    }

    private ToolDefinition getToolDefinition(Map<String, Object> parameters) {
        McpProject mcpProject = new McpProject();

        mcpProject.setId(1L);
        mcpProject.setMcpServerId(1L);

        McpProjectWorkflow mcpProjectWorkflow = new McpProjectWorkflow();

        mcpProjectWorkflow.setMcpProjectId(1L);
        mcpProjectWorkflow.setParameters(parameters);
        mcpProjectWorkflow.setProjectDeploymentWorkflowId(1L);

        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(1L))
            .thenReturn(List.of(mcpProjectWorkflow));

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setWorkflowId("workflow-1");

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(1L)).thenReturn(projectDeploymentWorkflow);

        WorkflowTrigger workflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "parameters", Map.of(), "type", "workflow/v1/newWorkflowCall"));

        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn("Send Email");
        when(workflow.getDescription()).thenReturn("Sends an email to a customer");
        when(workflow.getExtensions(WorkflowExtConstants.TRIGGERS, WorkflowTrigger.class, List.of()))
            .thenReturn(List.of(workflowTrigger));
        when(workflowService.getWorkflow("workflow-1")).thenReturn(workflow);

        List<ToolCallback> toolCallbacks = automationMcpToolFacade.getFunctionToolCallbacks(mcpProject);

        assertEquals(1, toolCallbacks.size());

        ToolCallback toolCallback = toolCallbacks.getFirst();

        return toolCallback.getToolDefinition();
    }
}

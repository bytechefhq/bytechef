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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ListMcpProjectWorkflowsToolCallbackTest {

    private McpProjectService mcpProjectService;
    private McpProjectWorkflowService mcpProjectWorkflowService;
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private WorkflowService workflowService;
    private ListMcpProjectWorkflowsToolCallback toolCallback;

    @BeforeEach
    void beforeEach() {
        mcpProjectService = mock(McpProjectService.class);
        mcpProjectWorkflowService = mock(McpProjectWorkflowService.class);
        projectDeploymentWorkflowService = mock(ProjectDeploymentWorkflowService.class);
        workflowService = mock(WorkflowService.class);
        toolCallback = new ListMcpProjectWorkflowsToolCallback(
            mcpProjectService, mcpProjectWorkflowService, projectDeploymentWorkflowService, workflowService);
    }

    @Test
    void testToolDefinitionName() {
        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("listMcpProjectWorkflows");
    }

    @Test
    void testMissingMcpServerIdReturnsError() {
        String result = toolCallback.call("{}");

        assertThat(result).contains("error");
        assertThat(result).contains("mcpServerId is required");
    }

    @Test
    void testListsAttachedWorkflowsWithToolMappingState() {
        McpProject mcpProject = new McpProject(300L, 200L, 5L);

        McpProjectWorkflow mcpProjectWorkflow = new McpProjectWorkflow();

        mcpProjectWorkflow.setId(400L);
        mcpProjectWorkflow.setProjectDeploymentWorkflowId(500L);
        mcpProjectWorkflow.setParameters(Map.of("toolName", "get_weather", "toolDescription", "Fetch weather"));

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setWorkflowId("workflow-1");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getId()).thenReturn("workflow-1");
        when(workflow.getLabel()).thenReturn("Weather Lookup");

        when(mcpProjectService.getMcpServerMcpProjects(5L)).thenReturn(List.of(mcpProject));
        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(300L))
            .thenReturn(List.of(mcpProjectWorkflow));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(500L))
            .thenReturn(projectDeploymentWorkflow);
        when(workflowService.getWorkflow("workflow-1")).thenReturn(workflow);

        String result = toolCallback.call("{\"mcpServerId\": 5}");

        assertThat(result).contains("\"mcpProjectWorkflowId\":400");
        assertThat(result).contains("get_weather");
        assertThat(result).contains("Weather Lookup");
        // A mocked workflow carries no triggers, so the callable-trigger scan reports it as not tool-callable.
        assertThat(result).contains("\"toolCallable\":false");
    }
}

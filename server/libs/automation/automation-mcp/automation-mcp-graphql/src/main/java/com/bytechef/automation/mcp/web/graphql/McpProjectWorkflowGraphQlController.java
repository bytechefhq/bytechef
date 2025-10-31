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

package com.bytechef.automation.mcp.web.graphql;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.mcp.service.McpProjectWorkflowService;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing {@link McpProjectWorkflow} entities.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class McpProjectWorkflowGraphQlController {

    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final WorkflowService workflowService;

    McpProjectWorkflowGraphQlController(
        McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, WorkflowService workflowService) {

        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.workflowService = workflowService;
    }

    @QueryMapping
    McpProjectWorkflow mcpProjectWorkflow(@Argument long id) {
        return mcpProjectWorkflowService.fetchMcpProjectWorkflow(id)
            .orElse(null);
    }

    @QueryMapping
    List<McpProjectWorkflow> mcpProjectWorkflows() {
        return mcpProjectWorkflowService.getMcpProjectWorkflows();
    }

    @QueryMapping
    List<McpProjectWorkflow> mcpProjectWorkflowsByMcpProjectId(@Argument long mcpProjectId) {
        return mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProjectId);
    }

    @QueryMapping
    List<McpProjectWorkflow>
        mcpProjectWorkflowsByProjectDeploymentWorkflowId(@Argument long projectDeploymentWorkflowId) {
        return mcpProjectWorkflowService.getProjectDeploymentWorkflowMcpProjectWorkflows(projectDeploymentWorkflowId);
    }

    @MutationMapping
    McpProjectWorkflow createMcpProjectWorkflow(@Argument("input") Map<String, Object> input) {
        Long mcpProjectId = Long.valueOf(String.valueOf(input.get("mcpProjectId")));
        Long projectDeploymentWorkflowId = Long.valueOf(String.valueOf(input.get("projectDeploymentWorkflowId")));

        return mcpProjectWorkflowService.create(mcpProjectId, projectDeploymentWorkflowId);
    }

    @MutationMapping
    McpProjectWorkflow updateMcpProjectWorkflow(@Argument("id") long id, @Argument("input") Map<String, Object> input) {
        Long mcpProjectId = null;

        if (input.containsKey("mcpProjectId")) {
            mcpProjectId = Long.valueOf(String.valueOf(input.get("mcpProjectId")));
        }

        Long projectDeploymentWorkflowId = null;

        if (input.containsKey("projectDeploymentWorkflowId")) {
            projectDeploymentWorkflowId = Long.valueOf(String.valueOf(input.get("projectDeploymentWorkflowId")));
        }

        return mcpProjectWorkflowService.update(id, mcpProjectId, projectDeploymentWorkflowId);
    }

    @MutationMapping
    boolean deleteMcpProjectWorkflow(@Argument("id") long id) {
        mcpProjectWorkflowService.delete(id);

        return true;
    }

    @SchemaMapping
    ProjectDeploymentWorkflow projectDeploymentWorkflow(McpProjectWorkflow mcpProjectWorkflow) {
        return projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
            mcpProjectWorkflow.getProjectDeploymentWorkflowId());
    }

    @SchemaMapping
    Workflow workflow(McpProjectWorkflow mcpProjectWorkflow) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                mcpProjectWorkflow.getProjectDeploymentWorkflowId());

        return workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());
    }
}

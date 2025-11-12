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

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.mcp.domain.McpProject;
import com.bytechef.automation.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.mcp.facade.McpProjectFacade;
import com.bytechef.automation.mcp.service.McpProjectService;
import com.bytechef.automation.mcp.service.McpProjectWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing {@link McpProject} entities.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class McpProjectGraphQlController {

    private final McpProjectFacade mcpProjectFacade;
    private final McpProjectService mcpProjectService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public McpProjectGraphQlController(
        McpProjectFacade mcpProjectFacade, McpProjectService mcpProjectService,
        McpProjectWorkflowService mcpProjectWorkflowService, ProjectDeploymentService projectDeploymentService,
        ProjectService projectService) {

        this.mcpProjectFacade = mcpProjectFacade;
        this.mcpProjectService = mcpProjectService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    @MutationMapping
    public McpProject createMcpProject(@Argument CreateMcpProjectInput input) {
        return mcpProjectFacade.createMcpProject(
            input.mcpServerId(), input.projectId(), input.projectVersion(), input.selectedWorkflowIds());
    }

    @QueryMapping
    public McpProject mcpProject(@Argument long id) {
        return mcpProjectService.fetchMcpProject(id)
            .orElse(null);
    }

    @QueryMapping
    public List<McpProject> mcpProjects() {
        return mcpProjectService.getMcpProjects();
    }

    @QueryMapping
    public List<McpProject> mcpProjectsByServerId(@Argument long mcpServerId) {
        return mcpProjectService.getMcpServerMcpProjects(mcpServerId);
    }

    @SchemaMapping
    public List<McpProjectWorkflow> mcpProjectWorkflows(McpProject mcpProject) {
        return mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProject.getId());
    }

    @SchemaMapping
    public Project project(McpProject mcpProject) {
        return projectService.getProjectDeploymentProject(mcpProject.getProjectDeploymentId());
    }

    @SchemaMapping
    public Integer projectVersion(McpProject mcpProject) {
        return projectDeploymentService.getProjectDeployment(mcpProject.getProjectDeploymentId())
            .getProjectVersion();
    }

    @MutationMapping
    public boolean deleteMcpProject(@Argument long id) {
        mcpProjectFacade.deleteMcpProject(id);

        return true;
    }

    @SuppressFBWarnings("EI")
    public record CreateMcpProjectInput(
        long mcpServerId, long projectId, int projectVersion, List<String> selectedWorkflowIds) {
    }
}

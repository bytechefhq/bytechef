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

package com.bytechef.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.McpProject;
import com.bytechef.automation.configuration.service.McpProjectService;
import com.bytechef.automation.configuration.service.McpProjectWorkflowService;
import com.bytechef.platform.configuration.domain.McpProjectWorkflow;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
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

    private final McpProjectService mcpProjectService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;

    @SuppressFBWarnings("EI")
    public McpProjectGraphQlController(
        McpProjectService mcpProjectService, McpProjectWorkflowService mcpProjectWorkflowService) {

        this.mcpProjectService = mcpProjectService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
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
        return mcpProjectService.getMcpProjectsByServerId(mcpServerId);
    }

    @SchemaMapping
    public List<McpProjectWorkflow> mcpProjectWorkflows(McpProject mcpProject) {
        return mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProject.getId());
    }
}

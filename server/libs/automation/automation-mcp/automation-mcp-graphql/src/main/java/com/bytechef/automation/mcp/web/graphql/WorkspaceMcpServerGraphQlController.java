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
import com.bytechef.automation.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing workspace MCP server relationships.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class WorkspaceMcpServerGraphQlController {

    private final WorkspaceMcpServerFacade workspaceMcpServerFacade;

    @SuppressFBWarnings("EI")
    public WorkspaceMcpServerGraphQlController(WorkspaceMcpServerFacade workspaceMcpServerFacade) {
        this.workspaceMcpServerFacade = workspaceMcpServerFacade;
    }

    @QueryMapping
    public List<McpServer> workspaceMcpServers(@Argument Long workspaceId) {
        return workspaceMcpServerFacade.getWorkspaceMcpServers(workspaceId);
    }

    @MutationMapping
    public McpServer createWorkspaceMcpServer(@Argument CreateWorkspaceMcpServerInput input) {
        return workspaceMcpServerFacade.createWorkspaceMcpServer(
            input.name(),
            input.type(),
            Environment.values()[(int) input.environmentId()],
            input.enabled(),
            input.workspaceId());
    }

    @MutationMapping
    public boolean deleteWorkspaceMcpServer(@Argument Long mcpServerId) {
        workspaceMcpServerFacade.deleteWorkspaceMcpServer(mcpServerId);

        return true;
    }

    public record CreateWorkspaceMcpServerInput(
        String name, PlatformType type, long environmentId, Boolean enabled, Long workspaceId) {
    }
}

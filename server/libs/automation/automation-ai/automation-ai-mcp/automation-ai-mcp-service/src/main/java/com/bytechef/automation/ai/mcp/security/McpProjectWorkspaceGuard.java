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

package com.bytechef.automation.ai.mcp.security;

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Refuses an MCP project whose MCP server and project live in different workspaces.
 *
 * <p>
 * An {@code McpProject} references both, and every MCP ownership resolver answers "whose is this" through the
 * <em>server</em>. That is only correct while the project belongs to the same workspace. A link spanning two would make
 * the project's workflows readable, as MCP tools, to anyone holding MCP_VIEW in the server's workspace -- including
 * members with no access to the workspace the project comes from. Keeping the link consistent where it is written is
 * what keeps the server route correct; moving one resolver to the deployment route instead would leave its sibling
 * {@code McpProjectWorkflow}, {@code McpComponent} and {@code McpTool} resolvers answering differently.
 *
 * <p>
 * Denies with {@link AccessDeniedException} rather than a validation error, so the response does not confirm that a
 * project in another workspace exists. Fails closed when either side's workspace cannot be resolved.
 *
 * @author Ivica Cardic
 */
@Component
public class McpProjectWorkspaceGuard {

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public McpProjectWorkspaceGuard(
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        WorkspaceMcpServerService workspaceMcpServerService) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    /**
     * For a link about to be created, when only the project is known and its deployment does not exist yet.
     */
    public void requireProjectInServerWorkspace(long mcpServerId, long projectId) {
        Optional<Long> projectWorkspaceId = projectService.fetchProject(projectId)
            .map(Project::getWorkspaceId);

        requireSameWorkspace(mcpServerId, projectWorkspaceId);
    }

    /**
     * For a link that references an existing deployment.
     */
    public void requireDeploymentInServerWorkspace(long mcpServerId, long projectDeploymentId) {
        Optional<Long> projectWorkspaceId = projectDeploymentService.fetchProjectDeployment(projectDeploymentId)
            .map(ProjectDeployment::getProjectId)
            .flatMap(projectService::fetchProject)
            .map(Project::getWorkspaceId);

        requireSameWorkspace(mcpServerId, projectWorkspaceId);
    }

    private void requireSameWorkspace(long mcpServerId, Optional<Long> projectWorkspaceId) {
        Optional<Long> serverWorkspaceId = workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(mcpServerId);

        if (serverWorkspaceId.isEmpty() || projectWorkspaceId.isEmpty() ||
            !Objects.equals(serverWorkspaceId.get(), projectWorkspaceId.get())) {

            throw new AccessDeniedException(
                "The project is not in the workspace of MCP server id=%s".formatted(mcpServerId));
        }
    }
}

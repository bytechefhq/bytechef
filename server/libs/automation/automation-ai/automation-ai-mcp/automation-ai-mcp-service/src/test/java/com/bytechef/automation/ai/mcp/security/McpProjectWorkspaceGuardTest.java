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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @author Ivica Cardic
 */
class McpProjectWorkspaceGuardTest {

    private static final long MCP_SERVER_ID = 11L;
    private static final long PROJECT_DEPLOYMENT_ID = 22L;
    private static final long PROJECT_ID = 33L;
    private static final long SERVER_WORKSPACE_ID = 1L;
    private static final long OTHER_WORKSPACE_ID = 2L;

    private McpProjectWorkspaceGuard mcpProjectWorkspaceGuard;
    private ProjectDeploymentService projectDeploymentService;
    private ProjectService projectService;
    private WorkspaceMcpServerService workspaceMcpServerService;

    @BeforeEach
    void beforeEach() {
        projectDeploymentService = mock(ProjectDeploymentService.class);
        projectService = mock(ProjectService.class);
        workspaceMcpServerService = mock(WorkspaceMcpServerService.class);

        mcpProjectWorkspaceGuard = new McpProjectWorkspaceGuard(
            projectDeploymentService, projectService, workspaceMcpServerService);

        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(MCP_SERVER_ID))
            .thenReturn(Optional.of(SERVER_WORKSPACE_ID));
    }

    @Test
    void testRequireProjectInServerWorkspaceAllowsTheSameWorkspace() {
        stubProjectWorkspace(SERVER_WORKSPACE_ID);

        assertThatCode(() -> mcpProjectWorkspaceGuard.requireProjectInServerWorkspace(MCP_SERVER_ID, PROJECT_ID))
            .doesNotThrowAnyException();
    }

    @Test
    void testRequireProjectInServerWorkspaceDeniesAnotherWorkspace() {
        stubProjectWorkspace(OTHER_WORKSPACE_ID);

        assertThatThrownBy(() -> mcpProjectWorkspaceGuard.requireProjectInServerWorkspace(MCP_SERVER_ID, PROJECT_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRequireProjectInServerWorkspaceDeniesAnUnknownProject() {
        when(projectService.fetchProject(PROJECT_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcpProjectWorkspaceGuard.requireProjectInServerWorkspace(MCP_SERVER_ID, PROJECT_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRequireProjectInServerWorkspaceDeniesAServerInNoWorkspace() {
        stubProjectWorkspace(SERVER_WORKSPACE_ID);

        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(MCP_SERVER_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcpProjectWorkspaceGuard.requireProjectInServerWorkspace(MCP_SERVER_ID, PROJECT_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRequireDeploymentInServerWorkspaceAllowsTheSameWorkspace() {
        stubDeploymentProject();
        stubProjectWorkspace(SERVER_WORKSPACE_ID);

        assertThatCode(() -> mcpProjectWorkspaceGuard.requireDeploymentInServerWorkspace(
            MCP_SERVER_ID, PROJECT_DEPLOYMENT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void testRequireDeploymentInServerWorkspaceDeniesAnotherWorkspace() {
        stubDeploymentProject();
        stubProjectWorkspace(OTHER_WORKSPACE_ID);

        assertThatThrownBy(() -> mcpProjectWorkspaceGuard.requireDeploymentInServerWorkspace(
            MCP_SERVER_ID, PROJECT_DEPLOYMENT_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRequireDeploymentInServerWorkspaceDeniesAnUnknownDeployment() {
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcpProjectWorkspaceGuard.requireDeploymentInServerWorkspace(
            MCP_SERVER_ID, PROJECT_DEPLOYMENT_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void stubDeploymentProject() {
        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);

        when(projectDeployment.getProjectId()).thenReturn(PROJECT_ID);
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(projectDeployment));
    }

    private void stubProjectWorkspace(long workspaceId) {
        Project project = mock(Project.class);

        when(project.getWorkspaceId()).thenReturn(workspaceId);
        when(projectService.fetchProject(PROJECT_ID))
            .thenReturn(Optional.of(project));
    }
}

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

package com.bytechef.automation.ai.mcp.tool.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.tool.McpServerToolCallbacksFactory;
import com.bytechef.automation.ai.tool.WorkspaceScopeResolver;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.mcp.domain.McpServer;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class McpServerMcpContributorConfigurationTest {

    private final McpServerMcpContributorConfiguration configuration =
        new McpServerMcpContributorConfiguration();

    @Nested
    class McpServerCrudContributorTest {

        private final McpProjectService mcpProjectService = mock(McpProjectService.class);
        private final WorkspaceMcpServerFacade workspaceMcpServerFacade = mock(WorkspaceMcpServerFacade.class);
        private final WorkspaceScopeResolver workspaceScopeResolver = mock(WorkspaceScopeResolver.class);

        @Test
        void testListMcpProjectWorkflowsAcceptsWorkspaceId() {
            ToolCallback listMcpProjectWorkflowsToolCallback = getListMcpProjectWorkflowsToolCallback();

            assertThat(listMcpProjectWorkflowsToolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId");
        }

        @Test
        void testListMcpProjectWorkflowsRejectsAnMcpServerOutsideTheResolvedWorkspace() {
            McpServer workspaceMcpServer = mock(McpServer.class);

            when(workspaceMcpServer.getId()).thenReturn(6L);
            when(workspaceMcpServerFacade.getWorkspaceMcpServers(7L)).thenReturn(List.of(workspaceMcpServer));
            when(workspaceScopeResolver.resolve(7L, null)).thenReturn(new WorkspaceScopeResolver.Resolved(7L, 0L));

            String result = getListMcpProjectWorkflowsToolCallback().call("{\"mcpServerId\": 5, \"workspaceId\": 7}");

            assertThat(result).contains("MCP server 5 not found in the current workspace");
            verify(mcpProjectService, never()).getMcpServerMcpProjects(anyLong());
        }

        @Test
        void testListMcpProjectWorkflowsListsAnMcpServerOfTheResolvedWorkspace() {
            McpServer workspaceMcpServer = mock(McpServer.class);

            when(workspaceMcpServer.getId()).thenReturn(5L);
            when(workspaceMcpServerFacade.getWorkspaceMcpServers(7L)).thenReturn(List.of(workspaceMcpServer));
            when(workspaceScopeResolver.resolve(7L, null)).thenReturn(new WorkspaceScopeResolver.Resolved(7L, 0L));
            when(mcpProjectService.getMcpServerMcpProjects(5L)).thenReturn(List.of());

            String result = getListMcpProjectWorkflowsToolCallback().call("{\"mcpServerId\": 5, \"workspaceId\": 7}");

            assertThat(result).isEqualTo("[]");
            verify(mcpProjectService).getMcpServerMcpProjects(5L);
        }

        @SuppressWarnings("unchecked")
        private ToolCallback getListMcpProjectWorkflowsToolCallback() {
            McpServerToolCallbacksFactory mcpServerToolCallbacksFactory = new McpServerToolCallbacksFactory(
                mock(McpProjectFacade.class), mcpProjectService, mock(McpProjectWorkflowService.class),
                mock(ProjectDeploymentWorkflowService.class), mock(WorkflowService.class), workspaceMcpServerFacade);

            ObjectProvider<McpServerToolCallbacksFactory> provider = mock(ObjectProvider.class);

            when(provider.getIfAvailable()).thenReturn(mcpServerToolCallbacksFactory);

            McpServerToolCallbackContributor contributor = configuration.mcpServerCrudMcpContributor(
                provider, workspaceScopeResolver);

            return contributor.getToolCallbacks()
                .stream()
                .filter(toolCallback -> "listMcpProjectWorkflows".equals(
                    toolCallback.getToolDefinition()
                        .name()))
                .findFirst()
                .orElseThrow();
        }
    }
}

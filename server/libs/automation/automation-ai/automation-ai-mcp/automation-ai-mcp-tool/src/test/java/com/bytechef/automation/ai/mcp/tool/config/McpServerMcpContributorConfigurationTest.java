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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.McpProjectWorkflowFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.mcp.tool.McpServerToolCallbacksFactory;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
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
    private final WorkspaceFacade workspaceFacade = memberWorkspaceFacade();

    @Nested
    class McpServerCrudContributorTest {

        private final McpProjectWorkflowFacade mcpProjectWorkflowFacade = mock(McpProjectWorkflowFacade.class);

        @Test
        void testListMcpProjectWorkflowsAcceptsWorkspaceId() {
            ToolCallback listMcpProjectWorkflowsToolCallback = getListMcpProjectWorkflowsToolCallback();

            assertThat(listMcpProjectWorkflowsToolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId");
        }

        @Test
        void testListMcpProjectWorkflowsListsThroughTheFacadeWithTheGivenWorkspace() {
            when(mcpProjectWorkflowFacade.getMcpServerMcpProjectWorkflows(5L)).thenReturn(List.of());

            String result = getListMcpProjectWorkflowsToolCallback().call("{\"mcpServerId\": 5, \"workspaceId\": 7}");

            assertThat(result).isEqualTo("[]");
            verify(mcpProjectWorkflowFacade).getMcpServerMcpProjectWorkflows(5L);
        }

        @SuppressWarnings("unchecked")
        private ToolCallback getListMcpProjectWorkflowsToolCallback() {
            McpServerToolCallbacksFactory mcpServerToolCallbacksFactory = new McpServerToolCallbacksFactory(
                mock(McpProjectFacade.class), mcpProjectWorkflowFacade, mock(WorkspaceMcpServerFacade.class));

            ObjectProvider<McpServerToolCallbacksFactory> provider = mock(ObjectProvider.class);

            when(provider.getIfAvailable()).thenReturn(mcpServerToolCallbacksFactory);

            McpServerToolCallbackContributor contributor = configuration.mcpServerCrudMcpContributor(
                provider, workspaceFacade);

            return contributor.getToolCallbacks()
                .stream()
                .filter(toolCallback -> "listMcpProjectWorkflows".equals(
                    toolCallback.getToolDefinition()
                        .name()))
                .findFirst()
                .orElseThrow();
        }
    }

    private static WorkspaceFacade memberWorkspaceFacade() {
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        lenient().when(workspaceFacade.getMyWorkspaces())
            .thenReturn(
                List.of(
                    new Workspace(1L, "Workspace 1"), new Workspace(7L, "Workspace 7"),
                    new Workspace(42L, "Workspace 42"), new Workspace(43L, "Workspace 43")));

        return workspaceFacade;
    }
}

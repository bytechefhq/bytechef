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
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class McpServerMcpContributorConfigurationTest {

    private final McpServerMcpContributorConfiguration configuration =
        new McpServerMcpContributorConfiguration();
    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

    @Nested
    class McpServerCrudContributorTest {

        private final WorkspaceMcpServerFacade workspaceMcpServerFacade = mock(WorkspaceMcpServerFacade.class);

        @Test
        void testContributesOnlyTheMcpServerToolsConfigureMcpServerCannotReplace() {
            assertThat(getToolCallbacks())
                .extracting(toolCallback -> toolCallback.getToolDefinition()
                    .name())
                .containsExactlyInAnyOrder("listMcpServers", "createMcpServer", "updateMcpServer", "createMcpProject")
                .doesNotContain(
                    "cloneMcpProject", "listMcpProjectWorkflows", "updateMcpProjectWorkflowParameters");
        }

        @Test
        void testListAndCreateMcpServerAcceptWorkspaceId() {
            assertThat(getToolCallbacks())
                .filteredOn(toolCallback -> Set.of("listMcpServers", "createMcpServer")
                    .contains(toolCallback.getToolDefinition()
                        .name()))
                .hasSize(2)
                .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                    .inputSchema()).contains("workspaceId"));
        }

        @Test
        void testListMcpServersListsThroughTheFacadeWithTheGivenWorkspace() {
            when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(7L, "Workspace")));

            String result = getToolCallbacks().stream()
                .filter(toolCallback -> "listMcpServers".equals(toolCallback.getToolDefinition()
                    .name()))
                .findFirst()
                .orElseThrow()
                .call("{\"workspaceId\": 7}");

            assertThat(result).isEqualTo("[]");
            verify(workspaceMcpServerFacade).getWorkspaceMcpServers(7L);
        }

        @SuppressWarnings("unchecked")
        private List<ToolCallback> getToolCallbacks() {
            McpServerToolCallbacksFactory mcpServerToolCallbacksFactory = new McpServerToolCallbacksFactory(
                mock(McpProjectFacade.class), mock(McpProjectWorkflowFacade.class), workspaceMcpServerFacade);

            ObjectProvider<McpServerToolCallbacksFactory> provider = mock(ObjectProvider.class);

            when(provider.getIfAvailable()).thenReturn(mcpServerToolCallbacksFactory);

            McpServerToolCallbackContributor contributor = configuration.mcpServerCrudMcpContributor(
                provider, workspaceFacade);

            return contributor.getToolCallbacks();
        }
    }
}

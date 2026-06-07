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

package com.bytechef.automation.ai.mcp.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.server.facade.AutomationMcpToolFacade;
import com.bytechef.automation.ai.mcp.server.spi.McpServerWorkspaceToolCallbackContributor;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import io.modelcontextprotocol.server.McpServerFeatures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Unit test for {@link AutomationMcpServerConfiguration#buildToolSpecifications}. Verifies that the per-server tool
 * list returned to the McpServer enumeration path includes callbacks contributed by all
 * {@link McpServerWorkspaceToolCallbackContributor} beans (e.g. the EE Context Store provider) alongside the existing
 * component-action and workflow tool surface, and that CE-only deployments without any provider compile and run cleanly
 * without contributing extra callbacks.
 *
 * @author Ivica Cardic
 */
@SuppressWarnings("unchecked")
class AutomationMcpServerConfigurationTest {

    private static final String SECRET_KEY = "secret-key";
    private static final long MCP_SERVER_ID = 42L;
    private static final long WORKSPACE_ID = 7L;

    private AutomationMcpToolFacade mcpToolFacade;
    private McpComponentService mcpComponentService;
    private McpProjectService mcpProjectService;
    private McpServer mcpServer;
    private McpServerService mcpServerService;
    private McpToolService mcpToolService;
    private WorkspaceMcpServerService workspaceMcpServerService;

    @BeforeEach
    void setUp() {
        mcpComponentService = mock(McpComponentService.class);
        mcpProjectService = mock(McpProjectService.class);
        mcpServerService = mock(McpServerService.class);
        mcpToolService = mock(McpToolService.class);
        mcpToolFacade = mock(AutomationMcpToolFacade.class);
        workspaceMcpServerService = mock(WorkspaceMcpServerService.class);

        mcpServer = new McpServer("server-name", PlatformType.AUTOMATION, Environment.PRODUCTION);

        mcpServer.setId(MCP_SERVER_ID);

        when(mcpServerService.getMcpServer(SECRET_KEY)).thenReturn(mcpServer);
        when(mcpComponentService.getMcpServerMcpComponents(MCP_SERVER_ID)).thenReturn(List.of());
        when(mcpProjectService.getMcpServerMcpProjects(MCP_SERVER_ID)).thenReturn(List.of());
    }

    @Test
    void testBuildToolSpecificationsAppendsContributedCallbacks() {
        ToolCallback contextStoreCallback = stubCallback("CONTEXT_STORE_HUBSPOT_CONTACTS_SEARCH");
        ToolCallback otherProviderCallback = stubCallback("CUSTOM_OTHER_TOOL");

        McpServerWorkspaceToolCallbackContributor contextStoreProvider =
            mock(McpServerWorkspaceToolCallbackContributor.class);
        McpServerWorkspaceToolCallbackContributor otherProvider = mock(McpServerWorkspaceToolCallbackContributor.class);

        when(contextStoreProvider.getFunctionToolCallbacks(WORKSPACE_ID))
            .thenReturn(List.of(contextStoreCallback));
        when(otherProvider.getFunctionToolCallbacks(WORKSPACE_ID)).thenReturn(List.of(otherProviderCallback));

        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(MCP_SERVER_ID))
            .thenReturn(Optional.of(WORKSPACE_ID));

        ObjectProvider<McpServerWorkspaceToolCallbackContributor> providers = stubProvider(
            List.of(contextStoreProvider, otherProvider));

        List<McpServerFeatures.AsyncToolSpecification> specifications =
            AutomationMcpServerConfiguration.buildToolSpecifications(
                SECRET_KEY, mcpComponentService, mcpProjectService, mcpServerService, mcpToolService, mcpToolFacade,
                providers, workspaceMcpServerService);

        assertThat(specifications)
            .extracting(specification -> specification.tool()
                .name())
            .containsExactly("CONTEXT_STORE_HUBSPOT_CONTACTS_SEARCH", "CUSTOM_OTHER_TOOL");
    }

    @Test
    void testBuildToolSpecificationsWithoutAnyProviderReturnsEmpty() {
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(MCP_SERVER_ID))
            .thenReturn(Optional.of(WORKSPACE_ID));

        ObjectProvider<McpServerWorkspaceToolCallbackContributor> providers = stubProvider(List.of());

        List<McpServerFeatures.AsyncToolSpecification> specifications =
            AutomationMcpServerConfiguration.buildToolSpecifications(
                SECRET_KEY, mcpComponentService, mcpProjectService, mcpServerService, mcpToolService, mcpToolFacade,
                providers, workspaceMcpServerService);

        assertThat(specifications).isEmpty();
    }

    @Test
    void testBuildToolSpecificationsWhenWorkspaceUnassignedSkipsProviders() {
        ToolCallback contextStoreCallback = stubCallback("CONTEXT_STORE_HUBSPOT_CONTACTS_SEARCH");

        McpServerWorkspaceToolCallbackContributor provider = mock(McpServerWorkspaceToolCallbackContributor.class);

        when(provider.getFunctionToolCallbacks(WORKSPACE_ID)).thenReturn(List.of(contextStoreCallback));

        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(MCP_SERVER_ID)).thenReturn(Optional.empty());

        ObjectProvider<McpServerWorkspaceToolCallbackContributor> providers = stubProvider(List.of(provider));

        List<McpServerFeatures.AsyncToolSpecification> specifications =
            AutomationMcpServerConfiguration.buildToolSpecifications(
                SECRET_KEY, mcpComponentService, mcpProjectService, mcpServerService, mcpToolService, mcpToolFacade,
                providers, workspaceMcpServerService);

        assertThat(specifications).isEmpty();
    }

    private static ToolCallback stubCallback(String name) {
        ToolCallback toolCallback = mock(ToolCallback.class);

        ToolDefinition toolDefinition = ToolDefinition.builder()
            .name(name)
            .description("stub tool")
            .inputSchema("{\"type\":\"object\"}")
            .build();

        when(toolCallback.getToolDefinition()).thenReturn(toolDefinition);

        return toolCallback;
    }

    private static ObjectProvider<McpServerWorkspaceToolCallbackContributor> stubProvider(
        List<McpServerWorkspaceToolCallbackContributor> providers) {

        ObjectProvider<McpServerWorkspaceToolCallbackContributor> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.orderedStream()).thenAnswer(invocation -> providers.stream());

        return objectProvider;
    }
}

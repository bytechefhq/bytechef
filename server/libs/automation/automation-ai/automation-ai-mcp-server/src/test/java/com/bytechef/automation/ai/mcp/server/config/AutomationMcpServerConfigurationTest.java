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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.server.facade.AutomationMcpToolFacade;
import com.bytechef.automation.ai.mcp.server.spi.McpServerWorkspaceToolCallbackContributor;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import io.modelcontextprotocol.server.McpServerFeatures;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class AutomationMcpServerConfigurationTest {

    @Test
    void testEnforcingServerKeepsOnlyAuthorizedComponents() {
        McpServer mcpServer = enforcingServer();

        McpComponent salesforce = component("salesforce", Set.of("ROLE_SALES"));
        McpComponent admin = component("admin", Set.of("ROLE_ADMIN"));

        List<McpComponent> authorized = AutomationMcpServerConfiguration.authorizedComponents(
            mcpServer, List.of(salesforce, admin), Set.of("ROLE_USER", "ROLE_SALES"));

        assertThat(authorized).containsExactly(salesforce);
    }

    @Test
    void testEnforcingServerDeniesComponentWithoutGrantingAuthority() {
        McpServer mcpServer = enforcingServer();

        McpComponent admin = component("admin", Set.of("ROLE_ADMIN"));

        List<McpComponent> authorized = AutomationMcpServerConfiguration.authorizedComponents(
            mcpServer, List.of(admin), Set.of("ROLE_USER"));

        assertThat(authorized).isEmpty();
    }

    @Test
    void testNonEnforcingServerExposesAllComponents() {
        McpServer mcpServer = new McpServer();

        McpComponent salesforce = component("salesforce", Set.of("ROLE_SALES"));
        McpComponent admin = component("admin", Set.of("ROLE_ADMIN"));

        List<McpComponent> authorized = AutomationMcpServerConfiguration.authorizedComponents(
            mcpServer, List.of(salesforce, admin), Set.of());

        assertThat(authorized).containsExactly(salesforce, admin);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDisabledMcpToolIsNotListed() {
        McpServer mcpServer = new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer.setId(1L);

        McpComponent mcpComponent = new McpComponent();

        mcpComponent.setId(2L);

        McpTool enabledMcpTool = new McpTool("enabledTool", Map.of(), 2L);

        enabledMcpTool.setId(3L);

        McpTool disabledMcpTool = new McpTool("disabledTool", Map.of(), 2L);

        disabledMcpTool.setId(4L);
        disabledMcpTool.setEnabled(false);

        McpServerService mcpServerService = mock(McpServerService.class);
        McpComponentService mcpComponentService = mock(McpComponentService.class);
        McpToolService mcpToolService = mock(McpToolService.class);
        McpProjectService mcpProjectService = mock(McpProjectService.class);
        WorkspaceMcpServerService workspaceMcpServerService = mock(WorkspaceMcpServerService.class);
        AutomationMcpToolFacade mcpToolFacade = mock(AutomationMcpToolFacade.class);
        ObjectProvider<McpServerWorkspaceToolCallbackContributor> workspaceToolProviders =
            (ObjectProvider<McpServerWorkspaceToolCallbackContributor>) mock(ObjectProvider.class);

        when(mcpServerService.getMcpServer("secret")).thenReturn(mcpServer);
        when(mcpComponentService.getMcpServerMcpComponents(1L)).thenReturn(List.of(mcpComponent));
        when(mcpToolService.getMcpComponentMcpTools(2L)).thenReturn(List.of(enabledMcpTool, disabledMcpTool));
        when(mcpProjectService.getMcpServerMcpProjects(1L)).thenReturn(List.of());
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(1L)).thenReturn(Optional.empty());

        Function<Map<String, Object>, Object> toolFunction = request -> "ok";

        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback = FunctionToolCallback
            .builder("enabledTool", toolFunction)
            .inputType(Map.class)
            .inputSchema("{\"type\":\"object\"}")
            .build();

        when(mcpToolFacade.getFunctionToolCallback(enabledMcpTool)).thenReturn(functionToolCallback);

        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications =
            AutomationMcpServerConfiguration.buildToolSpecifications(
                "secret", Set.of(), mcpComponentService, mcpProjectService, mcpServerService, mcpToolService,
                mcpToolFacade, workspaceToolProviders, workspaceMcpServerService);

        assertThat(toolSpecifications).hasSize(1);
        verify(mcpToolFacade, never()).getFunctionToolCallback(disabledMcpTool);
    }

    private static McpServer enforcingServer() {
        McpServer mcpServer = new McpServer();

        mcpServer.setEnforceToolAuthorization(true);

        return mcpServer;
    }

    private static McpComponent component(String componentName, Set<String> requiredAuthorities) {
        McpComponent mcpComponent = new McpComponent();

        mcpComponent.setComponentName(componentName);
        mcpComponent.setRequiredAuthorities(requiredAuthorities);

        return mcpComponent;
    }
}

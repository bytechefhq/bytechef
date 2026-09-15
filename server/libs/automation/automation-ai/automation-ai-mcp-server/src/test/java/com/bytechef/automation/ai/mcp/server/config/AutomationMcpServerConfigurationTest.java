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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.server.facade.AutomationMcpToolFacade;
import com.bytechef.automation.ai.mcp.server.security.web.authentication.AutomationMcpServerApiKeyAuthenticationToken;
import com.bytechef.automation.ai.mcp.server.spi.McpServerWorkspaceToolCallbackContributor;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import io.modelcontextprotocol.server.McpServerFeatures;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class AutomationMcpServerConfigurationTest {

    private static final long MCP_SERVER_ID = 7L;
    private static final String SECRET_KEY = "secret-key";

    private final McpComponentService mcpComponentService = mock(McpComponentService.class);
    private final McpProjectService mcpProjectService = mock(McpProjectService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final AutomationMcpToolFacade mcpToolFacade = mock(AutomationMcpToolFacade.class);

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new AutomationMcpServerApiKeyAuthenticationToken("api-key"));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testBuildToolSpecificationsReadsGatedDataUnderTheServerSecretKeyPrincipal() {
        McpServer mcpServer = new McpServer();

        mcpServer.setId(MCP_SERVER_ID);

        when(mcpServerService.getMcpServer(SECRET_KEY)).thenReturn(mcpServer);

        McpProject mcpProject = new McpProject(3L);

        when(mcpProjectService.getMcpServerMcpProjects(MCP_SERVER_ID))
            .thenAnswer(deniedUnlessChecksAreSkipped(List.of(mcpProject)));

        ToolCallback toolCallback = FunctionToolCallback
            .builder("projectTool", (Map<String, Object> input) -> "done")
            .inputType(Map.class)
            .inputSchema("{\"type\":\"object\"}")
            .build();

        when(mcpToolFacade.getFunctionToolCallbacks(mcpProject)).thenReturn(List.of(toolCallback));

        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications =
            AutomationMcpServerConfiguration.buildToolSpecifications(
                SECRET_KEY, mcpComponentService, mcpProjectService, mcpServerService, mock(McpToolService.class),
                mcpToolFacade, mockWorkspaceToolProviders(), mock(WorkspaceMcpServerService.class));

        assertThat(toolSpecifications)
            .extracting(toolSpecification -> toolSpecification.tool()
                .name())
            .containsExactly("projectTool");
    }

    @Test
    void testBuildToolSpecificationsLeavesChecksActiveAfterwards() {
        McpServer mcpServer = new McpServer();

        mcpServer.setId(MCP_SERVER_ID);

        when(mcpServerService.getMcpServer(SECRET_KEY)).thenReturn(mcpServer);
        when(mcpProjectService.getMcpServerMcpProjects(anyLong())).thenReturn(List.of());

        AutomationMcpServerConfiguration.buildToolSpecifications(
            SECRET_KEY, mcpComponentService, mcpProjectService, mcpServerService, mock(McpToolService.class),
            mcpToolFacade, mockWorkspaceToolProviders(), mock(WorkspaceMcpServerService.class));

        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<McpServerWorkspaceToolCallbackContributor> mockWorkspaceToolProviders() {
        return mock(ObjectProvider.class);
    }

    private static <T> Answer<T> deniedUnlessChecksAreSkipped(T value) {
        return invocation -> {
            if (!AutomationAuthorizationContext.isSkipChecks()) {
                throw new AccessDeniedException("Access Denied");
            }

            return value;
        };
    }
}

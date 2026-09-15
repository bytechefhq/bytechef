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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class McpToolEnvironmentResolverTest {

    private static final long MCP_COMPONENT_ID = 3L;
    private static final long MCP_SERVER_ID = 9L;
    private static final long MCP_TOOL_ID = 6L;

    private final McpComponentService mcpComponentService = mock(McpComponentService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final McpToolService mcpToolService = mock(McpToolService.class);

    @BeforeEach
    void setUp() {
        McpServer mcpServer = new McpServer();

        mcpServer.setEnvironment(Environment.PRODUCTION);

        when(mcpServerService.fetchMcpServer(MCP_SERVER_ID)).thenReturn(Optional.of(mcpServer));
        when(mcpComponentService.fetchMcpComponentMcpServerId(MCP_COMPONENT_ID)).thenReturn(Optional.of(MCP_SERVER_ID));
        when(mcpToolService.fetchMcpToolMcpComponentId(MCP_TOOL_ID)).thenReturn(Optional.of(MCP_COMPONENT_ID));
    }

    @Test
    void testMcpToolReportsTheEnvironmentOfItsServer() {
        McpToolEnvironmentResolver resolver =
            new McpToolEnvironmentResolver(mcpComponentService, mcpServerService, mcpToolService);

        assertThat(resolver.resourceType()).isEqualTo("McpTool");
        assertThat(resolver.fetchEnvironment(MCP_TOOL_ID)).contains(Environment.PRODUCTION);
        assertThat(resolver.fetchEnvironment(MCP_TOOL_ID + 100)).isEmpty();
    }
}
